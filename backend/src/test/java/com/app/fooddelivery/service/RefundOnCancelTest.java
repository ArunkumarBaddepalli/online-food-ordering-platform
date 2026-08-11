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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Cancelling an order that was actually paid for has to return the money.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RefundOnCancelTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CartRepository cartRepository;
    @Mock private FoodItemRepository foodItemRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private RestaurantHoursValidator hoursValidator;
    @Mock private GeocodingService geocodingService;
    @Mock private DistanceCalculationService distanceCalculationService;
    @Mock private RazorpayPaymentService razorpayPaymentService;
    @Mock private NotificationEmailService notifications;

    @Spy private OrderStatusFlow statusFlow = new OrderStatusFlow();

    @InjectMocks private OrderService orderService;

    private Order order;
    private Payment payment;

    @BeforeEach
    void setUp() {
        FoodItem item = new FoodItem();
        item.setId(1L);
        item.setStockResetType("UNLIMITED");

        OrderItem line = new OrderItem();
        line.setFoodItem(item);
        line.setQuantity(1);

        payment = new Payment();
        payment.setId(9L);
        payment.setAmount(20.0);

        order = new Order();
        order.setId(5L);
        order.setStatus("PLACED");
        order.setOrderItems(new ArrayList<>(List.of(line)));
        order.setPayment(payment);

        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Cancelling a paid online order refunds it")
    void paidOnlineOrderIsRefunded() {
        payment.setPaymentMethod("ONLINE");
        payment.setPaymentStatus("PAID");

        orderService.cancelOrder(5L);

        verify(razorpayPaymentService).refund(payment);
    }

    @Test
    @DisplayName("A cash order has nothing to refund")
    void cashOrderIsNotRefunded() {
        payment.setPaymentMethod("COD");
        payment.setPaymentStatus("PENDING");

        orderService.cancelOrder(5L);

        verify(razorpayPaymentService, never()).refund(any());
        assertThat(payment.getPaymentStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("An online order that was never paid is not refunded")
    void unpaidOnlineOrderIsNotRefunded() {
        payment.setPaymentMethod("ONLINE");
        payment.setPaymentStatus("PENDING");

        orderService.cancelOrder(5L);

        verify(razorpayPaymentService, never()).refund(any());
        assertThat(payment.getPaymentStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("Cancelling still works when there is no payment record at all")
    void missingPaymentDoesNotBreakCancellation() {
        order.setPayment(null);

        assertThat(orderService.cancelOrder(5L).getStatus()).isEqualTo("CANCELLED");
        verify(razorpayPaymentService, never()).refund(any());
    }
}
