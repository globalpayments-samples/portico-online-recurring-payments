# Node.js — Portico Online Recurring Payments

Node.js/Express implementation of a recurring payment setup using the Global Payments Portico gateway. Uses Heartland Hosted Fields for PCI SAQ-A compliant card tokenization — card data never touches your server.

## Requirements

- Node.js 18+
- npm
- Global Payments Portico account with API credentials

## Project Structure

```
nodejs/
├── server.js       # Express server — GET /config and POST /process-payment
├── index.html      # Recurring payment form frontend
├── package.json    # globalpayments-api + dotenv
├── .env.sample
├── Dockerfile
├── run.sh
├── .devcontainer/
└── .codesandbox/
```

## Setup

**1. Install dependencies**
```bash
npm install
```

**2. Configure credentials**
```bash
cp .env.sample .env
```

Edit `.env`:
```env
PUBLIC_API_KEY=pkapi_cert_jKc1FtuyAydZhZfbB3
SECRET_API_KEY=skapi_cert_MTyMAQBiHVEAewvIzXVFcmUd2UcyBge_eCpaASUp0A
PORT=8000
```

**3. Start the server**
```bash
npm start
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
| `PORT` | Server port | no | `8000` (default) |

## SDK Configuration

```javascript
import { ServicesContainer, PorticoConfig } from 'globalpayments-api';
import * as dotenv from 'dotenv';

dotenv.config();

const config = new PorticoConfig();
config.secretApiKey = process.env.SECRET_API_KEY;
config.serviceUrl = 'https://cert.api2.heartlandportico.com';
ServicesContainer.configureService(config);
```

## API Endpoints

### GET /config

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

### POST /process-payment

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

```javascript
import {
    Customer, CreditCardData, Address,
    ScheduleFrequency, EmailReceipt
} from 'globalpayments-api';

// Step 1 — Create customer
const customer = new Customer();
customer.id = generateCustomerId();
customer.firstName = req.body.first_name.trim();
customer.lastName = req.body.last_name.trim();
customer.status = 'Active';
customer.email = req.body.email.trim();
customer.address = new Address();
customer.address.streetAddress1 = req.body.street_address.trim();
customer.address.city = req.body.city.trim();
customer.address.province = req.body.state.trim();
customer.address.postalCode = sanitizePostalCode(req.body.billing_zip);
customer.address.country = req.body.country.trim();
customer.workPhone = req.body.phone.trim();
const createdCustomer = await customer.create();

// Step 2 — Store payment method
const card = new CreditCardData();
card.token = req.body.payment_token;
const paymentMethod = await createdCustomer
    .addPaymentMethod(generatePaymentMethodId(), card)
    .create();

// Step 3 — Create schedule
const schedule = await paymentMethod.addSchedule(generateScheduleId())
    .withStatus('Active')
    .withAmount(amount)
    .withCurrency('USD')
    .withStartDate(new Date('2027-02-01'))
    .withFrequency(ScheduleFrequency.Weekly)
    .withEndDate(new Date('2027-04-01'))
    .withReprocessingCount(2)
    .withEmailReceipt(EmailReceipt.Never)
    .create();
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
docker build -t portico-recurring-nodejs .
docker run -p 8001:8000 \
  -e PUBLIC_API_KEY=your_key \
  -e SECRET_API_KEY=your_key \
  portico-recurring-nodejs
# Open http://localhost:8001
```

Or via docker-compose from the project root:
```bash
docker-compose up nodejs
```

## Troubleshooting

**Hosted Fields not loading**
Verify `GET /config` returns a 200 with a valid `publicApiKey`. Check the browser console for Heartland.js initialization errors. Ensure `PUBLIC_API_KEY` is set in `.env` and the server was restarted after editing it.

**"Missing required field: X" (400)**
All 11 fields are required — `payment_token`, `first_name`, `last_name`, `email`, `phone`, `street_address`, `city`, `state`, `billing_zip`, `country`, `amount`. Confirm all fields are included as non-empty strings.

**"Recurring payment schedule setup failed" — Portico error**
Confirm `SECRET_API_KEY` in `.env` starts with `skapi_cert_`. `process.env.SECRET_API_KEY` is read directly — trim any trailing whitespace before setting the value. Check the server console for the full Portico error.

**`import` syntax error on startup**
The project uses ES module syntax. Confirm `"type": "module"` is in `package.json` and you are running Node.js 18+. Check with `node --version`.

**Schedule created but no immediate charge**
Schedule creation does not immediately charge the customer. The first charge occurs on `start_date` (hardcoded to `2027-02-01` in this example). Ensure this date is in the future when testing.

**"Invalid amount" (400)**
`amount` must parse to a positive number. Empty string or `"0"` will fail before the Portico API is called.
