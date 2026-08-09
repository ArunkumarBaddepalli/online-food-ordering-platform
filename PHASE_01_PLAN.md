# Phase 01 — Make the Basics Work

**Status:** AWAITING APPROVAL — nothing implemented yet
**Goal:** Every core flow works correctly end to end. No architecture rewrites.
**Scope rule:** if a fix needs a new layer, a schema migration, or more than ~30 lines, it is not Phase 01.

---

## How to review

Mark each item:

- `[x]` — do it
- `[-]` — skip

Then say "go".

---

## What this phase does NOT do

Deliberately excluded as over-engineering for this project:

- No DTO/mapper layer across every controller
- No `OrderStatus` / `Role` enum migration (string columns stay)
- No RBAC filter chain or per-resource ownership assertions
- No repository/schema changes (no `Restaurant.owner` FK)
- No large test suite — a handful of focused tests only
- No Razorpay, WebSocket, email, admin UI, ratings (Phase 02 features)

---

## PART A — THE FIXES

15 items. All small, all confirmed by reading the code. Grouped by what breaks.

### A1. Bugs that break the demo  — do these first

#### `[ ]` F-1. `UNLIMITED` items die after 100 orders  **worst bug in the app**

`placeOrder` decrements stock for *every* type, including `UNLIMITED`. Entity default is 100. When it reaches 0 the `else if` flips `inStock=false`, and the scheduler only restocks `DAILY` items — so the item is gone permanently and cannot be re-added to any cart.

Fix: skip the decrement and the OOS flip when `stockResetType.equals("UNLIMITED")`.
`service/OrderService.java:121,125,133` — ~6 lines.

---

#### `[ ]` F-2. Changing cart quantity wipes modifier prices

`updateCartItem` recomputes `totalPrice = foodItem.price × quantity` and drops every `priceAdjustment`. Add "Cheese Stuffed Crust +3.50", change the quantity, and the charge silently falls back to the base price.

Fix: reuse the same modifier-aware calculation `CartService.updateItemPrice` already has. Make that method non-private and call it. Also reject `quantity < 1`.
`controller/CartController.java:49` — ~5 lines.

---

#### `[ ]` F-3. Reorder deletes the cart when it fails

`reorder` calls `clearCart` first, then re-adds items one by one. If any item is out of stock or the restaurant is closed, it throws — the cart is already empty and nothing was added. Not `@Transactional`, so the clear is committed.

Fix: add `@Transactional` so a failure rolls the clear back.
`service/CartService.java:182` — 1 annotation.

---

#### `[ ]` F-4. Fresh database is completely empty

`data.sql` never runs on MySQL — `spring.sql.init.mode` defaults to `embedded`. Anyone cloning the repo gets a working app with zero restaurants. The file also omits `latitude`, `longitude`, `delivery_radius_km`, and `stock_quantity`, which is exactly what caused the NPE logged as issue #3 in `DEVELOPMENT_CHALLENGES.md`.

Fix: set `spring.sql.init.mode=always`, add the missing columns to the seed rows, and guard the inserts so restarts don't duplicate.
`resources/application.properties`, `resources/data.sql` — small.

---

#### `[ ]` F-5. Checkout hangs on slow network

`GeocodingService` sets no connect or read timeout. If Nominatim is slow or unreachable, the request thread blocks forever — and this call sits inside the `@Transactional placeOrder`, holding a DB connection while it waits. Bad demo: user clicks "Place Order" and the page hangs with no error.

Fix: 3s connect / 5s read timeout. On timeout, fail with a clear message instead of hanging.
`service/GeocodingService.java:37` — 2 lines.

---

#### `[ ]` F-6. Null crashes on a fresh database

`stockQuantity`, `deliveryRadiusKm`, `acceptsScheduledOrders`, and `isOpen` are all unboxed without null checks. Entity defaults only apply to rows created through JPA — any row inserted via SQL has nulls, and every one of these throws a 500.

Fix: null-safe checks at the four call sites.
`service/OrderService.java:121,125`, `service/RestaurantHoursValidator.java:179` — ~8 lines.

---

#### `[ ]` F-7. Frontend white-screens on null values

`order.totalAmount.toFixed(2)` and `mod.modifier.priceAdjustment.toFixed(2)` have no guards. One null value blanks the whole page.

Fix: `(value ?? 0).toFixed(2)` at both sites.
`pages/OrderHistory.js:136`, `pages/Cart.js:163` — 2 lines.

---

### A2. Basic security — minimal, not enterprise

#### `[ ]` F-8. Passwords stored in plaintext

`registerUser` saves the raw string; `loginUser` compares with `.equals()`.

Fix: add a `BCryptPasswordEncoder` bean, `encode()` on register, `matches()` on login. Existing seed users get rehashed transparently the first time they log in successfully (no migration script, no forced reset).
`service/UserService.java:16,21` + one `@Bean` — ~15 lines.

---

#### `[ ]` F-9. Password sent back to the browser

`/api/auth/register` and `/api/auth/login` return the full `User` entity including the password, and `Login.js` writes that whole object into `localStorage`.

Fix: one annotation — `@JsonProperty(access = WRITE_ONLY)` on `User.password`. It still accepts a password on the way in and never serializes it on the way out. No DTO layer needed.
`model/User.java:19` — 1 line.

---

#### `[ ]` F-10. Anyone can set any order to DELIVERED

`PUT /api/order/{orderId}/status` takes any arbitrary string with no validation.

Fix (simple version): validate the value against the known status list and reject anything else with 400. **No role checks, no state machine** — that's Phase 02.
`controller/OrderController.java:68` — ~6 lines.

> Note: this leaves the API open to unauthenticated callers. That is acceptable while the repo is **private**. Full auth is the Phase 02 decision below.

