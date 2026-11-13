package com.futuretech.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futuretech.dto.order.OrderDto;
import com.futuretech.dto.order.OrderSummaryDto;
import com.futuretech.dto.order.PlaceOrderRequest;
import com.futuretech.entity.Order;
import com.futuretech.entity.OrderItem;
import com.futuretech.entity.Product;
import com.futuretech.entity.User;
import com.futuretech.entity.Warranty;
import com.futuretech.entity.enums.OrderStatus;
import com.futuretech.exception.ConflictException;
import com.futuretech.exception.ResourceNotFoundException;
import com.futuretech.repository.OrderRepository;
import com.futuretech.repository.ProductRepository;
import com.futuretech.repository.UserRepository;
import com.futuretech.repository.WarrantyRepository;

/**
 * Checkout and the customer's own orders. Every public method takes the logged-in user's id
 * (from the JWT) and only ever touches that user's orders.
 */
@Service
public class OrderService {

    static final int MAX_QUANTITY_PER_PRODUCT = 10;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final WarrantyRepository warrantyRepository;
    private final PointsService pointsService;
    private final Clock clock;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
                        UserRepository userRepository, WarrantyRepository warrantyRepository,
                        PointsService pointsService, Clock clock) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.warrantyRepository = warrantyRepository;
        this.pointsService = pointsService;
        this.clock = clock;
    }

    /**
     * Fake checkout, all in ONE transaction: if anything fails, nothing is saved.
     * 1. lock the products, 2. check availability and stock, 3. create the order with today's
     * prices, 4. reduce stock, 5. create warranties, 6. award points.
     */
    @Transactional
    public OrderDto placeOrder(Long userId, PlaceOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User " + userId + " not found"));

        // The same product twice in the cart becomes one line
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (PlaceOrderRequest.CartItem item : request.items()) {
            quantities.merge(item.productId(), item.quantity(), Integer::sum);
        }

        Map<Long, Product> products = productRepository.findAllByIdForUpdate(quantities.keySet()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        checkAvailability(quantities, products);

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PLACED);
        order.setShippingFullName(request.shipping().fullName().trim());
        order.setShippingAddress(request.shipping().address().trim());
        order.setShippingCity(request.shipping().city().trim());
        order.setShippingPhone(request.shipping().phone().trim());

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Product product = products.get(entry.getKey());
            int quantity = entry.getValue();

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setUnitPrice(product.getPrice());   // price snapshot: later price changes don't affect this order
            order.addItem(item);

            product.setStockQuantity(product.getStockQuantity() - quantity);
            total = total.add(item.getLineTotal());
        }
        order.setTotalAmount(total);
        orderRepository.save(order);  // also saves the items (cascade), which gives them ids

        List<Warranty> warranties = createWarranties(order);
        int points = pointsService.awardForOrder(order);
        return OrderDto.from(order, points, byItemId(warranties));
    }

    /** The user's order history, newest first. */
    @Transactional(readOnly = true)
    public List<OrderSummaryDto> getMyOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderSummaryDto::from)
                .toList();
    }

    /** One of the user's orders, or 404 (also when the order belongs to someone else). */
    @Transactional(readOnly = true)
    public OrderDto getMyOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> orderNotFound(orderId));
        return toDto(order);
    }

    /**
     * Cancel an order that hasn't shipped yet: stock goes back on the shelf, its warranties
     * are removed, and its points are taken back.
     */
    @Transactional
    public OrderDto cancelMyOrder(Long userId, Long orderId) {
        Order order = orderRepository.findWithLockByIdAndUserId(orderId, userId)
                .orElseThrow(() -> orderNotFound(orderId));
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new ConflictException("Only orders that have not shipped yet can be cancelled (this one is "
                    + order.getStatus() + ").");
        }

        List<Long> productIds = order.getItems().stream().map(i -> i.getProduct().getId()).toList();
        productRepository.findAllByIdForUpdate(productIds);  // lock the rows before changing stock
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
        warrantyRepository.deleteByOrderItemOrderId(order.getId());
        pointsService.reverseForOrder(order);
        return OrderDto.from(order, 0, Map.of());
    }

    // ---------------------------------------------------------------- helpers

    /** Collects EVERY problem first, so the customer can fix the whole cart in one go. */
    private void checkAvailability(Map<Long, Integer> quantities, Map<Long, Product> products) {
        List<String> problems = new ArrayList<>();
        quantities.forEach((productId, quantity) -> {
            Product product = products.get(productId);
            if (product == null || !product.getActive()) {
                problems.add("Product " + productId + " is no longer available.");
            } else if (quantity > MAX_QUANTITY_PER_PRODUCT) {
                problems.add(product.getName() + ": you can order at most " + MAX_QUANTITY_PER_PRODUCT + ".");
            } else if (product.getStockQuantity() < quantity) {
                problems.add(product.getName() + ": only " + product.getStockQuantity()
                        + " left in stock (you asked for " + quantity + ").");
            }
        });
        if (!problems.isEmpty()) {
            throw new ConflictException("Some items in your cart can't be ordered.", problems);
        }
    }

    /** One warranty per order line: from today until today + the product's warranty period. */
    private List<Warranty> createWarranties(Order order) {
        LocalDate today = LocalDate.now(clock);
        List<Warranty> warranties = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            int months = item.getProduct().getWarrantyMonths();
            if (months > 0) {
                Warranty warranty = new Warranty();
                warranty.setOrderItem(item);
                warranty.setStartDate(today);
                warranty.setExpiryDate(today.plusMonths(months));
                warranties.add(warranty);
            }
        }
        return warrantyRepository.saveAll(warranties);
    }

    private OrderDto toDto(Order order) {
        return OrderDto.from(order, pointsService.pointsForOrder(order.getId()),
                byItemId(warrantyRepository.findByOrderItemOrderId(order.getId())));
    }

    private static Map<Long, Warranty> byItemId(List<Warranty> warranties) {
        return warranties.stream().collect(Collectors.toMap(w -> w.getOrderItem().getId(), Function.identity()));
    }

    private static ResourceNotFoundException orderNotFound(Long orderId) {
        return new ResourceNotFoundException("Order " + orderId + " not found");
    }
}
