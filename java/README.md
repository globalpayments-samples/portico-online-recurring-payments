# Java — Portico Online Recurring Payments

Jakarta EE/Servlet implementation of a recurring payment setup using the Global Payments Portico gateway. Uses Heartland Hosted Fields for PCI SAQ-A compliant card tokenization — card data never touches your server.

## Requirements

- Java 17+
- Maven 3.8+
- Global Payments Portico account with API credentials

## Project Structure

```
java/
├── src/
│   └── main/
│       ├── java/com/globalpayments/example/
│       │   └── ProcessPaymentServlet.java  # Handles GET /config and POST /process-payment
│       └── webapp/
│           ├── index.html                  # Recurring payment form frontend
│           └── WEB-INF/web.xml             # Servlet configuration
├── pom.xml         # com.globalpayments:java-sdk dependency
├── .env.sample
├── Dockerfile
├── run.sh
├── .devcontainer/
└── .codesandbox/
```

## Setup

**1. Build the project**
```bash
mvn clean package
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
mvn cargo:run
# Open http://localhost:8080
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

Export credentials before running:
```bash
export SECRET_API_KEY=skapi_cert_...
export PUBLIC_API_KEY=pkapi_cert_...
mvn cargo:run
```

## SDK Configuration

Configured at servlet initialization:

```java
import com.global.api.ServicesContainer;
import com.global.api.serviceConfigs.PorticoConfig;

PorticoConfig config = new PorticoConfig();
config.setSecretApiKey(System.getenv("SECRET_API_KEY"));
config.setDeveloperId("000000");
config.setVersionNumber("0000");
config.setServiceUrl("https://cert.api2.heartlandportico.com");

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

```java
// Step 1 — Create customer
Customer customer = new Customer();
customer.setId(UUID.randomUUID().toString());
customer.setFirstName(firstName);
customer.setLastName(lastName);
customer.setStatus("Active");
customer.setEmail(email);
customer.setWorkPhone(phone);

Address address = new Address();
address.setStreetAddress1(streetAddress);
address.setCity(city);
address.setProvince(state);
address.setPostalCode(sanitizePostalCode(billingZip));
address.setCountry(country);
customer.setAddress(address);

Customer savedCustomer = customer.create();

// Step 2 — Store payment method
CreditCardData card = new CreditCardData();
card.setToken(paymentToken);
RecurringPaymentMethod savedMethod = savedCustomer
    .addPaymentMethod(UUID.randomUUID().toString(), card)
    .create();

// Step 3 — Create schedule
Schedule savedSchedule = savedMethod.addSchedule(UUID.randomUUID().toString())
    .withStatus("Active")
    .withAmount(new BigDecimal(amount))
    .withCurrency("USD")
    .withStartDate(Date.from(LocalDate.of(2027, 2, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()))
    .withFrequency(ScheduleFrequency.Weekly)
    .withEndDate(Date.from(LocalDate.of(2027, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()))
    .withReprocessingCount(2)
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
docker build -t portico-recurring-java .
docker run -p 8004:8000 \
  -e PUBLIC_API_KEY=your_key \
  -e SECRET_API_KEY=your_key \
  portico-recurring-java
# Open http://localhost:8004
```

Or via docker-compose from the project root:
```bash
docker-compose up java
```

## Troubleshooting

**Hosted Fields not loading**
Verify `GET /config` returns a 200 with a valid `publicApiKey`. If the servlet fails at startup, check that `PUBLIC_API_KEY` and `SECRET_API_KEY` are exported as shell environment variables before running `mvn cargo:run`.

**"Missing required fields" (400)**
All 11 fields are required. Confirm the JSON body includes `payment_token`, `first_name`, `last_name`, `email`, `phone`, `street_address`, `city`, `state`, `billing_zip`, `country`, and `amount`.

**"Recurring payment schedule setup failed" — Portico error**
Confirm `SECRET_API_KEY` starts with `skapi_cert_`. Environment variables must be exported in the shell before running `mvn cargo:run`:
```bash
export SECRET_API_KEY=skapi_cert_...
export PUBLIC_API_KEY=pkapi_cert_...
mvn cargo:run
```

**Maven build fails**
Requires Java 17+ and Maven 3.8+. Confirm with `java -version` and `mvn -v`. If the `com.globalpayments:java-sdk` dependency fails to resolve, run `mvn clean package -U` to force a fresh dependency download.

**Port conflict on 8080**
The Java implementation defaults to port 8080. If 8080 is occupied, update the `cargo` plugin port in `pom.xml` or stop the conflicting process with `lsof -i :8080`.

**Schedule created but no immediate charge**
Schedule creation does not immediately charge the customer. The first charge occurs on `start_date` (hardcoded in this example). Ensure `withStartDate` uses a future date when testing against the cert environment.
