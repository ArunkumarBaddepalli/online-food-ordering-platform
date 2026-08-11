package com.app.fooddelivery.service;

import com.app.fooddelivery.model.FoodItem;
import com.app.fooddelivery.repository.FoodItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
public class StockResetScheduler {

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private RestaurantHoursValidator hoursValidator;

    @Scheduled(cron = "0 * * * * *") // runs every minute
    @Transactional
    public void resetDailyStock() {
        List<FoodItem> dailyItems = foodItemRepository.findByStockResetType("DAILY");

        for (FoodItem item : dailyItems) {
            // "Restocked at 9am" means nine in the morning where the kitchen
            // is. Read on the server's clock it would fire at the wrong time
            // for anywhere else.
            java.time.ZonedDateTime local = hoursValidator.nowAt(item.getRestaurant());
            LocalDate today = local.toLocalDate();
            LocalTime now = local.toLocalTime();

            if (item.getDailyRestockTime() == null || item.getDailyStockLimit() == null) {
                continue;
            }

            try {
                LocalTime restockTime = LocalTime.parse(item.getDailyRestockTime());

                // Reset if restock time passed today AND not yet reset today
                boolean notResetToday = !today.equals(item.getLastResetDate());
                boolean restockTimePassed = !now.isBefore(restockTime);

                if (notResetToday && restockTimePassed) {
                    item.setStockQuantity(item.getDailyStockLimit());
                    item.setInStock(true);
                    item.setNextAvailableAt(null);
                    item.setOosReason(null);
                    item.setLastResetDate(today);
                    foodItemRepository.save(item);
                }
            } catch (Exception e) {
                System.err.println("StockResetScheduler error for item " + item.getId() + ": " + e.getMessage());
            }
        }
    }
}
