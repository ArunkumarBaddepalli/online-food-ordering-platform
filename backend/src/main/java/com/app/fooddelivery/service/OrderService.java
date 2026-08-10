package com.app.fooddelivery.service;

import com.app.fooddelivery.model.OrderType;
import com.app.fooddelivery.model.*;
import com.app.fooddelivery.repository.CartRepository;
import com.app.fooddelivery.repository.FoodItemRepository;
import com.app.fooddelivery.repository.OrderRepository;
import com.app.fooddelivery.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    /** Minutes from order placement to expected delivery, used for the tracking countdown. */
    private static final int DEFAULT_DELIVERY_MINUTES = 35;

    @Autowired
    private RestaurantHoursValidator hoursValidator;

    @Autowired
    private OrderStatusFlow statusFlow;

    @Autowired
    private GeocodingService geocodingService;

    @Autowired
    private DistanceCalculationService distanceCalculationService;


    private LocalDateTime computeNextRestock(String restockTime) {
        try {
            LocalTime rt = LocalTime.parse(restockTime);
            LocalDateTime candidate = LocalDate.now().atTime(rt);
            if (LocalDateTime.now().isBefore(candidate)) return candidate;
            return candidate.plusDays(1);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public Order placeOrder(Long userId, String deliveryAddress, LocalDateTime scheduledTime, OrderType orderType) {
        return placeOrder(userId, deliveryAddress, scheduledTime, orderType, "COD");
    }

    @Transactional
    public Order placeOrder(Long userId, String deliveryAddress, LocalDateTime scheduledTime, OrderType orderType,
            String paymentMethod) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        Restaurant restaurant = cart.getItems().get(0).getFoodItem().getRestaurant();

        if (orderType == OrderType.DELIVERY) {
            if (deliveryAddress == null || deliveryAddress.trim().isEmpty()) {
                throw new RuntimeException("Delivery address is required for delivery orders.");
            }

            if (restaurant.getLatitude() != null && restaurant.getLongitude() != null) {
                GeocodingService.GeocodingResult result = geocodingService.geocodeAddress(deliveryAddress);
                if (result == null) {
                    throw new RuntimeException("Could not find location for address: " + deliveryAddress);
                }

                double distance = distanceCalculationService.calculateDistance(
                        restaurant.getLatitude(), restaurant.getLongitude(),
                        result.getLatitude(), result.getLongitude());

                double radiusKm = restaurant.getDeliveryRadiusKm() == null ? 10.0 : restaurant.getDeliveryRadiusKm();
                if (distance > radiusKm) {
                    throw new RuntimeException(
                            String.format("Delivery address is out of range. Distance: %.2f km. Max: %.2f km",
                                    distance, radiusKm));
                }
            }
        }

        RestaurantHoursValidator.ValidationResult validation = hoursValidator.validateOrderPlacement(restaurant,
                scheduledTime);
        if (!validation.isValid()) {
            throw new RuntimeException(validation.getMessage());
        }

        Order order = new Order();
        order.setUser(cart.getUser());
        order.setRestaurant(restaurant);
        order.setOrderDate(LocalDateTime.now());
        // Restaurants that opt into auto-accept skip the manual Accept tap.
        // Everything after this still needs a real person.
        order.setStatus(Boolean.TRUE.equals(restaurant.getAutoAcceptOrders())
                ? OrderStatusFlow.CONFIRMED
                : OrderStatusFlow.PLACED);
        order.setDeliveryAddress(deliveryAddress);
        order.setOrderType(orderType);

        if (scheduledTime != null) {
            order.setScheduledDeliveryTime(scheduledTime);
            order.setIsScheduled(true);
        }

        List<OrderItem> orderItems = new ArrayList<>();
        double totalAmount = 0;

        for (CartItem cartItem : cart.getItems()) {
            FoodItem foodItem = foodItemRepository.findByIdWithLock(cartItem.getFoodItem().getId())
                    .orElseThrow(() -> new RuntimeException("Food item not found: " + cartItem.getFoodItem().getName()));

            if (Boolean.FALSE.equals(foodItem.getInStock())) {
                throw new RuntimeException("Item is out of stock: " + foodItem.getName());
            }

            // UNLIMITED items never track or deplete stock.
            if (!"UNLIMITED".equals(foodItem.getStockResetType())) {
                int available = foodItem.getStockQuantity() == null ? 0 : foodItem.getStockQuantity();
                if (available < cartItem.getQuantity()) {
                    throw new RuntimeException("Insufficient stock for item: " + foodItem.getName());
                }

                foodItem.setStockQuantity(available - cartItem.getQuantity());

                if (foodItem.getStockQuantity() <= 0) {
                    foodItem.setInStock(false);
                    if ("DAILY".equals(foodItem.getStockResetType()) && foodItem.getDailyRestockTime() != null) {
                        foodItem.setNextAvailableAt(computeNextRestock(foodItem.getDailyRestockTime()));
                        foodItem.setOosReason("Sold out today");
                    } else {
                        foodItem.setOosReason("Out of stock");
                    }
                }

                foodItemRepository.save(foodItem);
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setFoodItem(foodItem);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getTotalPrice());

            if (cartItem.getSelectedModifiers() != null && !cartItem.getSelectedModifiers().isEmpty()) {
                List<OrderItemModifier> orderItemModifiers = new ArrayList<>();
                for (CartItemModifier cartModifier : cartItem.getSelectedModifiers()) {
                    OrderItemModifier orderModifier = new OrderItemModifier();
                    orderModifier.setOrderItem(orderItem);
                    orderModifier.setModifier(cartModifier.getModifier());
                    orderModifier.setQuantity(cartModifier.getQuantity());
                    orderModifier.setPriceAtOrder(cartModifier.getModifier().getPriceAdjustment());
                    orderModifier.setModifierName(cartModifier.getModifier().getName());
                    orderItemModifiers.add(orderModifier);
                }
                orderItem.setSelectedModifiers(orderItemModifiers);
            }

            orderItems.add(orderItem);
            totalAmount += cartItem.getTotalPrice();
        }

        order.setOrderItems(orderItems);
        order.setTotalAmount(totalAmount);

        // Fixed target time so the tracking page can count down instead of
        // recomputing the same "minutes remaining" on every poll.
        order.setEstimatedDeliveryAt(scheduledTime != null
                ? scheduledTime
                : LocalDateTime.now().plusMinutes(DEFAULT_DELIVERY_MINUTES));

        Order savedOrder = orderRepository.save(order);

        // Every order gets a payment record. Cash is collected on handover;
        // an online payment is confirmed separately once Razorpay verifies it.
        Payment payment = new Payment();
        payment.setOrder(savedOrder);
        payment.setAmount(totalAmount);
        payment.setPaymentMethod("ONLINE".equalsIgnoreCase(paymentMethod) ? "ONLINE" : "COD");
        payment.setPaymentStatus("PENDING");
        payment.setPaymentDate(LocalDateTime.now());
        paymentRepository.save(payment);
        savedOrder.setPayment(payment);

        cart.getItems().clear();
        cartRepository.save(cart);

        return savedOrder;
    }

    /**
     * Moves an order to the next stage, rejecting any move the lifecycle
     * does not allow. Cancelling goes through cancelOrder so that stock and
     * payment are handled too.
     */
    @Transactional
    public Order advanceStatus(Long orderId, String requestedStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        String target = statusFlow.validateTransition(order, requestedStatus);

        if (OrderStatusFlow.CANCELLED.equals(target)) {
            return cancelOrder(orderId);
        }

        order.setStatus(target);

        // Cash is collected when the food changes hands.
        Payment payment = order.getPayment();
        if (payment != null
                && (OrderStatusFlow.DELIVERED.equals(target) || OrderStatusFlow.PICKED_UP.equals(target))
                && "COD".equals(payment.getPaymentMethod())
                && "PENDING".equals(payment.getPaymentStatus())) {
            payment.setPaymentStatus("PAID");
            payment.setPaymentDate(LocalDateTime.now());
            paymentRepository.save(payment);
        }

        return orderRepository.save(order);
    }

    /**
     * Cancel order. Only allowed for PLACED or CONFIRMED status.
     * Restores stock for non-UNLIMITED items.
     */
    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        String status = order.getStatus();
        if (!statusFlow.canCancel(status)) {
            throw new RuntimeException("Cannot cancel order with status: " + status +
                    ". Once the restaurant starts preparing your food it can no longer be cancelled.");
        }

        for (OrderItem item : order.getOrderItems()) {
            FoodItem food = item.getFoodItem();
            if (!"UNLIMITED".equals(food.getStockResetType())) {
                int current = food.getStockQuantity() == null ? 0 : food.getStockQuantity();
                food.setStockQuantity(current + item.getQuantity());
                if (food.getStockQuantity() > 0) {
                    food.setInStock(true);
                    food.setNextAvailableAt(null);
                    food.setOosReason(null);
                }
                foodItemRepository.save(food);
            }
        }

        Payment payment = order.getPayment();
        if (payment != null) {
            payment.setPaymentStatus("CANCELLED");
            paymentRepository.save(payment);
        }

        order.setStatus("CANCELLED");
        return orderRepository.save(order);
    }
}
