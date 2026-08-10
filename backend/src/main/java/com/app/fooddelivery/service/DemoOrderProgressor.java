package com.app.fooddelivery.service;

import com.app.fooddelivery.model.Order;
import com.app.fooddelivery.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Walks orders through their stages on a timer, for demonstrations only.
 *
 * This is not how the app works. A real order moves because a restaurant said
 * it did; a timer knows nothing about whether food was cooked or a rider left,
 * and would happily report "delivered" to someone standing at an empty door.
 *
 * It exists so the whole journey can be shown on one screen without a second
 * person playing the restaurant. Off unless explicitly switched on.
 */
@Component
public class DemoOrderProgressor {

    private static final Logger log = LoggerFactory.getLogger(DemoOrderProgressor.class);

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final OrderStatusFlow statusFlow;

    @Value("${demo.auto-progress-enabled:false}")
    private boolean enabled;

    /** How long an order rests at each stage before moving on. */
    @Value("${demo.auto-progress-seconds:30}")
    private int secondsPerStage;

    public DemoOrderProgressor(OrderRepository orderRepository, OrderService orderService,
            OrderStatusFlow statusFlow) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.statusFlow = statusFlow;
    }

    @Scheduled(fixedDelayString = "${demo.auto-progress-check-millis:10000}")
    public void advanceOrders() {
        if (!enabled) {
            return;
        }

        for (Order order : orderRepository.findAll()) {
            if (statusFlow.isTerminal(order.getStatus())) {
                continue;
            }

            String next = statusFlow.nextForwardStep(order);
            if (next == null) {
                continue;
            }

            LocalDateTime since = order.getStatusChangedAt() != null
                    ? order.getStatusChangedAt()
                    : order.getOrderDate();
            if (since == null
                    || Duration.between(since, LocalDateTime.now()).getSeconds() < secondsPerStage) {
                continue;
            }

            try {
                orderService.advanceStatus(order.getId(), next);
                log.info("Demo mode moved order {} to {}", order.getId(), next);
            } catch (RuntimeException e) {
                log.warn("Demo mode could not move order {}: {}", order.getId(), e.getMessage());
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}
