# PHP Recurring Payment Example

This example demonstrates recurring payment setup using PHP and the Global Payments SDK.

## Requirements

- PHP 7.4 or later
- Composer
- Global Payments account and API credentials

## Project Structure

- `process-payment.php` - Recurring payment processing script
- `index.php` - Client-side payment form with customer information collection
- `composer.json` - Project dependencies
- `.env.sample` - Template for environment variables
- `run.sh` - Convenience script to run the application

## Setup

1. Clone this repository
2. Copy `.env.sample` to `.env`
3. Update `.env` with your Global Payments credentials:
   ```
   PUBLIC_API_KEY=pk_test_xxx
   SECRET_API_KEY=sk_test_xxx
   ```
4. Install dependencies:
   ```bash
   composer install
   ```
5. Run the application:
   ```bash
   ./run.sh
   ```
   Or manually:
   ```bash
   php -S localhost:8000
   ```

## Implementation Details

### Application Structure
The application uses a simple PHP structure:
- Static HTML form for customer and payment information collection
- Separate PHP script for recurring payment processing
- Composer for dependency management

### SDK Configuration
Global Payments SDK configuration using environment variables:
- Loads credentials from .env file
- Sets up service URL for API communication
- Configures developer identification

### Recurring Payment Setup
Recurring payment setup flow:
1. Client submits payment token and complete customer information
2. Server creates Customer record with billing details
3. Creates CreditCardData with tokenized payment information
4. Adds payment method to customer account
5. Creates recurring payment schedule with specified frequency and duration
6. Returns success response with schedule key

### Error Handling
Implements comprehensive error handling:
- Catches and processes API exceptions
- Returns appropriate error messages
- Handles edge cases gracefully

## API Endpoints

### POST /process-payment.php
Creates a recurring payment schedule using customer information and tokenized payment method.

Request Parameters:
- `payment_token` (string, required) - Token from client-side SDK
- `first_name` (string, required) - Customer's first name
- `last_name` (string, required) - Customer's last name
- `email` (string, required) - Customer's email address
- `phone` (string, required) - Customer's phone number
- `street_address` (string, required) - Customer's street address
- `city` (string, required) - Customer's city
- `state` (string, required) - Customer's state/province
- `billing_zip` (string, required) - Billing postal code
- `country` (string, required) - Customer's country
- `amount` (string, required) - Recurring payment amount

Response (Success):
```
Schedule created successfully! Schedule Key: xxx
```

Response (Error):
```
Error: [error message]
```

## Security Considerations

This example demonstrates basic recurring payment implementation. For production use, consider:
- Implementing additional input validation for customer data
- Adding request rate limiting to prevent abuse
- Including security headers for web security
- Implementing proper logging and monitoring for recurring payments
- Adding payment fraud prevention measures
- Using HTTPS in production for secure data transmission
- Implementing CSRF protection for form submissions
- Configuring proper session handling
- Setting appropriate PHP security directives
- Implementing customer authentication for schedule management
- Adding webhook endpoints for payment failure notifications
- Securing stored payment method tokens with proper encryption
