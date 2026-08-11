# Phase 02 — Live Rider Tracking

**Status:** plan only. No code written.
**Goal:** a real delivery partner carries the order, reports position from their phone browser, and the customer watches them approach on a map with an ETA that reflects actual distance.

---

## PART A — WHERE WE ARE

### Already in place

| Piece | Detail |
|---|---|
| Maps | Leaflet + react-leaflet installed, `AddressMap.js` already draws one |
| Roles and tokens | `USER`, `RESTAURANT_OWNER`, `ADMIN`; adding a fourth is a small change |
| `OUT_FOR_DELIVERY` stage | already in `OrderStatusFlow` |
| Distance | `DistanceCalculationService` (Haversine) |
| Restaurant coordinates | `latitude` / `longitude` on `Restaurant` |
| Live polling | the floating tracker already polls every 15s |
| Ownership rules | `CurrentUser` centralises them |

### The blocker nobody has hit yet

**An order does not store where it is going.**

`Order` has `deliveryAddress` as free text. The coordinates *are* worked out at checkout — `OrderService.placeOrder` geocodes the address to check the delivery radius — and then thrown away.

Without a destination coordinate there is nothing to measure a rider against, so no ETA and no map line. This must be fixed first, and it is cheap: keep what we already geocode.

```java
// OrderService.placeOrder already has this result and discards it
GeocodingService.GeocodingResult result = geocodingService.geocodeAddress(deliveryAddress);
```

**A1. Add `deliveryLatitude` / `deliveryLongitude` to `Order`, populated at placement.**
Existing orders get null and simply show no map, which is correct — we genuinely do not know where they went.

---

## PART B — DATA MODEL

### B1. Rider identity

Reuse `User` with a new role `DELIVERY_PARTNER` rather than a separate table. Riders log in, are authorised and are refused exactly like everyone else, with no parallel auth path.

New entity `RiderProfile`:

| Field | Why |
|---|---|
| `user` | one-to-one with the account |
| `vehicleType` | BIKE / SCOOTER / BICYCLE — affects assumed speed |
| `isOnline` | riders are not always working |
| `currentLatitude`, `currentLongitude` | last reported position |
| `locationUpdatedAt` | how stale that position is — critical, see E7 |
| `activeOrderId` | what they are carrying, null when free |

### B2. Assignment on the order

| Field | Why |
|---|---|
| `rider` (`User`) | who is carrying it |
| `riderAssignedAt` | when they accepted |
| `pickedUpAt` | when they collected it |
| `deliveredAt` | when they handed it over |

`activeOrderId` on the profile and `rider` on the order are two sides of the same fact. The order is the authority; the profile field is a convenience for "is this rider free", and must be written in the same transaction.

### B3. Location history — deliberately not stored

Only the current position is kept. A breadcrumb trail is a privacy liability, grows without bound, and buys nothing the customer needs. If a route replay is ever wanted, that is a separate decision with its own retention answer.

---

## PART C — THE LIFECYCLE, REVISITED

### Today

```
PLACED → CONFIRMED → PREPARING → OUT_FOR_DELIVERY → DELIVERED
```

The restaurant taps every step, including `OUT_FOR_DELIVERY`, which today means nothing more than "we think it left".

### With riders

```
PLACED → CONFIRMED → PREPARING → READY_FOR_DELIVERY → OUT_FOR_DELIVERY → DELIVERED
                                        ↑                    ↑                ↑
                                  restaurant            rider picks up    rider hands over
                                  finished cooking
```

**C1. New stage `READY_FOR_DELIVERY`.** The restaurant has finished; the food is on the counter waiting for a rider. This is the state the current model cannot express, and it is exactly the window where a rider is needed.

**C2. Who moves what changes.**

| Transition | Today | With riders |
|---|---|---|
| `PREPARING → READY_FOR_DELIVERY` | n/a | restaurant |
| `READY_FOR_DELIVERY → OUT_FOR_DELIVERY` | restaurant | **rider**, on pickup |
| `OUT_FOR_DELIVERY → DELIVERED` | restaurant | **rider**, on handover |

