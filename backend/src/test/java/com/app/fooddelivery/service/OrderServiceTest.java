package com.app.fooddelivery.service;

import com.app.fooddelivery.model.*;
import com.app.fooddelivery.repository.CartRepository;
import com.app.fooddelivery.repository.FoodItemRepository;
import com.app.fooddelivery.repository.OrderRepository;
import com.app.fooddelivery.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Regression tests for the order placement bugs fixed in Phase 01.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CartRepository cartRepository;
    @Mock private FoodItemRepository foodItemRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private RestaurantHoursValidator hoursValidator;
    @Mock private GeocodingService geocodingService;
    @Mock private RazorpayPaymentService razorpayPaymentService;
    @Mock private DistanceCalculationService distanceCalculationService;

    // Real lifecycle rules rather than a mock — the transitions are the thing
    // under test in the cancellation cases below.
    @Spy private OrderStatusFlow statusFlow = new OrderStatusFlow();

    @InjectMocks private OrderService orderService;

    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        // No Spring context here, so the delivery flag would default to false
        // and every delivery order in these tests would be refused.
        ReflectionTestUtils.setField(orderService, "deliveryEnabled", true);

        restaurant = new Restaurant();
        restaurant.setId(1L);
        restaurant.setName("Test Kitchen");
        restaurant.setIsOpen(true);
        restaurant.setAcceptsScheduledOrders(true);
        // No coordinates, so delivery-radius geocoding is skipped.
        restaurant.setLatitude(null);
        restaurant.setLongitude(null);

        when(hoursValidator.validateOrderPlacement(any(), any()))
                .thenReturn(new RestaurantHoursValidator.ValidationResult(true, "ok"));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(foodItemRepository.save(any(FoodItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private FoodItem foodItem(Long id, String name, String resetType, Integer stock, double price) {
        FoodItem f = new FoodItem();
        f.setId(id);
        f.setName(name);
        f.setPrice(price);
        f.setInStock(true);
        f.setStockResetType(resetType);
        f.setStockQuantity(stock);
        f.setRestaurant(restaurant);
        return f;
    }

    private void givenCartWith(FoodItem item, int quantity) {
        User user = new User();
        user.setId(7L);

        CartItem cartItem = new CartItem();
        cartItem.setFoodItem(item);
        cartItem.setQuantity(quantity);
        cartItem.setTotalPrice(item.getPrice() * quantity);
        cartItem.setSelectedModifiers(new ArrayList<>());

        Cart cart = new Cart();
        cart.setUser(user);
        cart.setItems(new ArrayList<>(List.of(cartItem)));

        when(cartRepository.findByUserId(7L)).thenReturn(Optional.of(cart));
        when(foodItemRepository.findByIdWithLock(item.getId())).thenReturn(Optional.of(item));
    }

    @Test
    @DisplayName("UNLIMITED items never deplete, even after far more than 100 orders")
    void unlimitedItemsDoNotDeplete() {
        FoodItem pizza = foodItem(1L, "Margherita", "UNLIMITED", 100, 12.99);

        for (int i = 0; i < 200; i++) {
            givenCartWith(pizza, 1);
            orderService.placeOrder(7L, "1 Test Street", null, OrderType.DELIVERY);
        }

        assertThat(pizza.getStockQuantity())
                .as("UNLIMITED stock must not be decremented")
                .isEqualTo(100);
        assertThat(pizza.getInStock())
                .as("UNLIMITED item must stay on the menu")
                .isTrue();
    }

    @Test
    @DisplayName("DAILY items still deplete and go out of stock at zero")
    void dailyItemsStillDeplete() {
        FoodItem bread = foodItem(2L, "Garlic Bread", "DAILY", 2, 5.99);
        bread.setDailyRestockTime("09:00");

        givenCartWith(bread, 2);
        orderService.placeOrder(7L, "1 Test Street", null, OrderType.DELIVERY);

        assertThat(bread.getStockQuantity()).isZero();
        assertThat(bread.getInStock()).isFalse();
        assertThat(bread.getOosReason()).isEqualTo("Sold out today");
    }

    @Test
    @DisplayName("A null stockQuantity is treated as zero rather than throwing")
    void nullStockQuantityDoesNotCrash() {
        FoodItem mystery = foodItem(3L, "Legacy Row", "MANUAL", null, 9.99);
        givenCartWith(mystery, 1);

        assertThatThrownBy(() -> orderService.placeOrder(7L, "1 Test Street", null, OrderType.DELIVERY))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient stock")
                .isNotInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Placing an order creates a pending COD payment record")
    void orderGetsPaymentRecord() {
        FoodItem pizza = foodItem(1L, "Margherita", "UNLIMITED", 100, 12.99);
        givenCartWith(pizza, 2);

        Order order = orderService.placeOrder(7L, "1 Test Street", null, OrderType.DELIVERY);

        verify(paymentRepository).save(any(Payment.class));
        assertThat(order.getPayment()).isNotNull();
        assertThat(order.getPayment().getPaymentMethod()).isEqualTo("COD");
        assertThat(order.getPayment().getPaymentStatus()).isEqualTo("PENDING");
        assertThat(order.getPayment().getAmount()).isEqualTo(25.98);
    }

    @Test
    @DisplayName("Delivery orders are refused while home delivery is switched off")
    void deliveryRefusedWhenDisabled() {
        ReflectionTestUtils.setField(orderService, "deliveryEnabled", false);
        FoodItem pizza = foodItem(1L, "Margherita", "UNLIMITED", 100, 12.99);
        givenCartWith(pizza, 1);

        assertThatThrownBy(() -> orderService.placeOrder(7L, "1 Test Street", null, OrderType.DELIVERY))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not available yet");
    }

    @Test
    @DisplayName("Collection still works while home delivery is switched off")
    void pickupStillWorksWhenDeliveryDisabled() {
        ReflectionTestUtils.setField(orderService, "deliveryEnabled", false);
        FoodItem pizza = foodItem(1L, "Margherita", "UNLIMITED", 100, 12.99);
        givenCartWith(pizza, 1);

        assertThat(orderService.placeOrder(7L, null, null, OrderType.PICKUP).getOrderType())
                .isEqualTo(OrderType.PICKUP);
    }

    @Test
    @DisplayName("A new order waits at PLACED when auto-accept is off")
    void ordersWaitForManualAcceptanceByDefault() {
        FoodItem pizza = foodItem(1L, "Margherita", "UNLIMITED", 100, 12.99);
        givenCartWith(pizza, 1);

        Order order = orderService.placeOrder(7L, "1 Test Street", null, OrderType.DELIVERY);

        assertThat(order.getStatus()).isEqualTo("PLACED");
    }

    @Test
    @DisplayName("With auto-accept on, a new order is confirmed straight away")
    void autoAcceptConfirmsImmediately() {
        restaurant.setAutoAcceptOrders(true);
        FoodItem pizza = foodItem(1L, "Margherita", "UNLIMITED", 100, 12.99);
        givenCartWith(pizza, 1);

        Order order = orderService.placeOrder(7L, "1 Test Street", null, OrderType.DELIVERY);

        assertThat(order.getStatus())
                .as("only the Accept tap is skipped")
                .isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("Auto-accept does not skip past preparing")
    void autoAcceptStopsAtConfirmed() {
        restaurant.setAutoAcceptOrders(true);
        FoodItem pizza = foodItem(1L, "Margherita", "UNLIMITED", 100, 12.99);
        givenCartWith(pizza, 1);

        Order order = orderService.placeOrder(7L, "1 Test Street", null, OrderType.DELIVERY);

        assertThat(statusFlow.nextForwardStep(order)).isEqualTo("PREPARING");
        assertThat(statusFlow.canCancel(order.getStatus()))
                .as("a customer can still cancel a confirmed order")
                .isTrue();
    }

    @Test
    @DisplayName("Placing an order sets a fixed delivery target so the ETA can count down")
    void orderGetsEstimatedDeliveryTime() {
        FoodItem pizza = foodItem(1L, "Margherita", "UNLIMITED", 100, 12.99);
        givenCartWith(pizza, 1);

        Order order = orderService.placeOrder(7L, "1 Test Street", null, OrderType.DELIVERY);

        assertThat(order.getEstimatedDeliveryAt())
                .as("tracking page needs a fixed target, not a rolling estimate")
                .isNotNull();
    }

    @Test
    @DisplayName("Cancelling an order restores stock and cancels the payment")
    void cancelRestoresStockAndCancelsPayment() {
        FoodItem bread = foodItem(2L, "Garlic Bread", "DAILY", 0, 5.99);
        bread.setInStock(false);

        OrderItem item = new OrderItem();
        item.setFoodItem(bread);
        item.setQuantity(3);

        Payment payment = new Payment();
        payment.setPaymentStatus("PENDING");

        Order order = new Order();
        order.setId(99L);
        order.setStatus("PLACED");
        order.setOrderItems(new ArrayList<>(List.of(item)));
        order.setPayment(payment);

        when(orderRepository.findById(99L)).thenReturn(Optional.of(order));

        Order cancelled = orderService.cancelOrder(99L);

        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");
        assertThat(bread.getStockQuantity()).isEqualTo(3);
        assertThat(bread.getInStock()).isTrue();
        assertThat(payment.getPaymentStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("A delivered order cannot be cancelled")
    void deliveredOrderCannotBeCancelled() {
        Order order = new Order();
        order.setId(99L);
        order.setStatus("DELIVERED");
        when(orderRepository.findById(99L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot cancel order");
    }
}
