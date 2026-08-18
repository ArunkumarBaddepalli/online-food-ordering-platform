# Putting this online

Two addresses come out of this: the site people visit, and the API behind it.

Everything needed is already in the repository. What is not here, and cannot be,
is anything secret — you supply those once, in the dashboard.

---

## What you get

| Piece | Where it runs | Cost |
|---|---|---|
| The site | Render static hosting | free |
| The API | Render web service, built from `backend/Dockerfile` | free |
| The database | Render PostgreSQL | free for 30 days |

The database runs on PostgreSQL rather than MySQL, because no free host offers
MySQL. Nothing in the code changes: MySQL is still what runs on your machine, and
the driver for both is included. The tables build themselves on first start, and
the demo restaurants and menu are seeded into an empty database.

---

## Deploying

1. Sign in at [dashboard.render.com](https://dashboard.render.com) with GitHub.
2. **New** → **Blueprint**.
3. Choose `ArunkumarBaddepalli/online-food-ordering-platform`, branch `bugfix`.
4. Render reads `render.yaml` and lists a database, an API and a site.
5. It asks for the values below. Fill them in and **Apply**.

The first build takes about ten minutes: it compiles the backend inside Docker.

### What it asks for

| Asked for | What to give it |
|---|---|
| `APP_BASE_URL` | The address of your site, e.g. `https://food-delivery-web.onrender.com`. Links in emails point here. |
| `REACT_APP_API_URL` | The address of your API, e.g. `https://food-delivery-api.onrender.com`. |
| `MAIL_USERNAME` | `fooddelivery.orders.noreply@gmail.com` |
| `MAIL_PASSWORD` | The 16-character app password from Google |
| `MAIL_FROM` | The same address as `MAIL_USERNAME` |
| `RAZORPAY_KEY_ID` | Your test key id |
| `RAZORPAY_KEY_SECRET` | Your test key secret |

Render names services after this file, so the two addresses are predictable
before they exist. If you rename anything, use the real address instead.

### After it is up

The site is built with the API address baked in, so it must be built **after**
you know that address. If you filled it in above, it already is. If you change
it later, redeploy the site — rebuilding is what picks it up.

Then check the API log says:

```
Email is live: sending through smtp.gmail.com as ...
```

If it says email is not being sent, the mail values are missing. Either add them
or set `ORDERS_REQUIRE_VERIFIED_EMAIL` to `false`, because otherwise a new
account can never confirm its address and so can never order.

---

## Signing in to the deployed copy

The empty database is seeded with two accounts:

| Account | Password | Can do |
|---|---|---|
| `admin@example.com` | `admin123` | Approve restaurant applications |
| `john@example.com` | `password` | Order |

Change the admin password before showing this to anybody.

---

## Things worth knowing before you demonstrate it

**The API sleeps.** On the free plan it shuts down after fifteen minutes of
quiet, and the next request waits about fifty seconds while it wakes. Open the
site a minute before you present, or it looks broken.

**The free database expires after thirty days.** Render deletes it. If your
submission is assessed later than that, either upgrade the database or redeploy
close to the date.

**Uploaded documents do not survive a restart.** Restaurant applications keep the
file name in the database, but the file itself is written beside the running
program, and free hosting gives that no permanence. Everything else — accounts,
orders, payments, reviews — is in the database and safe.

**Order times follow one clock.** They are stored without a zone, so the server's
zone is what they mean. The blueprint sets it to `Asia/Kolkata`. Change `TZ` if
your users are elsewhere.

**Home delivery is off**, because there are no riders yet. The site offers
collection only, and the API refuses delivery orders rather than merely hiding
the option.

---

## Running it locally, unchanged

Nothing here affects local development. MySQL, `.env.local`, `mvn spring-boot:run`
and `npm start` behave exactly as before.