`OrderStatusFlow.validateTransition` currently only asks *what* the move is. It will need to ask *who* is asking, because the same transition is legal for a rider and illegal for a restaurant.

**C3. Pickup orders are untouched.** `PREPARING → READY_FOR_PICKUP → PICKED_UP` involves no rider at all. Every rule below applies to `DELIVERY` orders only.

---

## PART D — THE FLOWS

### D1. Rider

1. Signs in, opens the rider portal, toggles **online**
2. Browser asks for location permission and begins reporting
3. Sees a list of orders at `READY_FOR_DELIVERY`, each with restaurant, distance from them, and payout
4. Taps **Accept** on one
5. Navigates to the restaurant, taps **Picked up** → order becomes `OUT_FOR_DELIVERY`
6. Navigates to the customer, taps **Delivered** → order becomes `DELIVERED`, rider is free again
7. Location reporting stops when they go offline or the delivery ends

### D2. Customer

On the tracking page, once a rider is assigned:

- A map with three markers: restaurant, rider, destination
- The rider marker moves as new positions arrive
- ETA computed from the rider's actual distance
- Rider's first name and a masked phone number — see G3
- Before assignment, the existing countdown is unchanged

### D3. Restaurant

One extra step: mark an order **ready for delivery** instead of **out for delivery**. Then it waits for a rider, and the dashboard should say so rather than looking stalled.

### D4. Admin

Riders online, unassigned orders, and anything sitting too long. Small addition to the existing admin page.

---

## PART E — EVERY CASE I CAN THINK OF

This is the part that decides whether the feature is real or a demo.

### E1. Two riders accept the same order at the same moment

The obvious implementation reads the order, sees no rider, and writes one — two riders both pass that check.

**Handle with a conditional update**, not a read-then-write:

```sql
UPDATE orders SET rider_id = ? WHERE id = ? AND rider_id IS NULL
```

Zero rows updated means somebody else won; that rider is told the job is gone. `@Version` on `Order` is the alternative.

### E2. A rider accepts, then vanishes

Goes offline, closes the tab, or the phone dies. The order is assigned to someone who is not moving.

**Rule:** if a rider's location has not updated for **5 minutes** while carrying an order, mark the assignment stale, release the order back to the pool, and set the rider offline. The customer sees "finding another rider" rather than a frozen dot.

This is a background job — the same shape as the one just removed, but far narrower and only touching assignment, never cancelling anybody's order.

### E3. Rider accepts but never collects

Assigned, but the food is still on the counter 15 minutes later. Same treatment as E2: release and re-pool. The restaurant should see this happen.

### E4. Order cancelled after a rider was assigned

Cancellation is currently blocked once `PREPARING`, so a customer cannot cancel from under a rider. But a **restaurant** can still reject. In that case: unassign the rider, notify them, free them for other work.

### E5. Rider denies location permission

Browsers can refuse, and a rider might deliberately decline.

**Rule:** no location, no going online. Explain why in the UI. A rider with no position cannot be matched sensibly nor shown to a customer, and pretending otherwise produces a tracker that lies.

### E6. Location is available but rubbish

`navigator.geolocation` reports `accuracy` in metres; indoors it can be 2 km wide. Positions worse than **100 m** should be recorded but flagged, and the map should show an uncertainty circle rather than a confident pin.

### E7. Position is stale

The rider is in a tunnel. The last known point is 4 minutes old and the marker sits still, implying they have stopped.

**Rule:** over **60 seconds** old, the customer sees "last seen a minute ago" instead of a live position. `locationUpdatedAt` is what makes this possible, which is why it is in the model.

### E8. Destination coordinates are missing

Older orders, or an address that failed to geocode. **Rule:** no map, keep the fixed-time countdown, do not guess.

### E9. Rider is assigned two orders

Real platforms batch deliveries. **Not in this phase.** One order at a time, enforced by `activeOrderId`. Batching changes matching, ETA and the map, and is a phase of its own.

### E10. Rider marks delivered from the wrong place

Nothing stops a rider tapping "Delivered" from 5 km away.

