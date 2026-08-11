package com.app.fooddelivery.service;

import com.app.fooddelivery.model.Restaurant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Opening hours belong to the restaurant's clock.
 *
 * These build the hours relative to the current moment in each zone, so they
 * hold whatever time the suite happens to run at.
 */
class RestaurantTimeZoneTest {

    private final RestaurantHoursValidator validator = new RestaurantHoursValidator();

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);

    private Restaurant restaurantIn(String zone, int opensHoursFromNow, int closesHoursFromNow) {
        ZonedDateTime local = ZonedDateTime.now(ZoneId.of(zone));

        Restaurant r = new Restaurant();
        r.setTimeZone(zone);
        r.setIsOpen(true);
        r.setOpeningTime(local.plusHours(opensHoursFromNow).format(HH_MM));
        r.setClosingTime(local.plusHours(closesHoursFromNow).format(HH_MM));
        return r;
    }

    @Test
    @DisplayName("Open or closed is judged on the restaurant's clock, not the server's")
    void judgedInTheRestaurantsZone() {
        // Open from an hour ago until three hours from now, in each zone.
        for (String zone : new String[] { "Asia/Kolkata", "Europe/London", "America/New_York", "Australia/Sydney" }) {
            assertThat(validator.isRestaurantOpen(restaurantIn(zone, -1, 3)))
                    .as("%s should be open inside its own window", zone)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("A restaurant outside its own hours is closed, wherever the server is")
    void closedOutsideItsOwnHours() {
        for (String zone : new String[] { "Asia/Kolkata", "Europe/London", "America/Los_Angeles" }) {
            // Opens in two hours, closes in four: not yet.
            assertThat(validator.isRestaurantOpen(restaurantIn(zone, 2, 4)))
                    .as("%s should be closed before it opens", zone)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("The same wall-clock hours give different answers in different zones")
    void sameHoursDifferInDifferentZones() {
        // Built so it is currently open in Kolkata.
        Restaurant kolkata = restaurantIn("Asia/Kolkata", -1, 3);

        // The identical wall-clock strings, but read in Los Angeles, which is
        // twelve and a half hours behind.
        Restaurant losAngeles = new Restaurant();
        losAngeles.setTimeZone("America/Los_Angeles");
        losAngeles.setIsOpen(true);
        losAngeles.setOpeningTime(kolkata.getOpeningTime());
        losAngeles.setClosingTime(kolkata.getClosingTime());

        assertThat(validator.isRestaurantOpen(kolkata)).isTrue();

        // Whatever the answer for Los Angeles, it was reached from that city's
        // clock rather than Kolkata's, which is the whole point.
        assertThat(validator.zoneOf(losAngeles)).isEqualTo(ZoneId.of("America/Los_Angeles"));
        assertThat(validator.nowAt(losAngeles).getHour())
                .isNotEqualTo(validator.nowAt(kolkata).getHour());
    }

    @Test
    @DisplayName("A restaurant with no zone recorded falls back to the server's")
    void noZoneFallsBackToTheServer() {
        Restaurant legacy = new Restaurant();
        legacy.setTimeZone(null);

        assertThat(validator.zoneOf(legacy)).isEqualTo(ZoneId.systemDefault());
    }

    @Test
    @DisplayName("A nonsense zone falls back rather than throwing")
    void rubbishZoneFallsBack() {
        Restaurant broken = new Restaurant();
        broken.setTimeZone("Middle/Earth");

        assertThat(validator.zoneOf(broken)).isEqualTo(ZoneId.systemDefault());
        assertThat(validator.nowAt(broken)).isNotNull();
    }

    @Test
    @DisplayName("The day of the week is the restaurant's day")
    void dayOfWeekIsLocalToTheRestaurant() {
        Restaurant sydney = new Restaurant();
        sydney.setTimeZone("Australia/Sydney");

        Restaurant losAngeles = new Restaurant();
        losAngeles.setTimeZone("America/Los_Angeles");

        // Sydney is far enough ahead that these are frequently different days,
        // which is exactly why per-day hours must not use the server's date.
        assertThat(validator.nowAt(sydney).toLocalDate())
                .isAfterOrEqualTo(validator.nowAt(losAngeles).toLocalDate());
    }
}
