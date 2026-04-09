# PHP — Portico Online Recurring Payments

PHP implementation of a recurring payment setup using the Global Payments Portico gateway. Uses Heartland Hosted Fields for PCI SAQ-A compliant card tokenization — card data never touches your server.

## Requirements

- PHP 8.0+
- Composer
- Global Payments Portico account with API credentials

## Project Structure

```
php/
├── config.php           # GET /config.php — returns publicApiKey
├── process-payment.php  # POST /process-payment.php — creates Customer → PaymentMethod → Schedule
├── index.html           # Recurring payment form frontend
├── composer.json        # globalpayments/php-sdk + phpdotenv
├── .env.sample
├── Dockerfile
├── run.sh
├── .devcontainer/
└── .codesandbox/
```

## Setup

**1. Install dependencies**
```bash
composer install
```

**2. Configure credentials**
```bash
cp .env.sample .env
```

Edit `.env`:
```env
PUBLIC_API_KEY=pkapi_cert_jKc1FtuyAydZhZfbB3
SECRET_API_KEY=skapi_cert_MTyMAQBiHVEAewvIzXVFcmUd2UcyBge_eCpaASUp0A
```

**3. Start the server**
```bash
php -S localhost:8000
# Open http://localhost:8000
```

Or use the convenience script:
```bash
./run.sh
```

## Environment Variables

| Variable | Description | Required | Example |
|----------|-------------|----------|---------|
| `PUBLIC_API_KEY` | Public key for Heartland Hosted Fields (browser) | yes | `pkapi_cert_jKc1FtuyAydZhZfbB3` |
| `SECRET_API_KEY` | Secret key for server-side Portico API calls | yes | `skapi_cert_MTyMAQBiHVEA...` |

## SDK Configuration

```php
use GlobalPayments\Api\ServiceConfigs\Gateways\PorticoConfig;
use GlobalPayments\Api\ServicesContainer;

$config = new PorticoConfig();
$config->secretApiKey = $_ENV['SECRET_API_KEY'];
$config->developerId = '000000';
$config->versionNumber = '0000';
$config->serviceUrl = 'https://cert.api2.heartlandportico.com';

ServicesContainer::configureService($config);
```

## API Endpoints

### GET /config.php

Returns the public API key for Heartland Hosted Fields initialization.

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

### POST /process-payment.php

Creates a Customer, attaches a tokenized payment method, and creates a recurring schedule.

**Request fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `payment_token` | string | yes | Token from Heartland Hosted Fields |
| `first_name` | string | yes | Customer first name |
| `last_name` | string | yes | Customer last name |
| `email` | string | yes | Customer email address |
| `phone` | string | yes | Customer phone number |
| `street_address` | string | yes | Billing street address |
| `city` | string | yes | Billing city |
| `state` | string | yes | Billing state/province |
| `billing_zip` | string | yes | Billing postal code |
| `country` | string | yes | Billing country |
| `amount` | string | yes | Recurring payment amount |

**Example request:**
```json
{
  "payment_token": "supt_xxxxxxxxxxxxxx",
  "first_name": "Jane",
  "last_name": "Doe",
  "email": "jane@example.com",
  "phone": "555-555-5555",
  "street_address": "123 Main St",
  "city": "Anytown",
  "state": "GA",
  "billing_zip": "12345",
  "country": "US",
  "amount": "25.00"
}
```

**Success response (200):**
```json
{
  "success": true,
  "message": "Schedule created successfully! Schedule Key: <scheduleKey>",
  "data": {
    "scheduleKey": "<scheduleKey>"
  }
}
```

**Error response (400):**
```json
{
  "success": false,
  "message": "Recurring payment schedule setup failed",
  "error": {
    "code": "API_ERROR",
    "details": "Error message details"
  }
}
```

## Recurring Payment Flow

```php
// Step 1 — Create customer
$customer = new Customer();
$customer->id = generateCustomerId();
$customer->firstName = trim($_POST['first_name']);
$customer->lastName = trim($_POST['last_name']);
$customer->status = 'Active';
$customer->email = trim($_POST['email']);
$customer->address = new Address();
$customer->address->streetAddress1 = trim($_POST['street_address']);
// ... city, province, postalCode, country, workPhone ...
$customer = $customer->create();

// Step 2 — Store payment method
$card = new CreditCardData();
$card->token = $_POST['payment_token'];
$paymentMethod = $customer->addPaymentMethod(generatePaymentMethodId(), $card)->create();

// Step 3 — Create schedule
$schedule = $paymentMethod->addSchedule(generateScheduleId())
    ->withStatus('Active')
    ->withAmount($amount)
    ->withCurrency('USD')
    ->withStartDate(\DateTime::createFromFormat('Y-m-d', '2027-02-01'))
    ->withFrequency(ScheduleFrequency::WEEKLY)
    ->withEndDate(\DateTime::createFromFormat('Y-m-d', '2027-04-01'))
    ->withReprocessingCount(2)
    ->create();
```

## Test Cards

| Brand | Card Number | CVV | Expiry |
|-------|-------------|-----|--------|
| Visa | 4012002000060016 | 123 | Any future date |
| Mastercard | 5473500000000014 | 123 | Any future date |
| Discover | 6011000990156527 | 123 | Any future date |
| Amex | 372700699251018 | 1234 | Any future date |

## Docker

```bash
docker build -t portico-recurring-php .
docker run -p 8003:8000 \
  -e PUBLIC_API_KEY=your_key \
  -e SECRET_API_KEY=your_key \
  portico-recurring-php
# Open http://localhost:8003
```

Or via docker-compose from the project root:
```bash
docker-compose up php
```

## Troubleshooting

**Hosted Fields not loading**
Verify `GET /config.php` returns a 200 with a valid `publicApiKey`. If it errors, confirm `.env` exists and `composer install` completed. Restart `php -S` after editing `.env`.

**"Missing required field: X" (400)**
All 11 fields are required — `payment_token`, `first_name`, `last_name`, `email`, `phone`, `street_address`, `city`, `state`, `billing_zip`, `country`, `amount`. Verify the form is sending all fields in the POST body.

**"Recurring payment schedule setup failed" — Portico error**
Confirm `SECRET_API_KEY` starts with `skapi_cert_`. The cert service URL (`cert.api2.heartlandportico.com`) must be reachable from your environment. Check PHP error logs for the raw Portico exception message.

**Schedule created but no immediate charge**
Schedule creation does not immediately charge the customer. The first charge occurs on `start_date` (hardcoded to `2027-02-01` in this example). Use this date in the future when testing.

**`composer install` fails**
Requires PHP 8.0+ and Composer 2.x. Confirm with `php -v` and `composer --version`. Missing `ext-curl` or `ext-json` will cause install failures — install via your OS package manager (e.g., `apt install php-curl php-json`).

**"Invalid amount" (400)**
`amount` must be a positive numeric value. Empty string or zero will fail validation before reaching the Portico API.
