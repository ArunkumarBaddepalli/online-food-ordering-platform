# Phase 01 — Change Plan for Review

**Status:** awaiting your review. Nothing committed, nothing pushed, `main` untouched.

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

## PART 4 — WHAT I NEED FROM YOU

1. **Mark items 1–15** `[k]` / `[r]` / `[?]`.
2. **Item 7 (modifier rules)** — does the menu UI already force a required-group choice? If not, this breaks add-to-cart for Margherita.
3. **Item 15 (seed data)** — keep "Burger King" or use "Burger Barn"? Hyderabad coordinates OK?
4. **Item 6** — is 35 minutes the right default ETA?
5. Once marked, do you want the remaining four items (F-7, F-16, F-18, live DB run) done in this phase or dropped?

I won't touch code again until you come back on these.
