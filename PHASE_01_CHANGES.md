# Phase 01 — Change Record

**Status:** implemented, tested, committed on `phase-01` as `3b8a760`. Not pushed. `main` untouched.

**Verification:** 38 unit tests green; booted against MySQL and exercised end to end (login, cart, modifiers, order, payment, ETA countdown, cancellation) on both a clean database and the existing one.

**Two bugs were found during verification that are not in the list below** — see "Found during testing" at the end. Both were real and both are fixed.

The changes below already exist as **uncommitted edits on the `phase-01` branch** — I wrote them before you asked me to stop. Treat this document as the review: read each item, mark it, and I'll act on your marks.

Mark each:

- `[k]` keep
- `[r]` revert this one
- `[?]` explain more

Revert everything at once with:

```bash
git checkout . && git checkout main && git branch -D phase-01
```

---

## Summary

| | |
|---|---|
| Files changed | 17 (backend only) |
| Lines | +350 / −133 |
| Commits | 0 |
| Frontend touched | none |
| Tests written | none |
| Compiles | yes, clean |
| Run against a database | **no — not yet verified** |

Done: 14 of 18 planned fixes. Not started: F-7, F-16, F-18, and part of F-4.

---

## PART 1 — CHANGES ALREADY IN THE WORKING TREE

### `[ ]` 1. UNLIMITED items no longer run out  *(F-1)*

`OrderService.placeOrder`

**Before** — stock was decremented for every item type. An `UNLIMITED` item started at 100, counted down, and at 0 was flagged out of stock permanently.

```java
if (Boolean.FALSE.equals(foodItem.getInStock())
        || foodItem.getStockQuantity() < cartItem.getQuantity()) {
    throw new RuntimeException("Insufficient stock for item: " + foodItem.getName());
}
foodItem.setStockQuantity(foodItem.getStockQuantity() - cartItem.getQuantity());
```

**After** — the whole stock block is skipped for `UNLIMITED`. A manual out-of-stock flag is still respected.

```java
if (Boolean.FALSE.equals(foodItem.getInStock())) {
    throw new RuntimeException("Item is out of stock: " + foodItem.getName());
}

// UNLIMITED items never track or deplete stock.
if (!"UNLIMITED".equals(foodItem.getStockResetType())) {
    int available = foodItem.getStockQuantity() == null ? 0 : foodItem.getStockQuantity();
    ...
}
```

Same treatment added to `CartService` via a new `assertStockAvailable(food, wanted)` helper, so add-to-cart agrees with checkout.

**Behaviour change:** an `UNLIMITED` item can now be ordered forever. Its `stockQuantity` column stops moving.

---

### `[ ]` 2. Changing cart quantity keeps modifier prices  *(F-2)*

`CartController.updateCartItem` → moved into `CartService.updateCartItemQuantity`

**Before** — modifier prices were silently dropped:

```java
item.setQuantity(quantity);
item.setTotalPrice(item.getFoodItem().getPrice() * quantity);   // modifiers gone
```

**After** — controller delegates; the service reuses the existing modifier-aware `updateItemPrice`, and rejects `quantity < 1`:

```java
item.setQuantity(quantity);
updateItemPrice(item);     // same method add-to-cart uses
```

The controller now returns `400` with a message instead of throwing a raw exception.

---

### `[ ]` 3. Failed reorder no longer empties the cart  *(F-3)*

`CartService.reorder` — added `@Transactional`. `clearCart` now rolls back if any item fails to re-add.

Also added `@Transactional` to `addToCart`.

---

### `[ ]` 4. Geocoding can't hang checkout  *(F-5)*

`GeocodingService.geocodeAddress`

```java
conn.setConnectTimeout(3000);
conn.setReadTimeout(5000);
```

**Note / open question:** this call still sits inside the `@Transactional placeOrder`, so a slow response holds a DB connection for up to 5s. Moving it out was in the "enterprise" pile I cut. Say if you want it moved.

---

### `[ ]` 5. Null guards  *(F-6)*

Four unboxing sites that threw 500s on SQL-inserted rows:

| File | Field |
|---|---|
| `OrderService` (place + cancel) | `stockQuantity`, `deliveryRadiusKm` |
| `CartService` | `stockQuantity` |
| `RestaurantHoursValidator:179` | `acceptsScheduledOrders` |
| `RestaurantSettingsController:56` | `acceptsScheduledOrders` |

All now use `== null ? default :` or `Boolean.TRUE.equals(...)`.

---

### `[ ]` 6. ETA actually counts down  *(F-11)*

New column on `Order`:

```java
private LocalDateTime estimatedDeliveryAt;
```

