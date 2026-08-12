# Order Lifecycle & Receipts — delivered

**Status: built and in use.** Kept for the reasoning behind the design; it is no
longer a plan. Owners move orders through their stages from the partner portal,
and prepaid orders offer a receipt while cash orders do not.

"Phase 02" now means rider tracking, which is [PHASE_02_RIDER_PLAN.md](PHASE_02_RIDER_PLAN.md)
and is still unbuilt.

Two pieces of work:

1. **Restaurant order management** — give owners a screen where they can move an order through its stages, so orders stop being frozen at PLACED.
2. **Receipts** — download available for prepaid orders only. Cash-on-delivery gets nothing, because the restaurant hands over a printed bill.

---

## PART A — WHY ORDERS ARE FROZEN

Nothing in the app can advance an order.

- The backend writes exactly two statuses: `PLACED` when the order is made, `CANCELLED` when it is cancelled.
- `PUT /order/{id}/status` exists and works, but **no frontend code calls it**. There is no such function in `api.js`.
- The owner's dashboard is 88 lines showing their onboarding application. It has no orders section.
- `OrderRepository` has only `findByUserId`. There is no way to fetch a restaurant's orders.

Proof from your database: **16 PLACED, 1 CANCELLED.** Nothing else has ever existed.

### The blocker

A restaurant has no owner.

```
Restaurant table columns: id, name, description, address, imageUrl,
latitude, longitude, deliveryRadiusKm, phone, email, openingTime,
closingTime, isOpen, acceptsScheduledOrders, slotDurationMinutes
```

There is no `user_id`. So the app cannot answer "which restaurant does this owner manage?", and an orders screen has nothing to filter by.

`restaurant_onboarding` does have `user_id` and `created_restaurant_id`, which is populated when an admin approves an application — that is the intended link. But in your data:

| onboarding_id | user_id | restaurant_name | status | created_restaurant_id |
|---|---|---|---|---|
| 1 | 5 | restaurant-01 | PENDING_REVIEW | **NULL** |

The one application was never approved, so no restaurant was created. **All 11 of your restaurants are unowned**, and a backfill from onboarding would link nothing.

That is why Part D question 1 matters — without answering it, the new screen would be empty.

---

## PART B — WORK ITEMS

### B1. Link a restaurant to its owner

- Add `Restaurant.owner` (`@ManyToOne` to `User`).
- Populate it when an admin approves an onboarding application — the code already creates the restaurant there, it just never records who owns it.
- Backfill existing rows from `restaurant_onboarding.created_restaurant_id` where present.

Schema change. Hibernate adds the column automatically; existing rows get NULL.

### B2. Fetch a restaurant's orders

- `OrderRepository.findByRestaurantIdOrderByOrderDateDesc`
- `GET /api/order/restaurant/{restaurantId}` returning orders newest first, with the same ETA block the customer's active-orders endpoint already uses.

### B3. Status progression, enforced

Right now `PUT /order/{id}/status` accepts any of six values with no rules — an order can jump straight from PLACED to DELIVERED, or backwards.

Add a transition table:

```
PLACED           -> CONFIRMED, CANCELLED
CONFIRMED        -> PREPARING, CANCELLED
PREPARING        -> OUT_FOR_DELIVERY (delivery) | READY_FOR_PICKUP (pickup)
OUT_FOR_DELIVERY -> DELIVERED
READY_FOR_PICKUP -> PICKED_UP
DELIVERED        -> (end)
PICKED_UP        -> (end)
CANCELLED        -> (end)
```

Anything else returns 400 with the allowed next steps. This also settles the vocabulary mismatch: the frontend already lists `READY_FOR_PICKUP` and `PICKED_UP`, but the backend rejects them today.

### B4. Owner dashboard — orders panel

Added to the existing dashboard:

- Incoming orders, newest first, grouped New / In progress / Done
- Per order: number, time, customer name, items with modifiers, total, payment method and status
- One button showing the next legal step ("Accept", "Start preparing", "Out for delivery", "Mark delivered")
- Refresh every 20s so new orders appear without a reload

### B5. Customer-side follow-through

- Order tracking shows the live stage; the ETA already counts down against a fixed target
- Order history "Ongoing" and "Completed" tabs become meaningful, since orders can now reach DELIVERED
- The floating banner already reflects status — it will start changing as stages advance

### B6. Receipts — your rule

> Cash on delivery: no receipt, the restaurant gives a printed bill.
> Prepaid: downloadable receipt.

Implemented exactly as stated:

- A **Download receipt** button appears only when `paymentMethod != "COD"` **and** `paymentStatus == "PAID"`.
- COD orders show a line reading "Pay in cash on delivery — the restaurant will provide a bill." No button.
- The receipt is a printable page (browser print → Save as PDF). No PDF library, no new dependency.
- Contents: restaurant name and address, order number, date, customer, items with modifiers and line prices, total, payment method, transaction reference, paid timestamp.

**This cannot be exercised yet.** Every order in the app is COD, and no payment is ever marked PAID — `PaymentController.pay` is still a mock that returns a fake transaction id and saves nothing. So the rule would be correct but permanently dormant until an online payment path exists. See Part D question 2.

---

## PART C — FILES

| Area | Files |
|---|---|
| Owner link | `Restaurant.java`, `RestaurantOnboardingService.java`, backfill |
| Orders API | `OrderRepository.java`, `OrderController.java`, `OrderService.java` |
| Transitions | `OrderService.java` (+ tests) |
| Dashboard | `RestaurantDashboard.js`, `api.js` |
| Customer side | `OrderTracking.js`, `OrderHistory.js` |
| Receipt | new `Receipt.js`, `OrderTracking.js` |

Roughly 10 files. Tests for the transition rules, since that is the part with actual logic.

---

## PART D — QUESTIONS I NEED ANSWERED

### 1. How does an owner end up with a restaurant to manage?

Your only owner account (user 5, `baddepalliarunkumar@gmail.com`) has a **pending** application and owns nothing. All 11 restaurants are unowned. Without a decision here the new screen shows an empty list.

- **(a)** Approve the pending application through the existing admin flow. Creates a brand new restaurant, correctly owned. But it has no menu and no orders, so nothing to look at.
- **(b)** Assign an existing restaurant to user 5 — Pizza Palace has 6 orders, so the screen has real data immediately.
- **(c)** Both: assign Pizza Palace now for testing, and fix approval so future applications link properly.

### 2. Prepaid — required before any receipt can appear

- **(a)** Razorpay test mode. Real integration, free, no real money. **Needs a Razorpay account and test API keys from you.**
- **(b)** A simulated "Pay Online" option at checkout that marks the payment PAID immediately. Makes receipts fully testable today, no signup. It is simulated, and I would label it as such in the UI so it is not mistaken for a real gateway.
- **(c)** Build the receipt rule now and leave it dormant until online payment exists later.

### 3. Pickup orders

Should PICKUP orders use `READY_FOR_PICKUP -> PICKED_UP` instead of `OUT_FOR_DELIVERY -> DELIVERED`? Your frontend already lists those statuses, so it looks intended. Confirm and I will wire it that way.

### 4. Cancellation window

Customers can currently cancel while an order is PLACED or CONFIRMED. Once a restaurant starts cooking (PREPARING), should cancellation be blocked?

---

*Answer Part D and I will build it.*
