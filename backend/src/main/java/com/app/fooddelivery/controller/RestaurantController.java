package com.app.fooddelivery.controller;

import com.app.fooddelivery.model.FoodItem;
import com.app.fooddelivery.model.Restaurant;
import com.app.fooddelivery.model.RestaurantOperatingHours;
import com.app.fooddelivery.repository.RestaurantRepository;
import com.app.fooddelivery.service.RestaurantHoursValidator;
import com.app.fooddelivery.service.RestaurantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RestaurantController {

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private RestaurantHoursValidator hoursValidator;

    @GetMapping("/restaurants")
    public ResponseEntity<List<Restaurant>> getAllRestaurants() {
        return ResponseEntity.ok(restaurantService.getAllRestaurants());
    }

    @PostMapping("/restaurants")
    public ResponseEntity<Restaurant> createRestaurant(@RequestBody Restaurant restaurant) {
        return ResponseEntity.ok(restaurantService.createRestaurant(restaurant));
    }

    @GetMapping("/restaurants/{id}")
    public ResponseEntity<Restaurant> getRestaurantById(@PathVariable Long id) {
        return restaurantService.getAllRestaurants().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Consolidated live-status endpoint — replaces 3 separate frontend calls.
     * Returns: isOpen, isCurrentlyOpen, canAcceptOrders, todayHours, nextOpenTime,
     *          acceptsScheduledOrders, slotDurationMinutes, availableTimeSlots.
     */
    @GetMapping("/restaurants/{id}/live-status")
    public ResponseEntity<Map<String, Object>> getLiveStatus(@PathVariable Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        boolean isCurrentlyOpen = hoursValidator.isRestaurantOpen(restaurant);
        boolean canAcceptOrders = Boolean.TRUE.equals(restaurant.getIsOpen()) && isCurrentlyOpen;

        RestaurantOperatingHours todayHours = hoursValidator.getTodayHours(restaurant);
        LocalDateTime nextOpenTime = hoursValidator.getNextOpenTime(restaurant);

        List<String> slots = new ArrayList<>();
        if (Boolean.TRUE.equals(restaurant.getAcceptsScheduledOrders())) {
            slots = generateTimeSlots(restaurant, todayHours);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("isOpen", restaurant.getIsOpen());
        response.put("isCurrentlyOpen", isCurrentlyOpen);
        response.put("canAcceptOrders", canAcceptOrders);
        response.put("acceptsScheduledOrders", restaurant.getAcceptsScheduledOrders());
        response.put("slotDurationMinutes", restaurant.getSlotDurationMinutes());
        response.put("nextOpenTime", nextOpenTime);
        response.put("availableTimeSlots", slots);

        if (todayHours != null) {
            Map<String, Object> todayMap = new HashMap<>();
            todayMap.put("dayOfWeek", todayHours.getDayOfWeek());
            todayMap.put("isOpen", todayHours.getIsOpen());
            todayMap.put("openTime", todayHours.getOpenTime());
            todayMap.put("closeTime", todayHours.getCloseTime());
            response.put("todayHours", todayMap);
        } else {
            Map<String, Object> legacyHours = new HashMap<>();
            legacyHours.put("dayOfWeek", LocalDate.now().getDayOfWeek().name());
            legacyHours.put("isOpen", restaurant.getIsOpen());
            legacyHours.put("openTime", restaurant.getOpeningTime());
            legacyHours.put("closeTime", restaurant.getClosingTime());
            response.put("todayHours", legacyHours);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Real-time stock status for all menu items of a restaurant.
     */
    @GetMapping("/restaurants/{id}/menu-status")
    public ResponseEntity<List<Map<String, Object>>> getMenuStatus(@PathVariable Long id) {
        List<FoodItem> items = restaurantService.getFoodItemsByRestaurant(id);
        List<Map<String, Object>> result = new ArrayList<>();

        for (FoodItem item : items) {
            Map<String, Object> m = new HashMap<>();
            m.put("foodItemId", item.getId());
            m.put("name", item.getName());
            m.put("inStock", item.getInStock());
            m.put("stockQuantity", item.getStockQuantity());
            m.put("stockResetType", item.getStockResetType());
            m.put("nextAvailableAt", item.getNextAvailableAt());
            m.put("oosReason", item.getOosReason());
            m.put("lowStock", item.getInStock() != null && item.getInStock()
                    && item.getStockQuantity() != null && item.getStockQuantity() > 0
                    && item.getStockQuantity() <= 5);
            result.add(m);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/foods/{restaurantId}")
    public ResponseEntity<List<FoodItem>> getFoodItemsAndRestaurant(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(restaurantService.getFoodItemsByRestaurant(restaurantId));
    }

    @PostMapping("/restaurants/{id}/validate-delivery")
    public ResponseEntity<Map<String, Object>> validateDelivery(@PathVariable Long id,
            @RequestBody com.app.fooddelivery.dto.DeliveryValidationRequest request) {
        Restaurant restaurant = restaurantService.getAllRestaurants().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        Map<String, Object> response = new HashMap<>();

        if (restaurant.getLatitude() == null || restaurant.getLongitude() == null) {
            response.put("possible", true);
            response.put("message", "Restaurant location not set, skipping validation.");
            return ResponseEntity.ok(response);
        }

        double userLat;
        double userLon;

        if (request.getLatitude() != null && request.getLongitude() != null) {
            userLat = request.getLatitude();
            userLon = request.getLongitude();
        } else if (request.getAddress() != null && !request.getAddress().isEmpty()) {
            com.app.fooddelivery.service.GeocodingService geocodingService = new com.app.fooddelivery.service.GeocodingService();
            com.app.fooddelivery.service.GeocodingService.GeocodingResult result = geocodingService
                    .geocodeAddress(request.getAddress());

            if (result == null) {
                response.put("possible", false);
                response.put("message", "Could not find location for address.");
                return ResponseEntity.ok(response);
            }
            userLat = result.getLatitude();
            userLon = result.getLongitude();
        } else {
            response.put("possible", false);
            response.put("message", "Address or coordinates required.");
            return ResponseEntity.ok(response);
        }

        double distance = com.app.fooddelivery.service.GeocodingService.calculateDistance(
                restaurant.getLatitude(), restaurant.getLongitude(), userLat, userLon);

        boolean possible = distance <= restaurant.getDeliveryRadiusKm();
        response.put("possible", possible);
        response.put("distanceKm", distance);
        response.put("maxRadiusKm", restaurant.getDeliveryRadiusKm());
        response.put("message", possible ? "Delivery available." : "Address is out of delivery range.");

        return ResponseEntity.ok(response);
    }

    private List<String> generateTimeSlots(Restaurant restaurant, RestaurantOperatingHours todayHours) {
        List<String> slots = new ArrayList<>();
        String openTime = todayHours != null ? todayHours.getOpenTime() : restaurant.getOpeningTime();
        String closeTime = todayHours != null ? todayHours.getCloseTime() : restaurant.getClosingTime();
        // A zero or negative step would never advance the loop below and would
        // hang the request thread.
        Integer configured = restaurant.getSlotDurationMinutes();
        int slotDuration = (configured == null || configured <= 0) ? 30 : configured;

        if (openTime == null || closeTime == null) return slots;

        DateTimeFormatter[] formatters = {
                caseInsensitive("h:mm a"),
                caseInsensitive("hh:mm a"),
                caseInsensitive("HH:mm"),
                caseInsensitive("H:mm")
        };

        try {
            LocalTime open = parseTime(openTime, formatters);
            LocalTime close = parseTime(closeTime, formatters);
            if (open == null || close == null) return slots;

            LocalDateTime now = LocalDateTime.now();
            for (int day = 0; day < 2; day++) {
                LocalDate baseDate = LocalDate.now().plusDays(day);
                LocalDateTime slotTime = LocalDateTime.of(baseDate, open);
                LocalDateTime closeDateTime = LocalDateTime.of(baseDate, close);
                if (close.isBefore(open)) closeDateTime = closeDateTime.plusDays(1);

                while (slotTime.isBefore(closeDateTime)) {
                    if (slotTime.isAfter(now.plusMinutes(30))) {
                        slots.add(slotTime.toString());
                    }
                    slotTime = slotTime.plusMinutes(slotDuration);
                }
            }
        } catch (Exception e) {
            System.err.println("Error generating slots: " + e.getMessage());
        }

        return slots;
    }

    /** Locale-pinned and case-insensitive so "11:00 PM" and "11:00 pm" both parse. */
    private static DateTimeFormatter caseInsensitive(String pattern) {
        return new java.time.format.DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(pattern)
                .toFormatter(java.util.Locale.US);
    }

    private LocalTime parseTime(String timeStr, DateTimeFormatter[] formatters) {
        if (timeStr == null || timeStr.trim().isEmpty()) return null;
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalTime.parse(timeStr.trim(), formatter);
            } catch (Exception ignored) {}
        }
        return null;
    }
}
