package com.app.fooddelivery.service;

import com.app.fooddelivery.model.Restaurant;
import com.app.fooddelivery.model.RestaurantOperatingHours;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

@Service
public class RestaurantHoursValidator {

    /**
     * Formatters are pinned to Locale.US and made case-insensitive on purpose.
     * Without the locale, "11:00 PM" fails to parse under locales such as en_IN
     * whose CLDR data uses lowercase am/pm — and an unparsed time silently made
     * every restaurant look permanently open.
     */
    private static final DateTimeFormatter[] TIME_FORMATS = {
            caseInsensitive("hh:mm a"),
            caseInsensitive("h:mm a"),
            caseInsensitive("HH:mm"),
            caseInsensitive("H:mm")
    };

    private static DateTimeFormatter caseInsensitive(String pattern) {
        return new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(pattern)
                .toFormatter(Locale.US);
    }

    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty())
            return null;
        for (DateTimeFormatter formatter : TIME_FORMATS) {
            try {
                return LocalTime.parse(timeStr.trim(), formatter);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private boolean isTimeInRange(LocalTime time, LocalTime open, LocalTime close) {
        if (open == null || close == null) return true;
        if (close.isBefore(open)) {
            // Crosses midnight
            return !time.isBefore(open) || time.isBefore(close);
        }
        // Inclusive of the opening minute, so 10:00 counts as open at 10:00.
        return !time.isBefore(open) && time.isBefore(close);
    }

    /**
     * Get today's operating hours from per-day config if available.
     */
    public RestaurantOperatingHours getTodayHours(Restaurant restaurant) {
        if (restaurant.getOperatingHours() == null || restaurant.getOperatingHours().isEmpty()) {
            return null;
        }
        String today = LocalDate.now().getDayOfWeek().name();
        return restaurant.getOperatingHours().stream()
                .filter(h -> today.equals(h.getDayOfWeek()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Check if restaurant is currently open. Uses per-day hours if available,
     * falls back to legacy openingTime/closingTime.
     */
    public boolean isRestaurantOpen(Restaurant restaurant) {
        if (restaurant.getIsOpen() == null || !restaurant.getIsOpen()) {
            return false;
        }

        RestaurantOperatingHours todayHours = getTodayHours(restaurant);

        if (todayHours != null) {
            if (Boolean.FALSE.equals(todayHours.getIsOpen())) return false;
            LocalTime open = parseTime(todayHours.getOpenTime());
            LocalTime close = parseTime(todayHours.getCloseTime());
            return isTimeInRange(LocalTime.now(), open, close);
        }

        // Fall back to legacy single pair
        if (restaurant.getOpeningTime() == null || restaurant.getClosingTime() == null) {
            return true;
        }
        try {
            LocalTime open = parseTime(restaurant.getOpeningTime());
            LocalTime close = parseTime(restaurant.getClosingTime());
            return isTimeInRange(LocalTime.now(), open, close);
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Get next open time. Scans next 7 days.
     */
    public LocalDateTime getNextOpenTime(Restaurant restaurant) {
        if (restaurant.getOperatingHours() == null || restaurant.getOperatingHours().isEmpty()) {
            return null;
        }
        for (int i = 1; i <= 7; i++) {
            LocalDate date = LocalDate.now().plusDays(i);
            String dayName = date.getDayOfWeek().name();
            java.util.Optional<RestaurantOperatingHours> found = restaurant.getOperatingHours().stream()
                    .filter(h -> dayName.equals(h.getDayOfWeek()) && Boolean.TRUE.equals(h.getIsOpen()))
                    .findFirst();
            if (found.isPresent()) {
                LocalTime open = parseTime(found.get().getOpenTime());
                if (open != null) {
                    return LocalDateTime.of(date, open);
                }
            }
        }
        return null;
    }

    /**
     * Validate if scheduled delivery time is during operating hours.
     */
    public boolean isValidScheduledTime(Restaurant restaurant, LocalDateTime scheduledTime) {
        RestaurantOperatingHours dayHours = null;
        if (restaurant.getOperatingHours() != null && !restaurant.getOperatingHours().isEmpty()) {
            String dayName = scheduledTime.getDayOfWeek().name();
            dayHours = restaurant.getOperatingHours().stream()
                    .filter(h -> dayName.equals(h.getDayOfWeek()))
                    .findFirst()
                    .orElse(null);
        }

        if (dayHours != null) {
            if (Boolean.FALSE.equals(dayHours.getIsOpen())) return false;
            LocalTime open = parseTime(dayHours.getOpenTime());
            LocalTime close = parseTime(dayHours.getCloseTime());
            return isTimeInRange(scheduledTime.toLocalTime(), open, close);
        }

        // Fallback to legacy
        if (restaurant.getOpeningTime() == null || restaurant.getClosingTime() == null) {
            return true;
        }
        try {
            LocalTime open = parseTime(restaurant.getOpeningTime());
            LocalTime close = parseTime(restaurant.getClosingTime());
            return isTimeInRange(scheduledTime.toLocalTime(), open, close);
        } catch (Exception e) {
            return false;
        }
    }

    public String getNextOpeningMessage(Restaurant restaurant) {
        if (restaurant.getOpeningTime() == null) {
            return "Restaurant hours not set";
        }
        return "Opens at " + restaurant.getOpeningTime();
    }

    public String getClosingMessage(Restaurant restaurant) {
        if (restaurant.getClosingTime() == null) {
            return "Open 24/7";
        }
        return "Closes at " + restaurant.getClosingTime();
    }

    /**
     * Validate order placement.
     */
    public ValidationResult validateOrderPlacement(Restaurant restaurant, LocalDateTime scheduledTime) {
        if (restaurant.getIsOpen() == null || !restaurant.getIsOpen()) {
            return new ValidationResult(false, "Restaurant is currently not accepting orders");
        }

        if (scheduledTime == null) {
            if (!isRestaurantOpen(restaurant)) {
                return new ValidationResult(false,
                        "Restaurant is currently closed. " + getNextOpeningMessage(restaurant));
            }
            return new ValidationResult(true, "Order can be placed");
        }

        if (!Boolean.TRUE.equals(restaurant.getAcceptsScheduledOrders())) {
            return new ValidationResult(false, "Restaurant does not accept scheduled orders");
        }

        if (!isValidScheduledTime(restaurant, scheduledTime)) {
            return new ValidationResult(false,
                    "Scheduled time must be during restaurant hours: " +
                            restaurant.getOpeningTime() + " - " + restaurant.getClosingTime());
        }

        return new ValidationResult(true, "Scheduled order can be placed");
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }
}