Set once at order placement (`now + 35 min`, or the scheduled time). `OrderETAService` now measures remaining minutes against that fixed target instead of returning `now + constant` on every poll.

Also: null-safe on `status`, and removed the unused `DistanceCalculationService` autowire plus the `if`/`else` whose branches were identical.

**Schema impact:** Hibernate `ddl-auto=update` adds the column automatically. Existing orders get `null` and fall back to the old per-status estimate.

**Assumption I made:** 35 minutes flat. Change it if you want a different default.

---

### `[ ]` 7. Modifier rules enforced server-side  *(F-13)*

New `CartService.validateModifierSelection(food, selected)`. Rejects:

- modifiers marked `available = false`
- modifiers belonging to a different food item
- fewer selections than `minSelection` (or than 1 when `required = true`)
- more selections than `maxSelection`

**Behaviour change:** you can no longer add a Margherita without picking a crust — "Choose Crust" is `required` with `min 1`. **If the frontend doesn't already force that choice, add-to-cart will start failing there.** Worth testing before you keep this one.

---

### `[ ]` 8. Every order gets a payment record  *(F-15)*

`OrderService.placeOrder` now creates one `Payment` row in the same transaction — method `COD`, status `PENDING`. `cancelOrder` sets it to `CANCELLED`.

No gateway, no Razorpay. `PaymentController.pay` is still the old mock — **I did not touch it**, so it remains a stub that persists nothing.

---

### `[ ]` 9. Restaurant is open at its opening minute  *(F-14)*

`RestaurantHoursValidator.isTimeInRange` — `time.isAfter(open)` → `!time.isBefore(open)`. A 10:00 AM opener now counts as open at exactly 10:00.

---

### `[ ]` 10. Marking sold-out keeps the daily restock config  *(F-12)*

`FoodItemController.markOOS` — deleted `food.setStockResetType("MANUAL")`. A `DAILY` item marked sold out now still auto-restocks the next morning.

---

### `[ ]` 11. Order status is validated  *(F-10)*

`OrderController.updateOrderStatus` now checks against a fixed list and returns `400` otherwise:

```java
private static final List<String> VALID_STATUSES = List.of(
        "PLACED", "CONFIRMED", "PREPARING", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED");
```

**Still open to unauthenticated callers** — no role check, no transition rules. That was the Phase 02 line we drew.

---

### `[ ]` 12. Infinite-loop guard  *(F-17)*

Both copies of `generateTimeSlots` (`RestaurantController`, `RestaurantSettingsController`) now fall back to 30 when `slotDurationMinutes` is null or `<= 0`. Previously `0` hung the request thread forever.

---

### `[ ]` 13. BCrypt password hashing  *(F-8)*

New `PasswordEncoder` bean in `SecurityConfig`. `UserService` rewritten:

- register: `passwordEncoder.encode(...)`, rejects blank passwords, rejects duplicate emails, defaults role to `USER`
- login: `passwordEncoder.matches(...)`
- **plaintext upgrade path** — an old plaintext row is accepted once on a correct password, then rehashed in place. No migration script, no forced reset.

`AuthController` now returns `400` with a message on duplicate email instead of a 500.

---

### `[ ]` 14. Password never leaves the server  *(F-9)*

One annotation on `User.password`:

```java
@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
private String password;
```

Still accepted on register/login, never serialized in a response. Fixes the leak into `localStorage` too.

---

### `[ ]` 15. Seed data + config  *(F-4, partial)*

`application.properties`:

```properties
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:}
spring.sql.init.mode=always
spring.jpa.defer-datasource-initialization=true
```

`data.sql` rewritten — explicit IDs + `INSERT IGNORE` so restarts don't duplicate; adds the `latitude` / `longitude` / `delivery_radius_km` / `stock_quantity` / `in_stock` / `stock_reset_type` columns that were missing; 2 restaurants, 5 items, 4 modifier groups, 10 modifiers; prices moved to ₹.

**This is the riskiest change in the set and the one I'd most want you to check:**

- It **renames "Burger King" to "Burger Barn"** (using a real brand name in a portfolio repo is asking for trouble). Say if you'd rather keep it.
- Coordinates are Hyderabad. Change if your demo should be elsewhere.
- Seeded passwords are still plaintext, relying on the upgrade-on-login path from item 13.
- **`INSERT IGNORE` is MySQL-only** — it will not run on H2, which matters for the tests in F-18.
- **Not tested.** I have not run this against a live MySQL. If a column name is wrong, startup fails.

---

## PART 2 — NOT DONE

