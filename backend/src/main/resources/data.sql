-- Users
INSERT INTO users (name, email, password, role, address) VALUES ('John Doe', 'john@example.com', 'password', 'USER', '123 Main St');
INSERT INTO users (name, email, password, role, address) VALUES ('Admin', 'admin@example.com', 'admin123', 'ADMIN', 'Headquarters');

-- Restaurants
INSERT INTO restaurant (name, description, address, image_url, opening_time, closing_time, is_open, accepts_scheduled_orders) VALUES ('Pizza Palace', 'Best Pizza in Town', '42 Pizza Lane', 'https://via.placeholder.com/150', '10:00 AM', '10:00 PM', true, true);
INSERT INTO restaurant (name, description, address, image_url, opening_time, closing_time, is_open, accepts_scheduled_orders) VALUES ('Burger King', 'Flame Grilled Burgers', '10 Burger Ave', 'https://via.placeholder.com/150', '11:00 AM', '11:00 PM', true, true);

-- Food Items
-- Assuming Pizza Palace has ID 1 and Burger King has ID 2
INSERT INTO food_item (name, description, price, image_url, restaurant_id) VALUES ('Margherita Pizza', 'Cheese and Tomato', 12.99, 'https://via.placeholder.com/150', 1);
INSERT INTO food_item (name, description, price, image_url, restaurant_id) VALUES ('Pepperoni Pizza', 'Lots of Pepperoni', 14.99, 'https://via.placeholder.com/150', 1);
INSERT INTO food_item (name, description, price, image_url, restaurant_id) VALUES ('Whopper', 'Big Burger', 8.99, 'https://via.placeholder.com/150', 2);
-- Modifiers for Margherita Pizza (ID 1)
INSERT INTO modifier_group (name, min_selection, max_selection, required, food_item_id) VALUES ('Choose Crust', 1, 1, true, 1);
-- Assuming 'Choose Crust' got ID 1
INSERT INTO modifier (name, price_adjustment, available, modifier_group_id) VALUES ('Thin Crust', 0.0, true, 1);
INSERT INTO modifier (name, price_adjustment, available, modifier_group_id) VALUES ('Thick Crust', 2.0, true, 1);
INSERT INTO modifier (name, price_adjustment, available, modifier_group_id) VALUES ('Cheese Stuffed Crust', 3.5, true, 1);

INSERT INTO modifier_group (name, min_selection, max_selection, required, food_item_id) VALUES ('Extra Toppings', 0, 5, false, 1);
-- Assuming 'Extra Toppings' got ID 2
INSERT INTO modifier (name, price_adjustment, available, modifier_group_id) VALUES ('Extra Cheese', 1.5, true, 2);
INSERT INTO modifier (name, price_adjustment, available, modifier_group_id) VALUES ('Mushrooms', 1.0, true, 2);
INSERT INTO modifier (name, price_adjustment, available, modifier_group_id) VALUES ('Olives', 1.0, true, 2);

-- Modifiers for Whopper (ID 3)
INSERT INTO modifier_group (name, min_selection, max_selection, required, food_item_id) VALUES ('Make it a Meal', 0, 1, false, 3);
-- Assuming 'Make it a Meal' got ID 3
INSERT INTO modifier (name, price_adjustment, available, modifier_group_id) VALUES ('Add Fries & Drink', 4.0, true, 3);
INSERT INTO modifier (name, price_adjustment, available, modifier_group_id) VALUES ('Add Onion Rings & Drink', 5.0, true, 3);

INSERT INTO modifier_group (name, min_selection, max_selection, required, food_item_id) VALUES ('Extras', 0, 3, false, 3);
-- Assuming 'Extras' got ID 4
INSERT INTO modifier (name, price_adjustment, available, modifier_group_id) VALUES ('Bacon', 1.5, true, 4);
INSERT INTO modifier (name, price_adjustment, available, modifier_group_id) VALUES ('Extra Patty', 2.5, true, 4);
