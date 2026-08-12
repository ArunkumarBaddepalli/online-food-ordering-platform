# Sending real email

The app sends five kinds of message:

| When | To |
|---|---|
| Someone registers | Confirm your email address |
| Someone forgets their password | A reset link, valid 30 minutes |
| An admin approves, rejects or asks for documents | The applicant, with the reason given |
| An order is delivered or collected | The customer |
| An order is cancelled | The customer, including refund status |

All of it is built. **Until a mail server is configured nothing is delivered** — messages are written to the server log instead, so the app runs fine without credentials.

---

## Setting it up with Gmail

Free, about 500 messages a day, which is far beyond what this needs.

### 1. Turn on 2-Step Verification

Google Account → **Security** → **2-Step Verification**.

App passwords do not exist without it.

### 2. Create an app password

Google Account → **Security** → **2-Step Verification** → **App passwords**.

Choose **Mail**, name it anything, and Google shows a 16-character code.

This is not your Google password. It only works for sending mail, and you can revoke it at any time without touching your account.

### 3. Put it in `backend/.env.local`

That file is gitignored, so nothing here reaches GitHub.

```properties
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your.address@gmail.com
MAIL_PASSWORD=the16charcode
MAIL_FROM=your.address@gmail.com
```

`MAIL_FROM` should be the same address. Gmail rejects attempts to send as somebody else.

### 4. Restart the backend

The log says which state it is in:

```
Email is live: sending through smtp.gmail.com as your.address@gmail.com
```

or

```
Email is NOT being sent. Messages will be logged instead.
```

### 5. Prove it works

Sign in as an administrator and send yourself a test:

```bash
TOKEN=$(curl -s -X POST http://localhost:8082/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@fooddelivery.local","password":"admin12345"}' \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")

curl -X POST http://localhost:8082/api/config/test-email \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"email":"your.address@gmail.com"}'
```

Check the inbox, and the spam folder. Mail from a personal Gmail account to itself sometimes lands there the first time.

---

## Confirmed addresses are required

This is **already on**, in `application.properties`:

```properties
orders.require-verified-email=true
```

A new account must click the link in its confirmation email before it can order. Checkout says so plainly and offers to send the link again.

Accounts that existed before verification was added are already marked confirmed and are never asked.

Set it back to `false` if you ever run without a mail server. With it on and no mail working, a new account can never confirm and so can never order.

---

## If nothing arrives

| Symptom | Cause |
|---|---|
| Log says "NOT being sent" | `MAIL_HOST` is unset — check `.env.local` is in `backend/` |
| `535 Username and Password not accepted` | Using the Google password instead of the app password, or 2-Step Verification is off |
| `Connection timed out` | Port 587 blocked, common on college and office networks |
| Log says sent, nothing arrives | Check spam; check `MAIL_FROM` matches `MAIL_USERNAME` |

---

## Other providers

Nothing is Gmail-specific — it is plain SMTP. Any of these work by changing the same four values:

| | Free allowance |
|---|---|
| Brevo | 300 a day |
| Mailjet | 6,000 a month |
| Resend | 3,000 a month |

All require an account. There is no way to send real email without authenticating to something.
