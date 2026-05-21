# Portico Online Recurring Payments

> Set up recurring payment schedules via the Global Payments Portico gateway, demonstrated in PHP, Node.js, Java, and .NET.

## Critical Patterns

1. **Recurring setup is a three-step SDK chain: customer → payment method → schedule.** `customer.create()` must succeed before calling `addPaymentMethod()`, and that must succeed before calling `addSchedule()`. The schedule is attached to the stored payment method, not the card token directly — skipping either intermediate step will fail silently or throw a configuration error.

2. **The schedule dates and frequency are hardcoded in every implementation.** Start: `2027-02-01`, end: `2027-04-01`, frequency: Weekly, reprocessingCount: 2. These are not read from user input or environment variables. An agent adapting this sample for a different billing period must update all four implementations independently.

3. **PHP uses file-based routing — its paths differ from all other languages.** `GET /config.php` and `POST /process-payment.php` are the PHP endpoints; the other three languages expose `GET /config` and `POST /process-payment`. The frontend (`index.html`) must point to the correct path for the language being served. Do not assume all four implementations share identical URL paths.

4. **`PUBLIC_API_KEY` initializes the `globalpayments.js` tokenizer on the frontend.** The frontend calls `GET /config` (or `/config.php` for PHP) to retrieve this key at page load, then uses it to tokenize the card client-side before submitting the form. The backend never receives raw card numbers — only the `payment_token` returned by the tokenizer. `SECRET_API_KEY` is server-side only and must never be sent to the client.

## Repository Structure

### PHP (native PHP + Global Payments SDK)
- [`php/config.php`](php/config.php) — `GET /config.php`; returns `PUBLIC_API_KEY` for frontend tokenization
- [`php/process-payment.php`](php/process-payment.php) — `POST /process-payment.php`; `configureSdk()` sets up `PorticoConfig`; `generateCustomerId()`, `generatePaymentMethodId()`, `generateScheduleId()` generate UUIDs for each SDK object
- [`php/composer.json`](php/composer.json) — `globalpayments/php-sdk` ^13.1, `vlucas/phpdotenv` ^5.5

### Node.js (Express + Global Payments SDK)
- [`nodejs/server.js`](nodejs/server.js) — `GET /config` at `app.get('/config', ...)`, `POST /process-payment` at `app.post('/process-payment', ...)`; `PorticoConfig` configured at module load; `generateCustomerId`, `generatePaymentMethodId`, `generateScheduleId` as arrow functions
- [`nodejs/package.json`](nodejs/package.json) — `globalpayments-api` ^3.10.6, Express ^4.18.2

### Java (Jakarta EE servlet + Global Payments SDK)
- [`java/src/main/java/com/globalpayments/example/ProcessPaymentServlet.java`](java/src/main/java/com/globalpayments/example/ProcessPaymentServlet.java) — `@WebServlet` handles both `/config` and `/process-payment`; `init()` configures `PorticoConfig`; `doGet()` serves config; `doPost()` runs the three-step schedule creation chain
- [`java/pom.xml`](java/pom.xml) — `globalpayments-sdk` 14.2.20, Jakarta Servlet 5.0, Tomcat 10 via Cargo plugin (hardcoded port 8000)

### .NET (ASP.NET Core minimal API + Global Payments SDK)
- [`dotnet/Program.cs`](dotnet/Program.cs) — `ConfigureGlobalPaymentsSDK()` sets up `PorticoConfig`; `ConfigureEndpoints()` maps `GET /config` and delegates to `ConfigurePaymentEndpoint()`; `ConfigurePaymentEndpoint()` maps `POST /process-payment`; port read from `PORT` env var, defaults to `"8000"`
- [`dotnet/dotnet.csproj`](dotnet/dotnet.csproj) — `GlobalPayments.Api` 9.0.16, net9.0

### Shared
- [`index.html`](index.html) — root copy of the frontend form (each language dir also has its own copy)
- [`docker-compose.yml`](docker-compose.yml) — runs all four implementations; host ports 8001 (Node.js), 8003 (PHP), 8004 (Java), 8006 (.NET); all container ports are 8000

## API Surface

