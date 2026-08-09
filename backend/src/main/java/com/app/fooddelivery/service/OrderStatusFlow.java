package com.app.fooddelivery.service;

import com.app.fooddelivery.model.Order;
import com.app.fooddelivery.model.OrderType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * The order lifecycle, in one place.
 *
 * Delivery: PLACED -> CONFIRMED -> PREPARING -> OUT_FOR_DELIVERY -> DELIVERED
 * Pickup:   PLACED -> CONFIRMED -> PREPARING -> READY_FOR_PICKUP -> PICKED_UP
 *
 * PLACED and CONFIRMED may also be cancelled. Once the kitchen starts
 * (PREPARING) the order can no longer be cancelled.
 */
@Component
public class OrderStatusFlow {

    public static final String PLACED = "PLACED";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String PREPARING = "PREPARING";
    public static final String OUT_FOR_DELIVERY = "OUT_FOR_DELIVERY";
    public static final String READY_FOR_PICKUP = "READY_FOR_PICKUP";
    public static final String DELIVERED = "DELIVERED";
    public static final String PICKED_UP = "PICKED_UP";
    public static final String CANCELLED = "CANCELLED";

    private static final List<String> TERMINAL = List.of(DELIVERED, PICKED_UP, CANCELLED);

    /** Statuses a customer is still allowed to cancel from. */
    private static final List<String> CANCELLABLE = List.of(PLACED, CONFIRMED);

    private static final Map<String, String> NEXT_STEP_LABELS = Map.of(
            CONFIRMED, "Accept order",
            PREPARING, "Start preparing",
            OUT_FOR_DELIVERY, "Send out for delivery",
            READY_FOR_PICKUP, "Mark ready for pickup",
            DELIVERED, "Mark delivered",
            PICKED_UP, "Mark picked up");

    private boolean isPickup(Order order) {
        return order.getOrderType() == OrderType.PICKUP;
    }

    /** Statuses this order may legally move to right now. */
    public List<String> allowedNext(Order order) {
        String current = order.getStatus() == null ? PLACED : order.getStatus();

        switch (current) {
            case PLACED:
                return List.of(CONFIRMED, CANCELLED);
            case CONFIRMED:
                return List.of(PREPARING, CANCELLED);
            case PREPARING:
                return List.of(isPickup(order) ? READY_FOR_PICKUP : OUT_FOR_DELIVERY);
            case OUT_FOR_DELIVERY:
                return List.of(DELIVERED);
            case READY_FOR_PICKUP:
                return List.of(PICKED_UP);
            default:
                return List.of();
        }
    }

    /**
     * The single forward step an owner would take next, excluding cancellation.
     * Null when the order is finished.
     */
    public String nextForwardStep(Order order) {
        return allowedNext(order).stream()
                .filter(s -> !CANCELLED.equals(s))
                .findFirst()
                .orElse(null);
    }

    public String nextForwardLabel(Order order) {
        String next = nextForwardStep(order);
        return next == null ? null : NEXT_STEP_LABELS.getOrDefault(next, next);
    }

    public boolean isTerminal(String status) {
        return status != null && TERMINAL.contains(status);
    }

    public boolean canCancel(String status) {
        return status != null && CANCELLABLE.contains(status);
    }

    /**
     * Validates a requested move and returns the normalised status.
     * Throws with the legal options when the move is not allowed.
     */
    public String validateTransition(Order order, String requested) {
        String target = requested == null ? "" : requested.trim().toUpperCase();
        String current = order.getStatus() == null ? PLACED : order.getStatus();

        if (target.equals(current)) {
            throw new RuntimeException("Order is already " + current);
        }

        List<String> allowed = allowedNext(order);
        if (allowed.isEmpty()) {
            throw new RuntimeException("Order is already " + current + " and cannot be changed further.");
        }

        if (!allowed.contains(target)) {
            throw new RuntimeException("Cannot move an order from " + current + " to "
                    + (target.isEmpty() ? "an empty status" : target)
                    + ". Allowed from here: " + String.join(", ", allowed));
        }

        return target;
    }
}
