package com.app.fooddelivery.service;

import com.app.fooddelivery.model.Order;
import com.app.fooddelivery.model.OrderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The order lifecycle rules.
 */
class OrderStatusFlowTest {

    private final OrderStatusFlow flow = new OrderStatusFlow();

    private Order order(String status, OrderType type) {
        Order o = new Order();
        o.setStatus(status);
        o.setOrderType(type);
        return o;
    }

    @Test
    @DisplayName("A delivery order runs PLACED -> CONFIRMED -> PREPARING -> OUT_FOR_DELIVERY -> DELIVERED")
    void deliveryHappyPath() {
        Order o = order("PLACED", OrderType.DELIVERY);

        o.setStatus(flow.validateTransition(o, "CONFIRMED"));
        o.setStatus(flow.validateTransition(o, "PREPARING"));
        o.setStatus(flow.validateTransition(o, "OUT_FOR_DELIVERY"));
        o.setStatus(flow.validateTransition(o, "DELIVERED"));

        assertThat(o.getStatus()).isEqualTo("DELIVERED");
        assertThat(flow.isTerminal(o.getStatus())).isTrue();
        assertThat(flow.nextForwardStep(o)).isNull();
    }

    @Test
    @DisplayName("A pickup order runs PREPARING -> READY_FOR_PICKUP -> PICKED_UP")
    void pickupUsesItsOwnStages() {
        Order o = order("PREPARING", OrderType.PICKUP);

        assertThat(flow.nextForwardStep(o)).isEqualTo("READY_FOR_PICKUP");

        o.setStatus(flow.validateTransition(o, "READY_FOR_PICKUP"));
        o.setStatus(flow.validateTransition(o, "PICKED_UP"));

        assertThat(o.getStatus()).isEqualTo("PICKED_UP");
        assertThat(flow.isTerminal(o.getStatus())).isTrue();
    }

    @Test
    @DisplayName("A pickup order is never sent out for delivery")
    void pickupCannotGoOutForDelivery() {
        Order o = order("PREPARING", OrderType.PICKUP);

        assertThatThrownBy(() -> flow.validateTransition(o, "OUT_FOR_DELIVERY"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("READY_FOR_PICKUP");
    }

    @Test
    @DisplayName("A delivery order is never marked ready for pickup")
    void deliveryCannotBeReadyForPickup() {
        Order o = order("PREPARING", OrderType.DELIVERY);

        assertThatThrownBy(() -> flow.validateTransition(o, "READY_FOR_PICKUP"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("OUT_FOR_DELIVERY");
    }

    @Test
    @DisplayName("Stages cannot be skipped")
    void cannotSkipStages() {
        Order o = order("PLACED", OrderType.DELIVERY);

        assertThatThrownBy(() -> flow.validateTransition(o, "DELIVERED"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot move an order from PLACED to DELIVERED");
    }

    @Test
    @DisplayName("An order cannot move backwards")
    void cannotGoBackwards() {
        Order o = order("OUT_FOR_DELIVERY", OrderType.DELIVERY);

        assertThatThrownBy(() -> flow.validateTransition(o, "PREPARING"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("A finished order cannot be changed again")
    void terminalStatusesAreFinal() {
        for (String terminal : new String[] { "DELIVERED", "PICKED_UP", "CANCELLED" }) {
            Order o = order(terminal, OrderType.DELIVERY);
            assertThat(flow.isTerminal(terminal)).isTrue();
            assertThatThrownBy(() -> flow.validateTransition(o, "CONFIRMED"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("cannot be changed further");
        }
    }

    @Test
    @DisplayName("Cancelling is allowed until the kitchen starts, then blocked")
    void cancelWindowClosesAtPreparing() {
        assertThat(flow.canCancel("PLACED")).isTrue();
        assertThat(flow.canCancel("CONFIRMED")).isTrue();

        assertThat(flow.canCancel("PREPARING")).isFalse();
        assertThat(flow.canCancel("OUT_FOR_DELIVERY")).isFalse();
        assertThat(flow.canCancel("READY_FOR_PICKUP")).isFalse();
        assertThat(flow.canCancel("DELIVERED")).isFalse();
    }

    @Test
    @DisplayName("An unknown or empty status is rejected with the legal options")
    void unknownStatusRejected() {
        Order o = order("PLACED", OrderType.DELIVERY);

        assertThatThrownBy(() -> flow.validateTransition(o, "BANANA"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Allowed from here: CONFIRMED, CANCELLED");

        assertThatThrownBy(() -> flow.validateTransition(o, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("empty status");
    }

    @Test
    @DisplayName("Re-applying the current status is rejected")
    void sameStatusRejected() {
        Order o = order("CONFIRMED", OrderType.DELIVERY);

        assertThatThrownBy(() -> flow.validateTransition(o, "confirmed"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already CONFIRMED");
    }

    @Test
    @DisplayName("Owner-facing labels describe the next action")
    void nextStepLabels() {
        assertThat(flow.nextForwardLabel(order("PLACED", OrderType.DELIVERY))).isEqualTo("Accept order");
        assertThat(flow.nextForwardLabel(order("CONFIRMED", OrderType.DELIVERY))).isEqualTo("Start preparing");
        assertThat(flow.nextForwardLabel(order("PREPARING", OrderType.DELIVERY))).isEqualTo("Send out for delivery");
        assertThat(flow.nextForwardLabel(order("PREPARING", OrderType.PICKUP))).isEqualTo("Mark ready for pickup");
        assertThat(flow.nextForwardLabel(order("DELIVERED", OrderType.DELIVERY))).isNull();
    }

    @Test
    @DisplayName("A null status is treated as a freshly placed order")
    void nullStatusTreatedAsPlaced() {
        Order o = order(null, OrderType.DELIVERY);

        assertThat(flow.allowedNext(o)).containsExactly("CONFIRMED", "CANCELLED");
    }
}