| Method | Path | Purpose | Languages |
|--------|------|---------|-----------|
| GET | `/config` | Returns `publicApiKey` for frontend tokenization | Node.js, Java, .NET |
| GET | `/config.php` | Same as above (file-based routing) | PHP only |
| POST | `/process-payment` | Creates customer, payment method, and schedule | Node.js, Java, .NET |
| POST | `/process-payment.php` | Same as above (file-based routing) | PHP only |

Note: PHP's file-based paths diverge from the other three. The frontend `index.html` per language dir is pre-configured to call the right path.

## Environment Variables

```bash
PUBLIC_API_KEY=pkapi_cert_...     # Portico public key — sent to frontend for globalpayments.js tokenization
SECRET_API_KEY=skapi_cert_...     # Portico secret key — server-side only, never exposed to client
PORT=8000                         # Optional; all implementations default to 8000
```

Each language reads from a `.env` file in its own directory. Copy the `.env.sample` in each language dir to `.env` and fill in credentials.

## Sandbox Credentials

| Brand | Number | CVV | Expiry |
|-------|--------|-----|--------|
| Visa | 4012002000060016 | 123 | Any future date |
| Mastercard | 5473500000000014 | 123 | Any future date |

Get sandbox credentials at [developer.globalpayments.com](https://developer.globalpayments.com).

## Architecture Summary

**Tokenization:** Page load → `GET /config[.php]` → frontend initializes `globalpayments.js` with `publicApiKey` → user enters card → `globalpayments.js` returns `payment_token`

**Schedule creation:** Form submit → `POST /process-payment[.php]` with `payment_token` + customer fields → `Customer.create()` → `addPaymentMethod(id, card).create()` → `addSchedule(id).withFrequency(Weekly)...create()` → return `scheduleKey`

## Security Notes

These demos have no authentication on the payment endpoint, hardcode the schedule dates in source, and use sandbox credentials in `.env.sample`. For production: add auth middleware, drive schedule parameters from a database or request body, use secrets management instead of `.env` files, and validate `payment_token` format server-side before passing to the SDK.

## How to Run

```bash
cd php && ./run.sh       # PHP — :8000
cd nodejs && ./run.sh    # Node.js — :8000
cd java && ./run.sh      # Java — :8000
cd dotnet && ./run.sh    # .NET — :8000
# All at once:
docker-compose up
```

## How to Verify

```bash
# Config endpoint (Node.js, Java, .NET)
curl http://localhost:8000/config
# Expected: {"success":true,"data":{"publicApiKey":"pkapi_cert_..."}}

# Config endpoint (PHP)
curl http://localhost:8000/config.php
# Expected: {"success":true,"data":{"publicApiKey":"pkapi_cert_..."}}

# Process payment — requires a valid payment_token from globalpayments.js tokenization
# The /process-payment endpoint cannot be fully tested with curl alone:
# globalpayments.js must run in a browser to produce a valid payment_token.
# With a real token from a browser session:
# curl -X POST http://localhost:8000/process-payment \
#   -d "payment_token=<token>&first_name=Jane&last_name=Doe&email=jane@example.com" \
#   -d "phone=5551234567&street_address=123+Main+St&city=Atlanta&state=GA" \
#   -d "billing_zip=30301&country=US&amount=25.00"
# Expected: {"success":true,"message":"Schedule created successfully! Schedule Key: ...","data":{"scheduleKey":"..."}}
```

The `/process-payment` endpoint requires a `payment_token` from `globalpayments.js` — this token is produced by the hosted fields iframe in the browser. It cannot be minted with curl alone.

## Making Changes

All language implementations expose identical behavior (PHP paths differ — see API Surface). A change to one must be applied to all four — each language in a separate commit. Do not modify `index.html` at the root or `docker-compose.yml` without confirming the change applies to every implementation.

## SDK Versions

- **PHP**: `globalpayments/php-sdk` ^13.1
- **Node.js**: `globalpayments-api` ^3.10.6
- **Java**: `globalpayments-sdk` (com.heartlandpaymentsystems) 14.2.20
- **.NET**: `GlobalPayments.Api` 9.0.16
