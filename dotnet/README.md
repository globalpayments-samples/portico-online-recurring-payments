# .NET — Portico Online Recurring Payments

ASP.NET Core implementation of a recurring payment setup using the Global Payments Portico gateway. Uses Heartland Hosted Fields for PCI SAQ-A compliant card tokenization — card data never touches your server.

## Requirements

- .NET 9.0+
- Global Payments Portico account with API credentials

## Project Structure

```
dotnet/
├── Program.cs          # ASP.NET Core minimal API — GET /config and POST /process-payment
├── wwwroot/
│   └── index.html      # Recurring payment form frontend (served as static file)
├── appsettings.json    # ASP.NET Core app settings
├── dotnet.csproj       # GlobalPayments.Api + dotenv.net
├── .env.sample
├── Dockerfile
├── run.sh
├── .devcontainer/
└── .codesandbox/
```

## Setup

**1. Restore dependencies**
```bash
dotnet restore
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
dotnet run
# Open http://localhost:5000
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

Configured once at startup in `Program.cs`:

```csharp
using GlobalPayments.Api;
using GlobalPayments.Api.ServiceConfigs.Gateways;
using dotenv.net;

DotEnv.Load();

var config = new PorticoConfig
{
    SecretApiKey = Environment.GetEnvironmentVariable("SECRET_API_KEY"),
    DeveloperId = "000000",
    VersionNumber = "0000",
    ServiceUrl = "https://cert.api2.heartlandportico.com"
};
ServicesContainer.Configure(config);
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

```csharp
// Step 1 — Create customer
var customer = new Customer
{
    Id = Guid.NewGuid().ToString(),
    FirstName = body.FirstName,
    LastName = body.LastName,
    Status = "Active",
    Email = body.Email,
    WorkPhone = body.Phone,
    Address = new Address
    {
        StreetAddress1 = body.StreetAddress,
        City = body.City,
        Province = body.State,
        PostalCode = SanitizePostalCode(body.BillingZip),
        Country = body.Country
    }
};
var savedCustomer = customer.Create();

// Step 2 — Store payment method
var card = new CreditCardData { Token = body.PaymentToken };
var paymentMethod = savedCustomer
    .AddPaymentMethod(Guid.NewGuid().ToString(), card)
    .Create();

// Step 3 — Create schedule
var schedule = paymentMethod.AddSchedule(Guid.NewGuid().ToString())
    .WithStatus("Active")
    .WithAmount(amount)
    .WithCurrency("USD")
    .WithStartDate(new DateTime(2027, 2, 1))
    .WithFrequency(ScheduleFrequency.Weekly)
    .WithEndDate(new DateTime(2027, 4, 1))
    .WithReprocessingCount(2)
    .Create();
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
docker build -t portico-recurring-dotnet .
docker run -p 8006:8000 \
  -e ASPNETCORE_URLS=http://+:8000 \
  -e PUBLIC_API_KEY=your_key \
  -e SECRET_API_KEY=your_key \
  portico-recurring-dotnet
# Open http://localhost:8006
```

Or via docker-compose from the project root:
```bash
docker-compose up dotnet
```

## Troubleshooting

**Hosted Fields not loading**
Verify `GET /config` returns a 200 with a valid `publicApiKey`. Confirm `.env` is present and `PUBLIC_API_KEY` is set. The SDK reads env vars at startup via `DotEnv.Load()` — restart `dotnet run` after editing `.env`.

**"Missing required fields" (400)**
All 11 fields are required. Check that the JSON body includes `payment_token`, `first_name`, `last_name`, `email`, `phone`, `street_address`, `city`, `state`, `billing_zip`, `country`, and `amount`.

**"Recurring payment schedule setup failed" — Portico error**
Confirm `SECRET_API_KEY` starts with `skapi_cert_`. Check the server console for the raw Portico exception message. The cert environment credential prefix distinguishes it from production keys.

**Static files not served (index.html 404)**
The frontend is served from `wwwroot/`. Ensure `app.UseStaticFiles()` and `app.UseDefaultFiles()` are both present in `Program.cs` and that `wwwroot/index.html` exists.

**`dotnet run` fails to resolve packages**
Requires .NET 9.0+. Confirm with `dotnet --version`. Run `dotnet restore` to pull fresh packages. If `GlobalPayments.Api` fails to resolve, clear the NuGet cache: `dotnet nuget locals all --clear`.

**Schedule created but no immediate charge**
Schedule creation does not immediately charge the customer. The first charge occurs on `start_date` (hardcoded in this example). Ensure `WithStartDate` uses a future date when testing against the cert environment.
