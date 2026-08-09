package com.app.fooddelivery.config;

import com.app.fooddelivery.model.FoodItem;
import com.app.fooddelivery.model.Modifier;
import com.app.fooddelivery.model.ModifierGroup;
import com.app.fooddelivery.model.Restaurant;
import com.app.fooddelivery.model.User;
import com.app.fooddelivery.repository.FoodItemRepository;
import com.app.fooddelivery.repository.ModifierGroupRepository;
import com.app.fooddelivery.repository.ModifierRepository;
import com.app.fooddelivery.repository.RestaurantRepository;
import com.app.fooddelivery.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Seeds demo data so a fresh clone has something to show.
 *
 * Runs only when the restaurant table is empty. On any database that already
 * holds data this does nothing, so existing rows are never touched.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final RestaurantRepository restaurantRepository;
    private final FoodItemRepository foodItemRepository;
    private final ModifierGroupRepository modifierGroupRepository;
    private final ModifierRepository modifierRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(RestaurantRepository restaurantRepository,
            FoodItemRepository foodItemRepository,
            ModifierGroupRepository modifierGroupRepository,
            ModifierRepository modifierRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.restaurantRepository = restaurantRepository;
        this.foodItemRepository = foodItemRepository;
        this.modifierGroupRepository = modifierGroupRepository;
        this.modifierRepository = modifierRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (restaurantRepository.count() > 0) {
            log.info("Existing data found, skipping demo seed entirely.");
            return;
        }

        log.info("Empty database detected, seeding demo accounts, restaurants and menu.");
        seedUsers();

        Restaurant pizza = restaurant("Pizza Palace", "Best pizza in town",
                "42 Pizza Lane, Hitech City, Hyderabad", 17.4485, 78.3908, 10.0,
                "9000000001", "contact@pizzapalace.test", "10:00 AM", "10:00 PM");

        Restaurant burger = restaurant("Burger Barn", "Flame grilled burgers",
                "10 Burger Ave, Gachibowli, Hyderabad", 17.4400, 78.3489, 8.0,
                "9000000002", "contact@burgerbarn.test", "11:00 AM", "11:00 PM");

        FoodItem margherita = unlimitedItem(pizza, "Margherita Pizza",
                "Cheese and tomato on a hand-tossed base", 12.99, true);
        unlimitedItem(pizza, "Pepperoni Pizza", "Loaded with pepperoni", 14.99, false);
        dailyItem(pizza, "Garlic Bread", "Fresh from the oven, limited batch each day", 5.99, 20, "09:00");

        FoodItem classicBurger = unlimitedItem(burger, "Classic Burger",
                "Grilled patty, lettuce, cheese", 8.99, true);
        unlimitedItem(burger, "Loaded Fries", "Cheese and jalapenos", 4.99, false);

        // Required single-choice group, so checkout exercises the validation path.
        ModifierGroup crust = group(margherita, "Choose Crust", 1, 1, true, 1);
        modifier(crust, "Thin Crust", 0.00, 1);
        modifier(crust, "Thick Crust", 2.00, 2);
        modifier(crust, "Cheese Stuffed Crust", 3.50, 3);

        ModifierGroup toppings = group(margherita, "Extra Toppings", 0, 5, false, 2);
        modifier(toppings, "Extra Cheese", 1.50, 1);
        modifier(toppings, "Mushrooms", 1.00, 2);
        modifier(toppings, "Olives", 1.00, 3);

        ModifierGroup meal = group(classicBurger, "Make it a Meal", 0, 1, false, 1);
        modifier(meal, "Add Fries & Drink", 4.00, 1);
        modifier(meal, "Add Onion Rings & Drink", 5.00, 2);

        ModifierGroup extras = group(classicBurger, "Extras", 0, 3, false, 2);
        modifier(extras, "Bacon", 1.50, 1);
        modifier(extras, "Extra Patty", 2.50, 2);

        log.info("Seeded {} restaurants and {} menu items.",
                restaurantRepository.count(), foodItemRepository.count());
    }

    /** Demo accounts, created only if that email is not already registered. */
    private void seedUsers() {
        createUserIfMissing("John Doe", "john@example.com", "password", "USER", "123 Main St, Hyderabad");
        createUserIfMissing("Admin", "admin@example.com", "admin123", "ADMIN", "Headquarters");
    }

    private void createUserIfMissing(String name, String email, String rawPassword, String role, String address) {
        if (userRepository.findByEmail(email).isPresent()) {
            return;
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setAddress(address);
        userRepository.save(user);
        log.info("Created demo account {} (password: {})", email, rawPassword);
    }

    private Restaurant restaurant(String name, String description, String address,
            double lat, double lon, double radiusKm,
            String phone, String email, String open, String close) {
        Restaurant r = new Restaurant();
        r.setName(name);
        r.setDescription(description);
        r.setAddress(address);
        r.setImageUrl("https://placehold.co/300x200?text=" + name.replace(" ", "+"));
        r.setLatitude(lat);
        r.setLongitude(lon);
        r.setDeliveryRadiusKm(radiusKm);
        r.setPhone(phone);
        r.setEmail(email);
        r.setOpeningTime(open);
        r.setClosingTime(close);
        r.setIsOpen(true);
        r.setAcceptsScheduledOrders(true);
        r.setSlotDurationMinutes(30);
        r.setOperatingHours(new ArrayList<>());
        return restaurantRepository.save(r);
    }

    private FoodItem unlimitedItem(Restaurant r, String name, String description, double price, boolean bestSeller) {
        FoodItem f = baseItem(r, name, description, price);
        f.setStockResetType("UNLIMITED");
        f.setStockQuantity(100);
        f.setIsBestSeller(bestSeller);
        return foodItemRepository.save(f);
    }

    private FoodItem dailyItem(Restaurant r, String name, String description, double price,
            int dailyLimit, String restockTime) {
        FoodItem f = baseItem(r, name, description, price);
        f.setStockResetType("DAILY");
        f.setStockQuantity(dailyLimit);
        f.setDailyStockLimit(dailyLimit);
        f.setDailyRestockTime(restockTime);
        return foodItemRepository.save(f);
    }

    private FoodItem baseItem(Restaurant r, String name, String description, double price) {
        FoodItem f = new FoodItem();
        f.setName(name);
        f.setDescription(description);
        f.setPrice(price);
        f.setImageUrl("https://placehold.co/300x200?text=" + name.replace(" ", "+"));
        f.setInStock(true);
        f.setRestaurant(r);
        return f;
    }

    private ModifierGroup group(FoodItem item, String name, int min, int max, boolean required, int order) {
        ModifierGroup g = new ModifierGroup();
        g.setName(name);
        g.setMinSelection(min);
        g.setMaxSelection(max);
        g.setRequired(required);
        g.setDisplayOrder(order);
        g.setFoodItem(item);
        g.setModifiers(new ArrayList<>());
        return modifierGroupRepository.save(g);
    }

    private Modifier modifier(ModifierGroup group, String name, double priceAdjustment, int order) {
        Modifier m = new Modifier();
        m.setName(name);
        m.setPriceAdjustment(priceAdjustment);
        m.setAvailable(true);
        m.setDisplayOrder(order);
        m.setModifierGroup(group);
        return modifierRepository.save(m);
    }
}
