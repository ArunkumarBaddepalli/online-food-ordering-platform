package com.app.fooddelivery.model;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
public class FoodItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private Double price;
    private String imageUrl;

    // Stock Management
    private Boolean inStock = true;
    private Integer stockQuantity = 100;

    // Stock Reset Config
    private String stockResetType = "UNLIMITED"; // UNLIMITED | DAILY | MANUAL
    private Integer dailyStockLimit;             // max per day for DAILY type
    private String dailyRestockTime;             // "HH:mm" when kitchen restocks, for DAILY type
    private LocalDateTime nextAvailableAt;       // computed when item goes OOS
    private String oosReason;                    // "Sold out today" / "Seasonal" / etc.
    private LocalDate lastResetDate;             // tracks if daily reset done today

    private Boolean isBestSeller = false;

    @ManyToOne
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    @OneToMany(mappedBy = "foodItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ModifierGroup> modifierGroups;
}