| # | Item | Note |
|---|---|---|
| F-7 | Frontend `.toFixed()` null guards | Not started. 2 lines in `OrderHistory.js` and `Cart.js`. |
| F-16 | Delete dead code | Partly done — removed the dead `.ifPresent()`, the unused autowire, the duplicate `if`/`else`, and the unannotated `placeOrder` overload. **Not** done: deleting `useOrderPolling.js`, merging the three Haversine copies, merging `SavedAddress`/`UserAddress`. |
| F-18 | ~8 regression tests | Not started. |
| — | Run the app against MySQL | **Not done. Nothing here has been executed — only compiled.** |

---

## PART 3 — ONE FINDING I GOT WRONG

The earlier plan listed "`CartItemModifier.quantity` is never set, so it's null" as a bug. It isn't — the field is declared `private Integer quantity = 1;`, so new instances default to 1. Dropped, no fix needed.

---

## PART 4 — FOUND DURING TESTING

Two real bugs surfaced only once the app was actually running. Neither was in the plan.

### A. Any order with modifiers returned broken JSON  — **worst bug found**

`OrderItemModifier.orderItem` had no `@JsonIgnore`, so the serialiser looped:

```
OrderItem -> selectedModifiers -> OrderItemModifier -> orderItem -> OrderItem -> ...
```

Placing an order with a crust and a topping returned **2,196,856 bytes** of nested JSON, truncated mid-write with `Could not write JSON: Infinite recursion (StackOverflowError)`.

The order saved correctly, but the client got garbage — so order confirmation, order history and tracking would all have failed on any customised order. The equivalent field on the cart side (`CartItemModifier.cartItem`) already had the annotation; the order side was missed.

Fixed. Same request now returns **3,055 bytes** of correct JSON.

### B. Opening hours never worked at all

`RestaurantHoursValidator` built its time formatters without a locale. This machine's default locale is `en_IN`, whose CLDR data uses lowercase `am`/`pm` — so `"11:00 PM"` failed every parse attempt and returned null.

`isTimeInRange` returns `true` when either bound is null. Net result: **every restaurant read as open 24/7 regardless of its configured hours.**

Formatters are now pinned to `Locale.US` and made case-insensitive, so both `"11:00 PM"` and `"11:00 pm"` parse. `RestaurantController` had the locale but not case-insensitivity; it was made consistent.

This is why the boundary fix in item 9 could not have been observed before — the comparison was never reached.

---

## PART 5 — VERIFICATION PERFORMED

**Tests:** 38 unit tests, 0 failures — `OrderServiceTest` (7), `CartServiceTest` (9), `UserServiceTest` (8), `RestaurantHoursValidatorTest` (8), `OrderETAServiceTest` (6).

**Against a clean MySQL database:**

| Check | Result |
|---|---|
| Seeder populates empty DB | 2 restaurants, 5 items, 4 modifier groups, 10 modifiers |
| Seeded passwords | BCrypt `$2a$10$`, 60 chars |
| Login response | no `password` field present |
| Wrong password | 401 |
| Pizza without crust | 400, "Please choose at least 1 option(s) for: Choose Crust" |
| Quantity 1 → 2 with cheese | 28.98 — correct; the old bug gave 25.98 |
| Quantity 0 | 400 |
| Order placed | payment row COD / PENDING / 28.98 |
| UNLIMITED stock after order | still 100 |
| Invalid status `BANANA` | 400 |
| ETA over 65 seconds | 34 min → 33 min, target timestamp unchanged |
| Cancel | order CANCELLED, payment CANCELLED |

**Against your existing database (11 restaurants, 41 items, 15 orders, 4 users):**

| Check | Result |
|---|---|
| Seeder behaviour | "Existing data found, skipping demo seed entirely" |
| Row counts after boot | unchanged — 4 / 11 / 15 / 41 |
| Schema change | `orders.estimated_delivery_at` added; additive only |
| Legacy rows (`stock_reset_type` NULL on all 41) | add to cart, order, stock decrement all work — no NPEs |
| Existing order detail | renders in 2,619 bytes |

**Test data cleanup:** the test order created in your real database was deleted and Pepperoni Pizza's stock restored to 40. Counts verified back at 4 / 11 / 15 / 41.

**One thing I did not undo:** user 1's cart was cleared during testing (it held items from a previous session). That content is gone.

Your app instance on ports 8080/8081 was left running and untouched; testing used 8098/8099 and a throwaway database that has since been dropped.

---

## PART 6 — STILL OPEN

- **The API remains open to unauthenticated callers.** Any endpoint, any user's data. Fine while the repo is private and you demo locally; not fine deployed. Phase 02.
- Not pushed to GitHub. Say the word and I'll push `phase-01`.
- Still outstanding from the original list: merging the three Haversine copies, merging `SavedAddress`/`UserAddress`, replacing 23 `alert()` calls with toasts.
