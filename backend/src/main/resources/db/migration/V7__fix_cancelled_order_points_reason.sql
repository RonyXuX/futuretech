-- V5 recorded the points taken back from the cancelled demo order as ADMIN_ADJUSTMENT, but the app
-- always uses ORDER_CANCELLED for that (PointsService.reverseForOrder), so the demo history looked
-- like a manual admin change. Real admin adjustments never belong to an order, so
-- "ADMIN_ADJUSTMENT with an order" finds exactly the wrong rows and nothing else.
UPDATE point_transactions
SET reason = 'ORDER_CANCELLED'
WHERE reason = 'ADMIN_ADJUSTMENT'
  AND order_id IS NOT NULL;