**Rule:** warn beyond **500 m** from the destination and require confirmation. Record the position at the moment of the tap for later dispute. Do not hard-block: GPS drifts, and a blocked rider with the food at the door is worse.

### E11. No rider is available

Nobody is online, or all are busy. The order sits at `READY_FOR_DELIVERY`. **Rule:** after 10 minutes tell the customer honestly ("still finding a rider") and flag it on the admin page. Never silently cancel — that is precisely what went wrong last time.

### E12. Rider goes offline while free

Fine. They simply stop appearing in matching.

### E13. Rider account reuse

Can a rider also be a customer? **No** — same reasoning as partner accounts: separate account, separate portal. Consistent with the decision already made.

### E14. The customer's tab is closed

Polling stops. Nothing breaks; the position is on the server. On reopening they see the current state.

### E15. Two customers, one rider

Only ever one active order per rider in this phase, so a rider's position is only ever visible to one customer.

### E16. Restaurant marks ready before food is ready

A human error, not a software one. Out of scope.

### E17. Rider delivers, then the customer disputes

`deliveredAt` plus the position at the tap gives an audit trail. No dispute workflow in this phase.

### E18. Rider's device clock is wrong

Never trust client timestamps. `locationUpdatedAt` is set **server-side** on receipt.

### E19. Location updates flood the server

`watchPosition` can fire several times a second while moving.

**Rule:** the client sends at most one update every **5 seconds**, and only when moved more than **20 m**. The server rejects anything faster. Otherwise a handful of riders becomes hundreds of writes a minute for nothing.

### E20. Scheduled orders

An order for 8pm should not be pooled at 2pm. Only pool when the restaurant marks it ready, which already handles this correctly.

---

## PART F — ETA, HONESTLY

Today's ETA is a promise made at checkout: `now + 35 minutes`. With a rider it can be a measurement.

```
minutes = straightLineDistance(rider, destination) / assumedSpeed × detourFactor
```

- `assumedSpeed` by vehicle: bike 25 km/h, scooter 30 km/h, bicycle 15 km/h in traffic
- `detourFactor` of **1.3**, because roads are not straight lines

**F1. Straight-line, not routed.** Real road distance needs OSRM or Google Directions — an external dependency, a key, and rate limits. Straight-line with a detour factor is within about 20% in a city and needs nothing. Say this plainly rather than implying real routing.

**F2. Only once a rider is carrying it.** Before pickup, the fixed estimate stands: there is nobody to measure.

**F3. Never let it go up and down.** A recomputed number that jumps 12 → 15 → 11 reads as broken. Keep the lowest figure shown so far unless it rises by more than 5 minutes, which is real news and worth showing.

---

## PART G — SECURITY

Every rule enforced server-side in `CurrentUser`, as with everything else.

**G1.** A rider sees full details only for their **assigned** order. The pool shows restaurant, area and distance — never the customer's exact address until they accept.

**G2.** A rider's position is visible only to the customer of the order they are carrying, plus admins. Never to other riders, never to other customers, never publicly.

**G3.** The customer sees the rider's **first name** and a **masked phone number**. The rider sees the customer's address only after accepting, and it disappears from their view after delivery.

**G4.** Only the assigned rider may mark that order picked up or delivered. A restaurant cannot, and neither can another rider — which the current flow would allow, since it only checks the transition, not the actor.

**G5.** Location updates apply only to the rider's own profile. `POST /api/rider/location` takes no rider id; it uses the token.

---

## PART H — TOUCH POINTS

### Backend

| File | Change |
|---|---|
| `model/Order.java` | delivery coordinates, rider, three timestamps |
| `model/RiderProfile.java` | **new** |
| `model/User.java` | `DELIVERY_PARTNER` role |
| `repository/RiderProfileRepository.java` | **new** |
| `repository/OrderRepository.java` | pool query, stale-assignment query, conditional assign |
| `service/OrderStatusFlow.java` | `READY_FOR_DELIVERY`; transitions become actor-aware |
| `service/OrderService.java` | persist delivery coordinates; assignment, pickup, delivery |
| `service/RiderMatchingService.java` | **new** — pool, accept, release |
| `service/RiderLocationService.java` | **new** — updates, throttling, staleness |
| `service/OrderETAService.java` | distance-based ETA once carried |
| `service/StaleAssignmentReleaser.java` | **new** — E2, E3 |
| `controller/RiderController.java` | **new** |
| `controller/OrderController.java` | expose rider position to that order's customer |
| `security/CurrentUser.java` | `requireAssignedRider`, `requireRider` |
| `security/AuthenticatedUser.java` | `isRider()` |
| `config/SecurityConfig.java` | `/api/rider/**` |
| `config/DataSeeder.java` | a demo rider account |

