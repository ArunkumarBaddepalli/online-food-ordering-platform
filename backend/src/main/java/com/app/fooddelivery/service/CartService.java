package com.app.fooddelivery.service;

import com.app.fooddelivery.model.*;
import com.app.fooddelivery.repository.CartRepository;
import com.app.fooddelivery.repository.FoodItemRepository;
import com.app.fooddelivery.repository.UserRepository;
import com.app.fooddelivery.repository.ModifierRepository;
import com.app.fooddelivery.repository.OrderRepository;
import com.app.fooddelivery.dto.CartValidationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModifierRepository modifierRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestaurantHoursValidator hoursValidator;

    public Cart getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart newCart = new Cart();
            User user = userRepository.findById(userId).orElseThrow();
            newCart.setUser(user);
            return cartRepository.save(newCart);
        });
    }

    public Cart addToCart(Long userId, Long foodId, int quantity, List<Long> modifierIds) {
        Cart cart = getCartByUserId(userId);
        FoodItem food = foodItemRepository.findById(foodId).orElseThrow();

        // Check item is in stock
        if (Boolean.FALSE.equals(food.getInStock())) {
            throw new RuntimeException("Item is out of stock: " + food.getName());
        }

        // Check restaurant is open (or accepts scheduled orders)
        Restaurant restaurant = food.getRestaurant();
        if (!hoursValidator.isRestaurantOpen(restaurant)
                && !Boolean.TRUE.equals(restaurant.getAcceptsScheduledOrders())) {
            throw new RuntimeException("Restaurant is currently closed");
        }

        // Check items are from same restaurant
        if (!cart.getItems().isEmpty()) {
            Long currentRestaurantId = cart.getItems().get(0).getFoodItem().getRestaurant().getId();
            if (!currentRestaurantId.equals(food.getRestaurant().getId())) {
                throw new RuntimeException("Items from different restaurant");
            }
        }

        List<Modifier> modifiersList = new ArrayList<>();
        if (modifierIds != null && !modifierIds.isEmpty()) {
            modifiersList = modifierRepository.findAllById(modifierIds);
        }
        final List<Modifier> modifiers = modifiersList;

        Optional<CartItem> existingItemOpt = cart.getItems().stream()
                .filter(item -> item.getFoodItem().getId().equals(foodId))
                .filter(item -> areModifiersEqual(item, modifiers))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + quantity;
            if (newQuantity > food.getStockQuantity()) {
                throw new RuntimeException("Cannot add more items. Only " + food.getStockQuantity() + " available.");
            }
            existingItem.setQuantity(newQuantity);
            updateItemPrice(existingItem);
        } else {
            if (quantity > food.getStockQuantity()) {
                throw new RuntimeException("Cannot add more items. Only " + food.getStockQuantity() + " available.");
            }
            CartItem newItem = new CartItem();
            newItem.setFoodItem(food);
            newItem.setQuantity(quantity);
            newItem.setSelectedModifiers(new ArrayList<>());

            for (Modifier mod : modifiers) {
                CartItemModifier cartItemModifier = new CartItemModifier();
                cartItemModifier.setCartItem(newItem);
                cartItemModifier.setModifier(mod);
                newItem.getSelectedModifiers().add(cartItemModifier);
            }

            updateItemPrice(newItem);
            cart.addItem(newItem);
        }

        return cartRepository.save(cart);
    }

    private void updateItemPrice(CartItem item) {
        double foodPrice = item.getFoodItem().getPrice();
        double modifiersPrice = item.getSelectedModifiers().stream()
                .mapToDouble(m -> m.getModifier().getPriceAdjustment())
                .sum();
        item.setTotalPrice((foodPrice + modifiersPrice) * item.getQuantity());
    }

    private boolean areModifiersEqual(CartItem item, List<Modifier> newModifiers) {
        if (item.getSelectedModifiers() == null && newModifiers.isEmpty())
            return true;
        if (item.getSelectedModifiers() == null || newModifiers == null)
            return false;

        List<Long> currentModIds = item.getSelectedModifiers().stream()
                .map(m -> m.getModifier().getId())
                .sorted()
                .collect(Collectors.toList());

        List<Long> newModIds = newModifiers.stream()
                .map(Modifier::getId)
                .sorted()
                .collect(Collectors.toList());

        return currentModIds.equals(newModIds);
    }

    public Cart addToCart(Long userId, Long foodId, int quantity) {
        return addToCart(userId, foodId, quantity, null);
    }

    public void clearCart(Long userId) {
        Cart cart = getCartByUserId(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    /**
     * Validates all cart items for OOS status. Returns per-item validation result.
     */
    public CartValidationResponse validateCart(Long userId) {
        Cart cart = getCartByUserId(userId);
        List<CartValidationResponse.CartItemValidation> validations = new ArrayList<>();
        boolean allValid = true;

        for (CartItem item : cart.getItems()) {
            FoodItem food = item.getFoodItem();
            boolean isOOS = Boolean.FALSE.equals(food.getInStock()) || food.getStockQuantity() < item.getQuantity();

            CartValidationResponse.CartItemValidation v = new CartValidationResponse.CartItemValidation();
            v.setCartItemId(item.getId());
            v.setFoodItemId(food.getId());
            v.setFoodItemName(food.getName());
            v.setOOS(isOOS);
            v.setStockQuantity(food.getStockQuantity());
            v.setNextAvailableAt(food.getNextAvailableAt());
            v.setOosReason(food.getOosReason());
            validations.add(v);

            if (isOOS) allValid = false;
        }

        CartValidationResponse response = new CartValidationResponse();
        response.setValid(allValid);
        response.setItems(validations);
        return response;
    }

    public Cart reorder(Long userId, Long orderId) {
        clearCart(userId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        for (OrderItem orderItem : order.getOrderItems()) {
            List<Long> modifierIds = new ArrayList<>();
            if (orderItem.getSelectedModifiers() != null) {
                modifierIds = orderItem.getSelectedModifiers().stream()
                        .map(m -> m.getModifier().getId())
                        .collect(Collectors.toList());
            }
            addToCart(userId, orderItem.getFoodItem().getId(), orderItem.getQuantity(), modifierIds);
        }

        return getCartByUserId(userId);
    }
}
