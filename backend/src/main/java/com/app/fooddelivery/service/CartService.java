package com.app.fooddelivery.service;

import com.app.fooddelivery.model.*;
import com.app.fooddelivery.repository.CartRepository;
import com.app.fooddelivery.repository.FoodItemRepository;
import com.app.fooddelivery.repository.UserRepository;
import com.app.fooddelivery.repository.ModifierRepository;
import com.app.fooddelivery.repository.OrderRepository;
import com.app.fooddelivery.dto.CartValidationResponse;
import com.app.fooddelivery.repository.CartItemRepository;
import com.app.fooddelivery.repository.ModifierGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private ModifierGroupRepository modifierGroupRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

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

    @Transactional
    public Cart addToCart(Long userId, Long foodId, int quantity, List<Long> modifierIds) {
        if (quantity < 1) {
            throw new RuntimeException("Quantity must be at least 1");
        }

        Cart cart = getCartByUserId(userId);
        FoodItem food = foodItemRepository.findById(foodId)
                .orElseThrow(() -> new RuntimeException("Food item not found"));

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

        validateModifierSelection(food, modifiers);

        Optional<CartItem> existingItemOpt = cart.getItems().stream()
                .filter(item -> item.getFoodItem().getId().equals(foodId))
                .filter(item -> areModifiersEqual(item, modifiers))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + quantity;
            assertStockAvailable(food, newQuantity);
            existingItem.setQuantity(newQuantity);
            updateItemPrice(existingItem);
        } else {
            assertStockAvailable(food, quantity);
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

    /**
     * Stock check that treats UNLIMITED items as always available and tolerates
     * a null stockQuantity on rows that were not created through JPA.
     */
    private void assertStockAvailable(FoodItem food, int wanted) {
        if ("UNLIMITED".equals(food.getStockResetType())) {
            return;
        }
        int available = food.getStockQuantity() == null ? 0 : food.getStockQuantity();
        if (wanted > available) {
            throw new RuntimeException("Cannot add more items. Only " + available + " available.");
        }
    }

    /**
     * Enforces the min/max/required rules configured on each modifier group,
     * and rejects modifiers that are unavailable or belong to another item.
     */
    private void validateModifierSelection(FoodItem food, List<Modifier> selected) {
        for (Modifier mod : selected) {
            if (Boolean.FALSE.equals(mod.getAvailable())) {
                throw new RuntimeException("Option is not available: " + mod.getName());
            }
            if (mod.getModifierGroup() == null
                    || !mod.getModifierGroup().getFoodItem().getId().equals(food.getId())) {
                throw new RuntimeException("Option does not belong to this item: " + mod.getName());
            }
        }

        for (ModifierGroup group : modifierGroupRepository.findByFoodItemId(food.getId())) {
            long chosen = selected.stream()
                    .filter(m -> m.getModifierGroup().getId().equals(group.getId()))
                    .count();

            int min = group.getMinSelection() == null ? 0 : group.getMinSelection();
            if (Boolean.TRUE.equals(group.getRequired()) && min < 1) {
                min = 1;
            }

            if (chosen < min) {
                throw new RuntimeException(
                        "Please choose at least " + min + " option(s) for: " + group.getName());
            }

            if (group.getMaxSelection() != null && chosen > group.getMaxSelection()) {
                throw new RuntimeException(
                        "You can choose at most " + group.getMaxSelection() + " option(s) for: " + group.getName());
            }
        }
    }

    /**
     * Changes the quantity of a cart item, keeping modifier prices intact.
     */
    @Transactional
    public CartItem updateCartItemQuantity(Long itemId, int quantity) {
        if (quantity < 1) {
            throw new RuntimeException("Quantity must be at least 1");
        }

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        FoodItem food = item.getFoodItem();
        if (!"UNLIMITED".equals(food.getStockResetType())) {
            int available = food.getStockQuantity() == null ? 0 : food.getStockQuantity();
            if (quantity > available) {
                throw new RuntimeException("Cannot update quantity. Only " + available + " available.");
            }
        }

        item.setQuantity(quantity);
        updateItemPrice(item);
        return cartItemRepository.save(item);
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

    @Transactional
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
