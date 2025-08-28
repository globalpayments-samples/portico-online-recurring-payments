# Python Recurring Payment Example

This example demonstrates recurring payment setup using Flask and the Global Payments SDK.

## Requirements

- Python 3.7 or later
- pip (Python Package Installer)
- Global Payments account and API credentials

## Project Structure

- `server.py` - Main application file containing server setup and recurring payment processing
- `index.html` - Client-side payment form with customer information collection
- `requirements.txt` - Project dependencies
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
4. Create and activate a virtual environment (recommended):
   ```bash
   python -m venv venv
   source venv/bin/activate  # On Windows, use: venv\Scripts\activate
   ```
5. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```
6. Run the application:
   ```bash
   ./run.sh
   ```
   Or manually:
   ```bash
   python server.py
   ```

## Implementation Details

### Server Setup
The application uses Flask to create a web server that:
- Serves static files including the payment form
- Creates customers and recurring payment schedules
- Provides configuration endpoint for client-side SDK
- Handles JSON responses

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
- Returns JSON responses for errors
- Includes appropriate HTTP status codes

## API Endpoints

### GET /config
Returns public API key for client-side SDK initialization.

Response:
```json
{
    "publicApiKey": "pk_test_xxx"
}
```

### POST /process-payment
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
```json
{
    "success": true,
    "message": "Schedule created successfully! Schedule Key: xxx"
}
```

Response (Error):
```json
{
    "success": false,
    "message": "Error: [error message]"
}
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
- Configuring secure session handling
- Using a production-grade WSGI server (e.g., Gunicorn, uWSGI)
- Implementing customer authentication for schedule management
- Adding webhook endpoints for payment failure notifications
- Securing stored payment method tokens with proper encryption