### Frontend

| File | Change |
|---|---|
| `pages/rider/RiderPortal.js` | **new** — online toggle, pool, active delivery |
| `pages/rider/RiderDelivery.js` | **new** — navigation, picked up, delivered |
| `components/RiderNavbar.js` | **new** — third shell, alongside customer and partner |
| `components/DeliveryMap.js` | **new** — three markers, moving rider |
| `components/LiveOrderBanner.js` | rider name and live ETA |
| `pages/OrderTracking.js` | map once assigned |
| `pages/RestaurantDashboard.js` | "ready for delivery"; show waiting-for-rider |
| `pages/AdminDashboard.js` | riders online, unassigned orders |
| `hooks/useRiderLocation.js` | **new** — `watchPosition`, throttling, permission handling |
| `App.js` | `/rider/*` routes, guards, third shell |
| `services/api.js` | rider endpoints |

**Roughly 27 files, 12 of them new.** Comparable to the authentication work, and the largest single feature in the project.

---

## PART I — API

```
POST   /api/rider/online              go online (requires a position)
POST   /api/rider/offline
POST   /api/rider/location            {lat, lng, accuracy}   throttled
GET    /api/rider/pool                orders awaiting a rider, nearest first
POST   /api/rider/orders/{id}/accept  conditional, wins or loses
POST   /api/rider/orders/{id}/pickup  → OUT_FOR_DELIVERY
POST   /api/rider/orders/{id}/deliver → DELIVERED, records position
GET    /api/rider/current             the rider's active delivery
GET    /api/order/{id}/tracking       rider position + ETA, customer only
```

---

## PART J — DEMONSTRATING IT

Two devices, or two browser windows on one machine.

**J1. A simulated rider**, off by default and labelled, that walks a position from restaurant to destination over a couple of minutes. This is the only way to show the feature in a viva without a second person on a bike, and the same honesty rule as demo mode applies: it must never be mistakable for real tracking.

**J2. Fixed coordinates for testing**, so a rider can be placed at a chosen point without leaving the desk.

---

## PART K — SEQUENCE

Each step leaves the app working.

1. **Persist delivery coordinates** — small, and everything else depends on it
2. **Rider role, account, portal shell** — sign in, go online, nothing to do yet
3. **`READY_FOR_DELIVERY` and actor-aware transitions** — restaurant side finished
4. **Pool and accept**, including the race (E1)
5. **Location reporting**, throttled, with staleness
6. **Pickup and delivery**, with the distance warning (E10)
7. **Customer map and live ETA**
8. **Stale-assignment release** (E2, E3)
9. **Admin view and the simulated rider**

---

## PART L — NOT IN THIS PHASE

Batched deliveries · road routing · rider earnings and payouts · rider ratings · shift scheduling · push notifications · chat between customer and rider · proof-of-delivery photos · dispute handling · route replay.

Each is a real feature in a real platform. None is needed to make live tracking work, and every one of them makes the first version later and shakier.

---

## PART M — WHAT I NEED FROM YOU

1. **Assignment model** — riders pick from a pool, as planned, or the system assigns the nearest automatically? The pool is simpler, more honest under failure, and easier to demo. Automatic assignment sounds cleverer and hides more problems.
2. **`READY_FOR_DELIVERY`** — happy to add the stage? It is the correct model, but it does mean an extra tap for the restaurant.
3. **Vehicle types** — worth having for speed, or assume one speed for everyone?
4. **The simulated rider** — build it? I would, but it is your call whether a labelled fake belongs in the repository.

*Answer these and I will build it in the order in Part K.*
