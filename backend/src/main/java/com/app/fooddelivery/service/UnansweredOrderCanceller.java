package com.app.fooddelivery.service;

import com.app.fooddelivery.model.Order;
import com.app.fooddelivery.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cancels orders no restaurant ever answered.
 *
 * Without this an order sits at PLACED forever: the customer waits for food
 * that is not coming, and the stock stays reserved so nobody else can buy it.
 * Cancelling releases both.
 *
 * Only PLACED orders qualify. Once a restaurant has accepted, the order is
 * theirs to finish and is none of this job's business.
 */
@Component
public class UnansweredOrderCanceller {

    private static final Logger log = LoggerFactory.getLogger(UnansweredOrderCanceller.class);

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    /** How long a restaurant has to accept before the order is given up on. */
    @Value("${orders.auto-cancel-after-minutes:15}")
    private int cancelAfterMinutes;

    @Value("${orders.auto-cancel-enabled:true}")
    private boolean enabled;

    public UnansweredOrderCanceller(OrderRepository orderRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    @Scheduled(fixedDelayString = "${orders.auto-cancel-check-millis:60000}")
    public void cancelUnansweredOrders() {
        if (!enabled) {
            return;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(cancelAfterMinutes);
        List<Order> stale = orderRepository.findByStatusAndOrderDateBefore(OrderStatusFlow.PLACED, cutoff);

        for (Order order : stale) {
            // A scheduled order is not late, it is early: it was placed now for
            // delivery later, so the clock that matters is its delivery time.
            if (Boolean.TRUE.equals(order.getIsScheduled())) {
                LocalDateTime due = order.getScheduledDeliveryTime();
                if (due != null && due.isAfter(LocalDateTime.now())) {
                    continue;
                }
                // Marked scheduled but with no time set is broken data. Skipping
                // it would leave the order stranded for good, which is the very
                // thing this job exists to prevent, so treat it as ordinary.
            }

            try {
                orderService.cancelOrder(order.getId());
                log.info("Cancelled order {}: no response from the restaurant within {} minutes",
                        order.getId(), cancelAfterMinutes);
            } catch (RuntimeException e) {
                // One problem order must not stop the rest being cleaned up.
                log.warn("Could not auto-cancel order {}: {}", order.getId(), e.getMessage());
            }
        }
    }
}
