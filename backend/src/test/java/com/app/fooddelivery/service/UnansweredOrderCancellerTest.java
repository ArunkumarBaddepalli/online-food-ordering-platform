package com.app.fooddelivery.service;

import com.app.fooddelivery.model.Order;
import com.app.fooddelivery.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UnansweredOrderCancellerTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderService orderService;

    private UnansweredOrderCanceller canceller;

    @BeforeEach
    void setUp() {
        canceller = new UnansweredOrderCanceller(orderRepository, orderService);
        ReflectionTestUtils.setField(canceller, "cancelAfterMinutes", 15);
        ReflectionTestUtils.setField(canceller, "enabled", true);
    }

    private Order order(Long id, boolean scheduled, LocalDateTime scheduledFor) {
        Order o = new Order();
        o.setId(id);
        o.setStatus("PLACED");
        o.setOrderDate(LocalDateTime.now().minusHours(2));
        o.setIsScheduled(scheduled);
        o.setScheduledDeliveryTime(scheduledFor);
        return o;
    }

    private void given(Order... orders) {
        when(orderRepository.findByStatusAndOrderDateBefore(eq("PLACED"), any()))
                .thenReturn(List.of(orders));
    }

    @Test
    @DisplayName("An order nobody accepted is cancelled")
    void unansweredOrderIsCancelled() {
        given(order(10L, false, null));

        canceller.cancelUnansweredOrders();

        verify(orderService).cancelOrder(10L);
    }

    @Test
    @DisplayName("Only orders older than the cutoff are looked at")
    void cutoffIsApplied() {
        given();

        canceller.cancelUnansweredOrders();

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(orderRepository).findByStatusAndOrderDateBefore(eq("PLACED"), cutoff.capture());

        assertThat(cutoff.getValue())
                .isBefore(LocalDateTime.now().minusMinutes(14))
                .isAfter(LocalDateTime.now().minusMinutes(16));
    }

    @Test
    @DisplayName("A scheduled order due later is left alone")
    void scheduledOrderForLaterIsNotCancelled() {
        // Placed this morning for dinner tonight: not unanswered, just early.
        given(order(11L, true, LocalDateTime.now().plusHours(5)));

        canceller.cancelUnansweredOrders();

        verify(orderService, never()).cancelOrder(anyLong());
    }

    @Test
    @DisplayName("A scheduled order whose time has passed is cancelled")
    void scheduledOrderPastItsTimeIsCancelled() {
        given(order(12L, true, LocalDateTime.now().minusHours(1)));

        canceller.cancelUnansweredOrders();

        verify(orderService).cancelOrder(12L);
    }

    @Test
    @DisplayName("A scheduled order with no time set is treated as ordinary")
    void scheduledOrderWithoutATimeIsCancelled() {
        given(order(13L, true, null));

        canceller.cancelUnansweredOrders();

        verify(orderService).cancelOrder(13L);
    }

    @Test
    @DisplayName("One failure does not stop the rest being cleaned up")
    void oneBadOrderDoesNotStopTheSweep() {
        given(order(20L, false, null), order(21L, false, null), order(22L, false, null));
        doThrow(new RuntimeException("boom")).when(orderService).cancelOrder(21L);

        canceller.cancelUnansweredOrders();

        verify(orderService).cancelOrder(20L);
        verify(orderService).cancelOrder(21L);
        verify(orderService).cancelOrder(22L);
    }

    @Test
    @DisplayName("Nothing happens when the sweep is switched off")
    void disabledDoesNothing() {
        ReflectionTestUtils.setField(canceller, "enabled", false);
        given(order(30L, false, null));

        canceller.cancelUnansweredOrders();

        verifyNoInteractions(orderRepository);
        verifyNoInteractions(orderService);
    }
}
