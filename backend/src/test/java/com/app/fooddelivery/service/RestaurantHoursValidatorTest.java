package com.app.fooddelivery.service;

import com.app.fooddelivery.model.Restaurant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Opening-hours regression tests.
 */
class RestaurantHoursValidatorTest {

    private final RestaurantHoursValidator validator = new RestaurantHoursValidator();

    private Restaurant open24Style(String open, String close) {
        Restaurant r = new Restaurant();
        r.setIsOpen(true);
        r.setAcceptsScheduledOrders(true);
        r.setOpeningTime(open);
        r.setClosingTime(close);
        return r;
    }

    @Test
    @DisplayName("A restaurant is open at exactly its opening minute")
    void openAtExactOpeningTime() {
        LocalTime now = LocalTime.now();
        // Opening time set to right now; previously the strict comparison made
        // this report "closed".
        Restaurant r = open24Style(String.format("%02d:%02d", now.getHour(), now.getMinute()), "23:59");

        assertThat(validator.isRestaurantOpen(r)).isTrue();
    }

    @Test
    @DisplayName("A closed restaurant is reported closed regardless of hours")
    void manuallyClosedWins() {
        Restaurant r = open24Style("00:00", "23:59");
        r.setIsOpen(false);

        assertThat(validator.isRestaurantOpen(r)).isFalse();
    }

    @Test
    @DisplayName("A null acceptsScheduledOrders does not throw when scheduling")
    void nullAcceptsScheduledOrdersDoesNotCrash() {
        Restaurant r = open24Style("00:00", "23:59");
        r.setAcceptsScheduledOrders(null);

        RestaurantHoursValidator.ValidationResult result =
                validator.validateOrderPlacement(r, LocalDateTime.now().plusHours(1));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).contains("does not accept scheduled orders");
    }

    @Test
    @DisplayName("A null isOpen does not throw")
    void nullIsOpenDoesNotCrash() {
        Restaurant r = open24Style("00:00", "23:59");
        r.setIsOpen(null);

        assertThat(validator.isRestaurantOpen(r)).isFalse();
        assertThat(validator.validateOrderPlacement(r, null).isValid()).isFalse();
    }

    @Test
    @DisplayName("Hours that cross midnight are handled")
    void crossesMidnight() {
        Restaurant r = open24Style("11:00 PM", "2:00 AM");
        LocalTime now = LocalTime.now();

        boolean expected = !now.isBefore(LocalTime.of(23, 0)) || now.isBefore(LocalTime.of(2, 0));
        assertThat(validator.isRestaurantOpen(r)).isEqualTo(expected);
    }

    @Test
    @DisplayName("AM/PM hours parse under any default locale, in either case")
    void amPmParsesRegardlessOfLocale() {
        // Under en_IN the AM/PM markers are lowercase in CLDR, so unpinned
        // formatters failed to parse "11:00 PM" and every restaurant then
        // looked permanently open.
        Locale original = Locale.getDefault();
        try {
            for (Locale locale : new Locale[] { Locale.US, new Locale("en", "IN"), Locale.FRANCE }) {
                Locale.setDefault(locale);

                // A window that definitely excludes "now" in every timezone-free case:
                // opens one minute from now and closes two minutes from now.
                LocalTime now = LocalTime.now();
                LocalTime open = now.plusMinutes(1);
                LocalTime close = now.plusMinutes(2);

                Restaurant closedNow = open24Style(format12h(open), format12h(close));
                assertThat(validator.isRestaurantOpen(closedNow))
                        .as("locale %s: outside its window the restaurant must read closed", locale)
                        .isFalse();

                Restaurant openNow = open24Style(format12h(now.minusMinutes(1)), format12h(now.plusMinutes(30)));
                assertThat(validator.isRestaurantOpen(openNow))
                        .as("locale %s: inside its window the restaurant must read open", locale)
                        .isTrue();
            }
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    @DisplayName("Lowercase am/pm also parses")
    void lowercaseAmPmParses() {
        Restaurant r = open24Style("12:00 am", "11:59 pm");

        assertThat(validator.isRestaurantOpen(r)).isTrue();
    }

    private String format12h(LocalTime t) {
        return t.format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a", Locale.US));
    }

    @Test
    @DisplayName("An order can be placed while open")
    void orderAllowedWhileOpen() {
        Restaurant r = open24Style("00:00", "23:59");

        assertThat(validator.validateOrderPlacement(r, null).isValid()).isTrue();
    }
}
