package com.app.fooddelivery.controller;

import com.app.fooddelivery.model.Restaurant;
import com.app.fooddelivery.repository.RestaurantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
public class RestaurantSettingsController {

    @Autowired
    private RestaurantRepository restaurantRepository;

    // Get restaurant settings
    @GetMapping("/{id}/settings")
    public ResponseEntity<Restaurant> getRestaurantSettings(@PathVariable Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        return ResponseEntity.ok(restaurant);
    }

    // Update restaurant settings
    @PutMapping("/{id}/settings")
    public ResponseEntity<Restaurant> updateRestaurantSettings(
            @PathVariable Long id,
            @RequestBody Restaurant updatedRestaurant) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        // Update settings
        restaurant.setPhone(updatedRestaurant.getPhone());
        restaurant.setEmail(updatedRestaurant.getEmail());
        restaurant.setOpeningTime(updatedRestaurant.getOpeningTime());
        restaurant.setClosingTime(updatedRestaurant.getClosingTime());
        restaurant.setIsOpen(updatedRestaurant.getIsOpen());
        restaurant.setAcceptsScheduledOrders(updatedRestaurant.getAcceptsScheduledOrders());
        restaurant.setSlotDurationMinutes(updatedRestaurant.getSlotDurationMinutes());

        return ResponseEntity.ok(restaurantRepository.save(restaurant));
    }

    // Generate time slots for scheduled orders
    @GetMapping("/{id}/time-slots")
    public ResponseEntity<List<String>> getAvailableTimeSlots(@PathVariable Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        if (!Boolean.TRUE.equals(restaurant.getAcceptsScheduledOrders())) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        List<String> slots = generateTimeSlots(
                restaurant.getOpeningTime(),
                restaurant.getClosingTime(),
                restaurant.getSlotDurationMinutes());

        return ResponseEntity.ok(slots);
    }

    private List<String> generateTimeSlots(String openingTime, String closingTime, Integer slotDuration) {
        List<String> slots = new ArrayList<>();
        // A zero or negative step would never advance the loop below and would
        // hang the request thread.
        int step = (slotDuration == null || slotDuration <= 0) ? 30 : slotDuration;
        // Supporters formats: "11:00 AM", "11:00", "23:00"
        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.US),
                DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.US),
                DateTimeFormatter.ofPattern("HH:mm", java.util.Locale.US),
                DateTimeFormatter.ofPattern("H:mm", java.util.Locale.US)
        };

        try {
            LocalTime open = parseTime(openingTime, formatters);
            LocalTime close = parseTime(closingTime, formatters);

            if (open == null || close == null)
                return slots;

            LocalDateTime now = LocalDateTime.now();

            // Generate slots for Today (0) and Tomorrow (1)
            for (int day = 0; day < 2; day++) {
                // Base date for this iteration (Today or Tomorrow at 00:00)
                java.time.LocalDate baseDate = java.time.LocalDate.now().plusDays(day);

                LocalDateTime slotTime = LocalDateTime.of(baseDate, open);
                LocalDateTime closeDateTime = LocalDateTime.of(baseDate, close);

                // Handle crossing midnight (e.g. 11 PM to 2 AM)
                if (close.isBefore(open)) {
                    closeDateTime = closeDateTime.plusDays(1);
                }

                // If the closing time is strictly after the opening time, loop
                while (slotTime.isBefore(closeDateTime)) {
                    // Check if slot is at least 30 mins in the future from NOW
                    if (slotTime.isAfter(now.plusMinutes(30))) {
                        slots.add(slotTime.toString());
                    }
                    slotTime = slotTime.plusMinutes(step);
                }
            }
        } catch (Exception e) {
            System.err.println("Error generating slots: " + e.getMessage());
            e.printStackTrace();
        }

        return slots;
    }

    private LocalTime parseTime(String timeStr, DateTimeFormatter[] formatters) {
        if (timeStr == null || timeStr.trim().isEmpty())
            return null;
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalTime.parse(timeStr.trim(), formatter);
            } catch (Exception ignored) {
                // Try next format
            }
        }
        System.err.println("Failed to parse time: " + timeStr);
        return null;
    }
}
