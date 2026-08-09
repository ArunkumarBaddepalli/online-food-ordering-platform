package com.app.fooddelivery.service;

import com.app.fooddelivery.model.*;
import com.app.fooddelivery.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Regression tests for the cart bugs fixed in Phase 01.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private FoodItemRepository foodItemRepository;
    @Mock private UserRepository userRepository;
    @Mock private ModifierRepository modifierRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private ModifierGroupRepository modifierGroupRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private RestaurantHoursValidator hoursValidator;

    @InjectMocks private CartService cartService;

    private Restaurant restaurant;
    private FoodItem pizza;
    private ModifierGroup crustGroup;
    private Modifier thinCrust;
    private Modifier extraCheese;

    @BeforeEach
    void setUp() {
        restaurant = new Restaurant();
        restaurant.setId(1L);
        restaurant.setIsOpen(true);
        restaurant.setAcceptsScheduledOrders(true);

        pizza = new FoodItem();
        pizza.setId(1L);
        pizza.setName("Margherita");
        pizza.setPrice(12.99);
        pizza.setInStock(true);
        pizza.setStockResetType("UNLIMITED");
        pizza.setStockQuantity(100);
        pizza.setRestaurant(restaurant);

        crustGroup = new ModifierGroup();
        crustGroup.setId(10L);
        crustGroup.setName("Choose Crust");
        crustGroup.setMinSelection(1);
        crustGroup.setMaxSelection(1);
        crustGroup.setRequired(true);
        crustGroup.setFoodItem(pizza);

        ModifierGroup toppings = new ModifierGroup();
        toppings.setId(11L);
        toppings.setName("Extra Toppings");
        toppings.setMinSelection(0);
        toppings.setMaxSelection(2);
        toppings.setRequired(false);
        toppings.setFoodItem(pizza);

        thinCrust = modifier(100L, "Thin Crust", 0.00, crustGroup);
        extraCheese = modifier(101L, "Extra Cheese", 1.50, toppings);

        when(hoursValidator.isRestaurantOpen(any())).thenReturn(true);
        when(foodItemRepository.findById(1L)).thenReturn(Optional.of(pizza));
        when(modifierGroupRepository.findByFoodItemId(1L)).thenReturn(List.of(crustGroup, toppings));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Modifier modifier(Long id, String name, double price, ModifierGroup group) {
        Modifier m = new Modifier();
        m.setId(id);
        m.setName(name);
        m.setPriceAdjustment(price);
        m.setAvailable(true);
        m.setModifierGroup(group);
        return m;
    }

    private Cart emptyCart() {
        User user = new User();
        user.setId(7L);
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setItems(new ArrayList<>());
        when(cartRepository.findByUserId(7L)).thenReturn(Optional.of(cart));
        return cart;
    }

    @Test
    @DisplayName("Changing quantity keeps modifier prices instead of dropping them")
    void quantityChangePreservesModifierPrices() {
        CartItem item = new CartItem();
        item.setId(50L);
        item.setFoodItem(pizza);
        item.setQuantity(1);

        CartItemModifier cheese = new CartItemModifier();
        cheese.setModifier(extraCheese);
        item.setSelectedModifiers(new ArrayList<>(List.of(cheese)));
        item.setTotalPrice(14.49); // 12.99 + 1.50

        when(cartItemRepository.findById(50L)).thenReturn(Optional.of(item));

        CartItem updated = cartService.updateCartItemQuantity(50L, 2);

        assertThat(updated.getTotalPrice())
                .as("(12.99 base + 1.50 cheese) x 2 — the cheese must not vanish")
                .isEqualTo(28.98);
    }

    @Test
    @DisplayName("Quantity below one is rejected")
    void rejectsZeroQuantity() {
        assertThatThrownBy(() -> cartService.updateCartItemQuantity(50L, 0))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("at least 1");
    }

    @Test
    @DisplayName("A required modifier group must be satisfied")
    void requiredGroupIsEnforced() {
        emptyCart();

        assertThatThrownBy(() -> cartService.addToCart(7L, 1L, 1, List.of()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Choose Crust");
    }

    @Test
    @DisplayName("Selecting more than maxSelection is rejected")
    void maxSelectionIsEnforced() {
        emptyCart();
        Modifier mushrooms = modifier(102L, "Mushrooms", 1.00, extraCheese.getModifierGroup());
        Modifier olives = modifier(103L, "Olives", 1.00, extraCheese.getModifierGroup());

        when(modifierRepository.findAllById(any()))
                .thenReturn(List.of(thinCrust, extraCheese, mushrooms, olives));

        assertThatThrownBy(() -> cartService.addToCart(7L, 1L, 1, List.of(100L, 101L, 102L, 103L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("at most 2");
    }

    @Test
    @DisplayName("An unavailable modifier is rejected")
    void unavailableModifierIsRejected() {
        emptyCart();
        thinCrust.setAvailable(false);
        when(modifierRepository.findAllById(any())).thenReturn(List.of(thinCrust));

        assertThatThrownBy(() -> cartService.addToCart(7L, 1L, 1, List.of(100L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not available");
    }

    @Test
    @DisplayName("A valid selection is accepted and priced with modifiers")
    void validSelectionIsAccepted() {
        Cart cart = emptyCart();
        when(modifierRepository.findAllById(any())).thenReturn(List.of(thinCrust, extraCheese));

        cartService.addToCart(7L, 1L, 2, List.of(100L, 101L));

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getTotalPrice())
                .as("(12.99 + 0.00 thin crust + 1.50 cheese) x 2")
                .isEqualTo(28.98);
    }

    @Test
    @DisplayName("An UNLIMITED item can be added in any quantity")
    void unlimitedItemIgnoresStockCeiling() {
        Cart cart = emptyCart();
        when(modifierRepository.findAllById(any())).thenReturn(List.of(thinCrust));

        cartService.addToCart(7L, 1L, 500, List.of(100L));

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(500);
    }

    @Test
    @DisplayName("A limited item is capped by its remaining stock, and null stock is treated as zero")
    void limitedItemRespectsStock() {
        emptyCart();
        pizza.setStockResetType("MANUAL");
        pizza.setStockQuantity(null);
        when(modifierRepository.findAllById(any())).thenReturn(List.of(thinCrust));

        assertThatThrownBy(() -> cartService.addToCart(7L, 1L, 1, List.of(100L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only 0 available")
                .isNotInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Adding a missing food item gives a clear message, not a bare NoSuchElementException")
    void missingFoodItemGivesClearError() {
        emptyCart();
        when(foodItemRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addToCart(7L, 999L, 1, List.of()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Food item not found");
    }
}
