# Global Payments Recurring Payments Examples

This repository demonstrates how to implement recurring payment functionality using the Global Payments SDK across multiple programming languages. Each implementation shows how to set up customers, payment methods, and recurring payment schedules.

## Available Implementations

- [.NET Core](./dotnet/) - ASP.NET Core recurring payment setup
- [Java](./java/) - Jakarta EE servlet-based recurring payments
- [Node.js](./nodejs/) - Express.js recurring payment scheduling
- [PHP](./php/) - PHP recurring payment implementation

## Features

- **Customer Management** - Create and manage customer records
- **Payment Method Storage** - Securely tokenize and store payment methods
- **Recurring Schedules** - Set up weekly, monthly, or custom payment schedules
- **Error Handling** - Comprehensive error handling for payment failures
- **Client Integration** - HTML form with hosted fields tokenization
- **Multiple Languages** - Consistent API structure across all implementations

## Implementation Details

Each implementation includes:

1. **SDK Setup**
   - Environment variable configuration  
   - Service URL configuration
   - API key management

2. **Core Endpoints**
   - GET `/config` - Returns public API key for client-side tokenization
   - POST `/process-payment` - Creates customer, payment method, and recurring schedule

3. **Recurring Payment Flow**
   - Customer record creation with billing information
   - Secure payment method storage using tokenized card data
   - Recurring payment schedule configuration with frequency and duration

## Quick Start

1. **Choose your language** - Navigate to any implementation directory (nodejs, php, java, dotnet)
2. **Set up credentials** - Copy `.env.sample` to `.env` and add your Global Payments API keys
3. **Run the server** - Execute `./run.sh` to install dependencies and start the server
4. **Test recurring payments** - Open http://localhost:8000 and complete the payment form
5. **View results** - Check the server response for the created schedule key

## Recurring Payment Use Cases

This implementation demonstrates recurring payment scenarios:

- **Subscription Services** - Monthly/yearly service subscriptions
- **Installment Plans** - Breaking large purchases into smaller payments
- **Membership Fees** - Regular membership or service fees
- **Utility Billing** - Regular utility or service billing
- **Donation Programs** - Recurring charitable donations
- **Gym Memberships** - Fitness center recurring billing

## Prerequisites

- Global Payments account with API credentials
- Development environment for your chosen language
- Package manager (npm, composer, maven, dotnet)

## Customization Guide

### Modifying Recurring Schedules

You can customize the recurring payment schedule in the `/process-payment` endpoint:

```javascript
// Weekly schedule (current example)
.withFrequency(ScheduleFrequency.Weekly)
.withStartDate(new Date('2027-02-01'))
.withEndDate(new Date('2027-04-01'))

// Monthly subscription
.withFrequency(ScheduleFrequency.Monthly)
.withStartDate(new Date())
.withEndDate(new Date(Date.now() + 365 * 24 * 60 * 60 * 1000)) // 1 year

// Semi-annual billing
.withFrequency(ScheduleFrequency.SemiAnnually)
```

### Adding Schedule Management

Extend the implementation with additional endpoints for:
1. Retrieving schedule details
2. Modifying existing schedules  
3. Canceling or pausing schedules
4. Processing schedule payments manually

### Production Considerations

For production recurring payment systems, enhance with:
- Input validation and sanitization
- Comprehensive error handling and logging
- Security headers and rate limiting
- PCI compliance measures for stored payment methods
- Retry logic for failed recurring payments
- Customer notification systems for payment failures
- Schedule modification and cancellation features
- Monitoring and alerting for payment processing
- Webhook handling for payment status updates