---

### A3. Features that look broken in a demo

#### `[ ]` F-11. The ETA never counts down

`calculateETA` returns `now + fixedMinutesForStatus`, recomputed on every 15s poll — so the tracking page shows the same number forever and only jumps when the status changes. It also NPEs if `status` is null.

Fix: store `estimatedDeliveryAt` on the order when it's placed, and compute the remaining minutes against it. Add a null guard on the switch.
`service/OrderETAService.java:38,87` + one column — ~15 lines.

---

#### `[ ]` F-12. Marking an item sold-out destroys its daily restock

`markOOS` hardcodes `stockResetType = "MANUAL"`. An owner marking today's special as sold out permanently loses its `DAILY` auto-restock config.

Fix: don't overwrite `stockResetType`.
`controller/FoodItemController.java:79` — delete 1 line.

---

#### `[ ]` F-13. Required modifiers aren't enforced

`ModifierGroup` has `minSelection` / `maxSelection` / `required`, and the UI renders them, but `addToCart` accepts any modifier list. You can order a pizza with no crust selected. It also never checks `Modifier.available`.

Fix: validate the selection against the group rules in `addToCart`, reject unavailable modifiers.
`service/CartService.java:74` — ~20 lines.

---

#### `[ ]` F-14. Restaurant shows as closed at its opening minute

`isTimeInRange` uses strict `isAfter`/`isBefore`, so 10:00 AM fails for a restaurant that opens at 10:00 AM.

Fix: inclusive comparison.
`service/RestaurantHoursValidator.java:39` — 1 line.

---

#### `[ ]` F-15. Orders have no payment record

`placeOrder` never creates a `Payment`, and `PaymentController.pay` returns a hardcoded mock without persisting. `Order.payment` is always null, so there's no record any order was paid.

Fix (simple version): create one COD `Payment` row inside `placeOrder` — method `COD`, status `PENDING`. Set it to `CANCELLED` when the order is cancelled. **No gateway, no Razorpay** — that's Phase 02.
`service/OrderService.java` — ~10 lines.

---

### A4. Cleanup — trivial, zero risk

#### `[ ]` F-16. Delete dead code

- `hooks/useOrderPolling.js` — broken (relative URL, no CRA proxy → 404) and imported by nothing. Delete it.
- `RestaurantHoursValidator.java:96-105` — a dead `.ifPresent()` with an empty lambda body, followed by the same stream repeated for real.
- `OrderETAService.java:19` — `DistanceCalculationService` autowired, never used. Same file :58-72 — an `if`/`else` whose two branches are identical.
- `OrderService.java:57` — unannotated 3-arg `placeOrder` overload that self-invokes the `@Transactional` version. Dead, but a landmine if anyone calls it (Spring proxy bypassed → no transaction).

All deletions. No behaviour change.

---

#### `[ ]` F-17. Guard the infinite loop

`generateTimeSlots` advances with `slotTime.plusMinutes(slotDuration)`. `slotDurationMinutes` is settable via the settings API with no validation — set it to `0` and the loop never terminates, hanging the thread.

Fix: `if (slotDuration == null || slotDuration <= 0) slotDuration = 30;` in both copies.
`controller/RestaurantController.java:217`, `controller/RestaurantSettingsController.java:106` — 2 lines.

---

### A5. Tests — small and targeted

#### `[ ]` F-18. ~8 tests, not a suite

One test per bug that actually broke something, so these can't silently come back:

- `UNLIMITED` item survives 200 orders (F-1)
- modifier price survives a quantity change (F-2)
- failed reorder leaves the cart intact (F-3)
- null `stockQuantity` doesn't 500 (F-6)
- BCrypt: register then login succeeds; wrong password fails (F-8)
- login response contains no `password` field (F-9)
- required modifier group rejects an empty selection (F-13)
- `slotDuration=0` returns slots instead of hanging (F-17)

Plain JUnit 5 + Mockito. `spring-boot-starter-test` is already in `pom.xml` and currently unused.

---

## PART B — TWO QUESTIONS

Only two things I can't decide for you.

### 1. JWT login — in or out?

Not needed for anything to *work*. But:

- `Resume_Project_Entry.md` currently claims **"secure JWT-based Authentication via Spring Security"** — which is not true today
- `SUGGESTIONS.md` tags it 🔥 Must Have because it's the #1 interview question
- Realistically ~3 small files (`JwtService`, a filter, a `SecurityConfig` edit) + an axios interceptor

Three options:

- **(a) Include it** — resume claim becomes true, standard interview answer. Adds ~1 workstream.
- **(b) Phase 02** — Phase 01 stays purely bug fixes. Keep the repo private until then.
- **(c) Skip, and soften the resume line** to "Spring Security + BCrypt password hashing".

F-8 (BCrypt) and F-9 (hide password) are in Phase 01 either way — both are tiny.

### 2. Branch

New branch `phase-01` off `main`, one commit per fix, then a PR you review before merging? Or commit straight to `main`?

---

## PART C — SIZE

| Group | Items | Roughly |
|---|---|---|
| A1 — demo-breaking bugs | F-1 … F-7 | ~10 files, small edits |
| A2 — basic security | F-8 … F-10 | ~4 files |
| A3 — broken-looking features | F-11 … F-15 | ~6 files |
| A4 — cleanup | F-16, F-17 | deletions + 2 lines |
| A5 — tests | F-18 | 3 new test files |

Total: roughly 20 files touched, almost all small edits. No new layers, no migrations.

**If you want the smallest useful set: A1 + F-8 + F-9.** That makes every core flow correct and stops the plaintext-password problem, and it's about half a day of changes.
