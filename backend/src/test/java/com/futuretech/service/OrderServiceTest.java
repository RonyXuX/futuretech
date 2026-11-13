package com.futuretech.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.futuretech.dto.order.OrderDto;
import com.futuretech.dto.order.PlaceOrderRequest;
import com.futuretech.dto.order.PlaceOrderRequest.CartItem;
import com.futuretech.entity.Order;
import com.futuretech.entity.OrderItem;
import com.futuretech.entity.Product;
import com.futuretech.entity.User;
import com.futuretech.entity.Warranty;
import com.futuretech.entity.enums.OrderStatus;
import com.futuretech.entity.enums.ProductType;
import com.futuretech.exception.ConflictException;
import com.futuretech.exception.ResourceNotFoundException;
import com.futuretech.repository.OrderRepository;
import com.futuretech.repository.ProductRepository;
import com.futuretech.repository.UserRepository;
import com.futuretech.repository.WarrantyRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Long USER_ID = 5L;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 13);
    private static final PlaceOrderRequest.Shipping SHIPPING =
            new PlaceOrderRequest.Shipping(" Lina Haddad ", "12 Hamra Street", "Beirut", "+961 70 000 001");

    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private WarrantyRepository warrantyRepository;
    @Mock private PointsService pointsService;

    private OrderService orderService;
    private User user;
    private Product cpu;    // 451.99, 5 in stock, 36 months warranty
    private Product mouse;  // 79.99, 2 in stock, 24 months warranty

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-13T10:00:00Z"), ZoneOffset.UTC);
        orderService = new OrderService(orderRepository, productRepository, userRepository, warrantyRepository,
                pointsService, clock);

        user = new User();
        user.setId(USER_ID);
        cpu = product(1L, "AMD Ryzen 7 9800X3D", "451.99", 5, 36);
        mouse = product(2L, "Logitech G502 X", "79.99", 2, 24);

        lenient().when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        // pretend the database gives ids to the order and its lines when saving
        AtomicLong ids = new AtomicLong(100);
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(ids.incrementAndGet());
            o.getItems().forEach(i -> i.setId(ids.incrementAndGet()));
            return o;
        });
        lenient().when(warrantyRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void placeOrder_createsOrderWithPriceSnapshotStockWarrantiesAndPoints() {
        when(productRepository.findAllByIdForUpdate(any())).thenReturn(List.of(cpu, mouse));
        when(pointsService.awardForOrder(any())).thenReturn(611);

        OrderDto result = orderService.placeOrder(USER_ID, request(new CartItem(1L, 1), new CartItem(2L, 2)));

        // 451.99 + 2 x 79.99
        assertThat(result.totalAmount()).isEqualByComparingTo("611.97");
        assertThat(result.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(result.pointsEarned()).isEqualTo(611);
        assertThat(result.shipping().fullName()).isEqualTo("Lina Haddad");   // trimmed
        assertThat(result.items()).extracting(OrderDto.Line::unitPrice)
                .containsExactly(new BigDecimal("451.99"), new BigDecimal("79.99"));

        // stock went down
        assertThat(cpu.getStockQuantity()).isEqualTo(4);
        assertThat(mouse.getStockQuantity()).isZero();

        // one warranty per line, from today + the product's months
        assertThat(result.items()).extracting(OrderDto.Line::warrantyExpiresOn)
                .containsExactly(TODAY.plusMonths(36), TODAY.plusMonths(24));
    }

    @Test
    void placeOrder_sameProductTwiceInCart_becomesOneLine() {
        when(productRepository.findAllByIdForUpdate(any())).thenReturn(List.of(mouse));

        OrderDto result = orderService.placeOrder(USER_ID, request(new CartItem(2L, 1), new CartItem(2L, 1)));

        assertThat(result.items()).singleElement().satisfies(line -> assertThat(line.quantity()).isEqualTo(2));
    }

    @Test
    void placeOrder_listsEveryProblem_andSavesNothing() {
        Product hidden = product(3L, "Old Case", "50.00", 9, 24);
        hidden.setActive(false);
        when(productRepository.findAllByIdForUpdate(any())).thenReturn(List.of(mouse, hidden));  // id 99 doesn't exist

        assertThatThrownBy(() -> orderService.placeOrder(USER_ID,
                request(new CartItem(2L, 3), new CartItem(3L, 1), new CartItem(99L, 1))))
                .isInstanceOfSatisfying(ConflictException.class, ex -> assertThat(ex.getProblems()).containsExactly(
                        "Logitech G502 X: only 2 left in stock (you asked for 3).",
                        "Product 3 is no longer available.",
                        "Product 99 is no longer available."));

        verify(orderRepository, never()).save(any());
        verify(pointsService, never()).awardForOrder(any());
        assertThat(mouse.getStockQuantity()).isEqualTo(2);  // untouched
    }

    @Test
    void placeOrder_moreThan10OfOneProduct_isRefused() {
        cpu.setStockQuantity(50);
        when(productRepository.findAllByIdForUpdate(any())).thenReturn(List.of(cpu));

        // 6 + 6 = 12 after merging the two lines
        assertThatThrownBy(() -> orderService.placeOrder(USER_ID, request(new CartItem(1L, 6), new CartItem(1L, 6))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void cancel_placedOrder_restoresStockRemovesWarrantiesAndTakesBackPoints() {
        Order order = order(OrderStatus.PLACED, cpu, 2);
        cpu.setStockQuantity(3);
        when(orderRepository.findWithLockByIdAndUserId(50L, USER_ID)).thenReturn(Optional.of(order));

        OrderDto result = orderService.cancelMyOrder(USER_ID, 50L);

        assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(result.pointsEarned()).isZero();
        assertThat(cpu.getStockQuantity()).isEqualTo(5);  // 3 + the 2 that were ordered
        verify(warrantyRepository).deleteByOrderItemOrderId(50L);
        verify(pointsService).reverseForOrder(order);
    }

    @Test
    void cancel_shippedOrder_isRefused() {
        Order order = order(OrderStatus.SHIPPED, cpu, 1);
        when(orderRepository.findWithLockByIdAndUserId(50L, USER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelMyOrder(USER_ID, 50L)).isInstanceOf(ConflictException.class);
        assertThat(cpu.getStockQuantity()).isEqualTo(5);
        verify(pointsService, never()).reverseForOrder(any());
    }

    @Test
    void getMyOrder_someoneElsesOrder_looksLikeItDoesNotExist() {
        when(orderRepository.findByIdAndUserId(50L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getMyOrder(USER_ID, 50L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order 50 not found");
    }

    @Test
    void getMyOrder_showsWarrantyAndNetPoints() {
        Order order = order(OrderStatus.DELIVERED, cpu, 1);
        Warranty warranty = new Warranty();
        warranty.setOrderItem(order.getItems().getFirst());
        warranty.setExpiryDate(TODAY.plusMonths(36));
        when(orderRepository.findByIdAndUserId(50L, USER_ID)).thenReturn(Optional.of(order));
        when(warrantyRepository.findByOrderItemOrderId(50L)).thenReturn(List.of(warranty));
        when(pointsService.pointsForOrder(50L)).thenReturn(451);

        OrderDto result = orderService.getMyOrder(USER_ID, 50L);

        assertThat(result.pointsEarned()).isEqualTo(451);
        assertThat(result.items().getFirst().warrantyExpiresOn()).isEqualTo(TODAY.plusMonths(36));
    }

    // ---------------------------------------------------------------- helpers

    private static PlaceOrderRequest request(CartItem... items) {
        return new PlaceOrderRequest(List.of(items), SHIPPING);
    }

    private static Product product(Long id, String name, String price, int stock, int warrantyMonths) {
        Product p = new Product();
        p.setId(id);
        p.setSku("FT-TEST-" + id);
        p.setName(name);
        p.setPrice(new BigDecimal(price));
        p.setStockQuantity(stock);
        p.setWarrantyMonths(warrantyMonths);
        p.setProductType(ProductType.CPU);
        return p;
    }

    private Order order(OrderStatus status, Product product, int quantity) {
        Order order = new Order();
        order.setId(50L);
        order.setUser(user);
        order.setStatus(status);
        order.setShippingFullName("Lina Haddad");
        order.setShippingAddress("12 Hamra Street");
        order.setShippingCity("Beirut");
        order.setShippingPhone("+961");
        OrderItem item = new OrderItem();
        item.setId(51L);
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setUnitPrice(product.getPrice());
        order.addItem(item);
        order.setTotalAmount(item.getLineTotal());
        return order;
    }
}
