# Portico Recurring Payments — Multi-Language Examples

Complete implementation of recurring payment schedules using the Global Payments Portico gateway across 4 programming languages. Each implementation demonstrates how to create a customer record, store a tokenized payment method, and configure a recurring billing schedule using the official Global Payments SDK.

## Available Implementations

| Language | Framework | SDK | Port | Preview |
|----------|-----------|-----|------|---------|
| [**PHP**](./php/) | Built-in Server | globalpayments/php-sdk | 8003 | [Open in CodeSandbox](https://githubbox.com/globalpayments-samples/portico-online-recurring-payments/tree/main/php) |
| [**Node.js**](./nodejs/) | Express.js | globalpayments-api | 8001 | [Open in CodeSandbox](https://githubbox.com/globalpayments-samples/portico-online-recurring-payments/tree/main/nodejs) |
| [**.NET**](./dotnet/) | ASP.NET Core | GlobalPayments.Api | 8006 | [Open in CodeSandbox](https://githubbox.com/globalpayments-samples/portico-online-recurring-payments/tree/main/dotnet) |
| [**Java**](./java/) | Jakarta Servlet | com.globalpayments:java-sdk | 8004 | [Open in CodeSandbox](https://githubbox.com/globalpayments-samples/portico-online-recurring-payments/tree/main/java) |

## How It Works

```
Browser                     Backend                        Portico API
   │                            │                               │
   │── GET /config ────────────>│                               │
   │<─ { publicApiKey } ────────│                               │
   │                            │                               │
   │  [User fills form]         │                               │
   │  [Heartland.js tokenizes]  │                               │
   │                            │                               │
   │── POST /process-payment ──>│                               │
   │   payment_token            │── Customer.create() ─────────>│
   │   customer info            │<─ customer record ────────────│
   │   amount                   │                               │
   │                            │── customer.addPaymentMethod()─>│
   │                            │<─ paymentMethod record ───────│
   │                            │                               │
   │                            │── paymentMethod.addSchedule()─>│
   │                            │<─ { scheduleKey } ────────────│
   │                            │                               │
   │<─ { success, scheduleKey } │                               │
```

## Recurring Payment Use Cases

| Scenario | Frequency | Example |
|----------|-----------|---------|
| Subscription service | Monthly | SaaS platform billing |
| Installment plan | Weekly | Buy-now-pay-later split |
| Membership fee | Annually | Annual club renewal |
| Utility billing | Monthly | Regular service billing |
| Charitable giving | Weekly | Regular donation program |
| Gym membership | Monthly | Fitness center auto-pay |

## Prerequisites

- Global Payments Portico developer account
- Portico API credentials (`PUBLIC_API_KEY` and `SECRET_API_KEY`)
- Docker, or runtime for your chosen language (PHP 8.0+, Node.js 18+, .NET 8+, Java 17+)

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/globalpayments-samples/portico-online-recurring-payments.git
cd portico-online-recurring-payments
```

### 2. Choose a Language and Configure Credentials

```bash
cd php   # or nodejs, dotnet, java
cp .env.sample .env
```

Edit `.env`:

```env
PUBLIC_API_KEY=pkapi_cert_your_key_here
SECRET_API_KEY=skapi_cert_your_key_here
```

### 3. Install, Build, and Run

**PHP:**
```bash
composer install
php -S localhost:8000
# Open http://localhost:8000
```

**Node.js:**
```bash
npm install
npm start
# Open http://localhost:8000
```

**.NET:**
```bash
dotnet restore
dotnet run
# Open http://localhost:5000
```

**Java:**
```bash
mvn clean package
mvn cargo:run
# Open http://localhost:8080
```

### 4. Test a Recurring Payment

1. Open the app in your browser
2. Enter a billing amount (e.g., `19.99`)
3. Fill in customer info (name, email, address)
4. Enter a test card number (see [Test Cards](#test-cards) below)
5. Submit the form
6. Verify the response includes a `scheduleKey`

## Docker Setup

Run all four language implementations simultaneously:

```bash
# Copy root .env first
cp php/.env.sample .env   # or any language — all use same variables

docker-compose up
```

| Service | External Port | URL |
|---------|--------------|-----|
| nodejs  | 8001 | http://localhost:8001 |
| php     | 8003 | http://localhost:8003 |
| java    | 8004 | http://localhost:8004 |
| dotnet  | 8006 | http://localhost:8006 |

Run a single service:

```bash
docker-compose up php
docker-compose up nodejs
docker-compose up dotnet
docker-compose up java
```

## API Endpoints

### GET /config

Returns the public API key for Heartland.js tokenization.

**Response:**
```json
{
  "success": true,
  "data": {
    "publicApiKey": "pkapi_cert_jKc1FtuyAydZhZfbB3"
  }
}
```

---

### POST /process-payment

Creates a customer record, stores a tokenized payment method, and sets up a recurring payment schedule.

**Request body** (`application/x-www-form-urlencoded` or `application/json`):

```json
{
  "payment_token": "supt_xxxxxxxxxxxxxx",
  "first_name": "Jane",
  "last_name": "Smith",
  "email": "jane.smith@example.com",
  "phone": "5551234567",
  "street_address": "123 Main St",
  "city": "Atlanta",
  "state": "GA",
  "billing_zip": "30301",
  "country": "US",
  "amount": "19.99"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `payment_token` | string | ✅ | Tokenized card from Heartland.js |
| `first_name` | string | ✅ | Customer first name |
| `last_name` | string | ✅ | Customer last name |
| `email` | string | ✅ | Customer email address |
| `phone` | string | ✅ | Customer phone number |
| `street_address` | string | ✅ | Billing street address |
| `city` | string | ✅ | Billing city |
| `state` | string | ✅ | Billing state / province |
| `billing_zip` | string | ✅ | Billing postal code |
| `country` | string | ✅ | Billing country code (e.g. `US`) |
| `amount` | string | ✅ | Recurring charge amount (e.g. `19.99`) |

**Response (success):**
```json
{
  "success": true,
  "message": "Schedule created successfully! Schedule Key: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "data": {
    "scheduleKey": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
  }
}
```

**Response (error):**
```json
{
  "success": false,
  "message": "Recurring payment schedule setup failed",
  "error": {
    "code": "API_ERROR",
    "details": "Specific error message from the API"
  }
}
```

## Recurring Payment Flow

The server-side implementation follows this three-step sequence on every call to `/process-payment`:

**Step 1 — Create customer**
```
Customer record created with billing address and contact info.
Returns a customer ID used in subsequent calls.
```

**Step 2 — Store payment method**
```
Tokenized card attached to the customer as a saved payment method.
Returns a payment method ID.
```

**Step 3 — Create schedule**
```
Schedule configured with:
  - Frequency: Weekly
  - Start date: 2027-02-01
  - End date:   2027-04-01
  - Currency:   USD
  - Reprocessing attempts: 2

Returns the scheduleKey identifying the active recurring billing agreement.
```

## SDK Configuration

All implementations use `PorticoConfig` with the `secretApiKey`:

**PHP:**
```php
$config = new PorticoConfig();
$config->secretApiKey = $_ENV['SECRET_API_KEY'];
$config->developerId = '000000';
$config->versionNumber = '0000';
$config->serviceUrl = 'https://cert.api2.heartlandportico.com';

ServicesContainer::configureService($config);
```

**Node.js:**
```javascript
const config = new PorticoConfig();
config.secretApiKey = process.env.SECRET_API_KEY;
ServicesContainer.configure(config);
```

**.NET:**
```csharp
var config = new PorticoConfig {
    SecretApiKey = Environment.GetEnvironmentVariable("SECRET_API_KEY")
};
ServicesContainer.Configure(config);
```

**Java:**
```java
PorticoConfig config = new PorticoConfig();
config.setSecretApiKey(System.getenv("SECRET_API_KEY"));
ServicesContainer.configure(config);
```

## Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `PUBLIC_API_KEY` | Public key for Heartland.js client-side tokenization | `pkapi_cert_jKc1FtuyAydZhZfbB3` |
| `SECRET_API_KEY` | Secret key for server-side Portico API calls | `skapi_cert_MTyMAQBiHVEAe...` |

Obtain credentials from your [Global Payments developer account](https://developer.globalpay.com/).

## Test Cards

Use these card numbers in the Portico certification environment:

| Brand | Card Number | CVV | Expiry |
|-------|-------------|-----|--------|
| Visa | 4012002000060016 | 123 | Any future date |
| Mastercard | 5473500000000014 | 123 | Any future date |
| Discover | 6011000990156527 | 123 | Any future date |
| Amex | 372700699251018 | 1234 | Any future date |

Additional test cards: [developer.globalpay.com/resources/test-cards](https://developer.globalpay.com/resources/test-cards)

## Project Structure

```
portico-online-recurring-payments/
├── index.html              # Shared frontend (hosted fields form + customer info)
├── docker-compose.yml      # Multi-service config (all 4 languages)
├── README.md               # This file
├── LICENSE
├── php/                    # PHP implementation (Docker: 8003)
│   ├── config.php          # GET /config endpoint
│   ├── process-payment.php # POST /process-payment endpoint
│   ├── composer.json
│   ├── .env.sample
│   ├── Dockerfile
│   ├── run.sh
│   ├── .devcontainer/
│   ├── .codesandbox/
│   └── README.md
├── nodejs/                 # Node.js implementation (Docker: 8001)
│   ├── server.js           # Express server with both endpoints
│   ├── package.json
│   ├── .env.sample
│   ├── Dockerfile
│   ├── run.sh
│   ├── .devcontainer/
│   ├── .codesandbox/
│   └── README.md
├── dotnet/                 # .NET implementation (Docker: 8006)
│   ├── Program.cs          # ASP.NET Core minimal API
│   ├── dotnet.csproj
│   ├── .env.sample
│   ├── Dockerfile
│   ├── run.sh
│   ├── .devcontainer/
│   ├── .codesandbox/
│   └── README.md
└── java/                   # Java implementation (Docker: 8004)
    ├── src/
    ├── pom.xml
    ├── .env.sample
    ├── Dockerfile
    ├── run.sh
    ├── .devcontainer/
    ├── .codesandbox/
    └── README.md
```

## Troubleshooting

**Schedule creation fails with "Invalid customer"**
The customer creation step must complete before the payment method or schedule can be created. Verify all required customer fields (`first_name`, `last_name`, `email`, `phone`, and full billing address) are present and non-empty in the request.

**"Authentication failed" or 401 error**
Your `SECRET_API_KEY` in `.env` is missing or incorrect. Confirm the key starts with `skapi_cert_` for the certification environment. Check for trailing whitespace or newline characters in the `.env` file.

**Heartland.js tokenization not working**
The `PUBLIC_API_KEY` returned from `GET /config` is used to initialize Heartland.js. If the key is wrong or the `/config` endpoint returns an error, the hosted fields won't load. Open browser DevTools → Console to see the specific Heartland.js error.

**Port already in use**
Each language runs on a different Docker port. If running locally (not Docker), only one service can use a given port at a time. Stop other processes on port 8000 or specify an alternate port in your run command.

**Composer install fails (PHP)**
Requires PHP 8.0+ and Composer 2.x. Run `php -v` and `composer --version` to confirm. If the `ext-curl` or `ext-json` extensions are missing, install them via your system package manager.

**Maven build fails (Java)**
Requires Java 17+ and Maven 3.8+. Run `java -version` and `mvn -v` to confirm. If the build fails on dependency resolution, check your network connection and try `mvn clean package -U` to force dependency updates.

## Per-Language Documentation

Each implementation has its own detailed README:

- [PHP README](./php/README.md)
- [Node.js README](./nodejs/README.md)
- [.NET README](./dotnet/README.md)
- [Java README](./java/README.md)

## External Resources

- [Global Payments Developer Portal](https://developer.globalpay.com/)
- [Portico API Reference](https://developer.globalpay.com/api/hosted-fields)
- [Hosted Fields / Heartland.js Guide](https://developer.globalpay.com/docs/payments/online/hosted-fields)
- [Test Cards](https://developer.globalpay.com/resources/test-cards)

## License

[MIT](./LICENSE)
