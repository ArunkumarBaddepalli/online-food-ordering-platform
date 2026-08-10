package com.app.fooddelivery.service;

import com.app.fooddelivery.model.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * ETA regression tests. The old implementation returned "now + constant" on
 * every call, so the tracking page never counted down.
 */
class OrderETAServiceTest {

    private final OrderETAService service = new OrderETAService();

    private Order order(String status, LocalDateTime target) {
        Order o = new Order();
        o.setStatus(status);
        o.setEstimatedDeliveryAt(target);
        return o;
    }

    @Test
    @DisplayName("Remaining minutes shrink as the target approaches")
    void etaCountsDown() {
        Order far = order("PLACED", LocalDateTime.now().plusMinutes(30));
        Order near = order("PLACED", LocalDateTime.now().plusMinutes(5));

        int farMinutes = service.calculateETA(far).getMinutesRemaining();
        int nearMinutes = service.calculateETA(near).getMinutesRemaining();

        assertThat(farMinutes).isGreaterThan(nearMinutes);
        assertThat(farMinutes).isBetween(28, 30);
        assertThat(nearMinutes).isBetween(3, 5);
    }

    @Test
    @DisplayName("A target in the past reports zero, never a negative countdown")
    void overdueOrderReportsZero() {
        Order late = order("OUT_FOR_DELIVERY", LocalDateTime.now().minusMinutes(20));

        assertThat(service.calculateETA(late).getMinutesRemaining()).isZero();
    }

    @Test
    @DisplayName("A null status does not throw")
    void nullStatusDoesNotCrash() {
        Order weird = order(null, LocalDateTime.now().plusMinutes(10));

        assertThatCode(() -> service.calculateETA(weird)).doesNotThrowAnyException();
        assertThat(service.calculateETA(weird).getStatusMessage()).isEqualTo("Processing your order");
    }

    @Test
    @DisplayName("Orders placed before estimatedDeliveryAt existed measure from when they were placed")
    void legacyOrderFallsBack() {
        Order justPlaced = order("CONFIRMED", null);
        justPlaced.setOrderDate(LocalDateTime.now());

        assertThat(service.calculateETA(justPlaced).getMinutesRemaining()).isBetween(18, 20);
        assertThat(service.calculateETA(justPlaced).getEstimatedDeliveryTime()).isNotNull();
    }

    @Test
    @DisplayName("A months-old order does not keep claiming it is minutes away")
    void staleLegacyOrderDoesNotClaimAnEta() {
        Order ancient = order("PLACED", null);
        ancient.setOrderDate(LocalDateTime.now().minusDays(60));

        assertThat(service.calculateETA(ancient).getMinutesRemaining())
                .as("its estimate expired two months ago")
                .isZero();
    }

    @Test
    @DisplayName("Delivered and cancelled orders report zero minutes")
    void terminalStatusesReportZero() {
        assertThat(service.calculateETA(order("DELIVERED", LocalDateTime.now().plusMinutes(30)))
                .getMinutesRemaining()).isZero();
        assertThat(service.calculateETA(order("CANCELLED", LocalDateTime.now().plusMinutes(30)))
                .getMinutesRemaining()).isZero();
    }

    @Test
    @DisplayName("Status messages match the order stage")
    void statusMessages() {
        LocalDateTime target = LocalDateTime.now().plusMinutes(10);

        assertThat(service.calculateETA(order("PLACED", target)).getStatusMessage())
                .isEqualTo("Order received, awaiting confirmation");
        assertThat(service.calculateETA(order("OUT_FOR_DELIVERY", target)).getStatusMessage())
                .isEqualTo("Your order is on the way");
        assertThat(service.calculateETA(order("DELIVERED", target)).getStatusMessage())
                .isEqualTo("Order delivered");
    }
}
