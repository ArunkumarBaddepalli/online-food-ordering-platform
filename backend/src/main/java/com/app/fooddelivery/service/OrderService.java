package com.app.fooddelivery.service;

import com.app.fooddelivery.model.OrderType;
import com.app.fooddelivery.model.*;
import com.app.fooddelivery.repository.CartRepository;
import com.app.fooddelivery.repository.FoodItemRepository;
import com.app.fooddelivery.repository.OrderRepository;
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
    private RestaurantHoursValidator hoursValidator;

    @Autowired
    private GeocodingService geocodingService;

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

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

    public Order placeOrder(Long userId, String deliveryAddress, OrderType orderType) {
        return placeOrder(userId, deliveryAddress, null, orderType);
    }

    @Transactional
    public Order placeOrder(Long userId, String deliveryAddress, LocalDateTime scheduledTime, OrderType orderType) {
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

                double distance = calculateDistance(
                        restaurant.getLatitude(), restaurant.getLongitude(),
                        result.getLatitude(), result.getLongitude());

                if (distance > restaurant.getDeliveryRadiusKm()) {
                    throw new RuntimeException(
                            String.format("Delivery address is out of range. Distance: %.2f km. Max: %.2f km",
                                    distance, restaurant.getDeliveryRadiusKm()));
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
        order.setStatus("PLACED");
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

            if (Boolean.FALSE.equals(foodItem.getInStock())
                    || foodItem.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Insufficient stock for item: " + foodItem.getName());
            }

            foodItem.setStockQuantity(foodItem.getStockQuantity() - cartItem.getQuantity());

            if (!"UNLIMITED".equals(foodItem.getStockResetType()) && foodItem.getStockQuantity() <= 0) {
                foodItem.setInStock(false);
                if ("DAILY".equals(foodItem.getStockResetType()) && foodItem.getDailyRestockTime() != null) {
                    foodItem.setNextAvailableAt(computeNextRestock(foodItem.getDailyRestockTime()));
                    foodItem.setOosReason("Sold out today");
                } else {
                    foodItem.setOosReason("Out of stock");
                }
            } else if (foodItem.getStockQuantity() == 0) {
                foodItem.setInStock(false);
            }

            foodItemRepository.save(foodItem);

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

        Order savedOrder = orderRepository.save(order);

        cart.getItems().clear();
        cartRepository.save(cart);

        return savedOrder;
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
        if (!"PLACED".equals(status) && !"CONFIRMED".equals(status)) {
            throw new RuntimeException("Cannot cancel order with status: " + status +
                    ". Only PLACED or CONFIRMED orders can be cancelled.");
        }

        for (OrderItem item : order.getOrderItems()) {
            FoodItem food = item.getFoodItem();
            if (!"UNLIMITED".equals(food.getStockResetType())) {
                food.setStockQuantity(food.getStockQuantity() + item.getQuantity());
                if (food.getStockQuantity() > 0) {
                    food.setInStock(true);
                    food.setNextAvailableAt(null);
                    food.setOosReason(null);
                }
                foodItemRepository.save(food);
            }
        }

        order.setStatus("CANCELLED");
        return orderRepository.save(order);
    }
}
