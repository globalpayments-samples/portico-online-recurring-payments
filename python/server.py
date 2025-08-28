"""
Card Payment Processing Server

This Flask application demonstrates card payment processing using the Global Payments SDK.
It provides endpoints for configuration and payment processing, handling tokenized card data
to ensure secure payment processing.

The server provides two main endpoints:
- /config: Returns the public API key for client-side tokenization
- /process-payment: Processes card payments using tokenized data

Author: Global Payments
License: MIT
"""

import os
import re
import uuid
from datetime import datetime, date
from flask import Flask, request, jsonify
from dotenv import load_dotenv
from globalpayments.api import PorticoConfig, ServicesContainer
from globalpayments.api.payment_methods import CreditCardData
from globalpayments.api.entities import Address, Customer
from globalpayments.api.entities.enums import ScheduleFrequency
from globalpayments.api.entities.exceptions import ApiException

# Initialize application
app = Flask(__name__, static_folder='.')

def configure_sdk():
    """
    Configure the Global Payments SDK with necessary credentials and settings.
    This must be called before processing any payments.
    """
    config = PorticoConfig()
    # Set secret API key for server-side operations
    config.secret_api_key = os.getenv('SECRET_API_KEY')
    # Set API endpoint URL - using certification environment
    config.service_url = 'https://cert.api2.heartlandportico.com'
    # Developer identification used by Global Payments
    config.developer_id = '000000'
    config.version_number = '0000'
    
    ServicesContainer.configure(config)

# Configure SDK on startup
configure_sdk()

def sanitize_postal_code(postal_code: str) -> str:
    """
    Sanitize postal code input by removing invalid characters.
    
    Args:
        postal_code (str): The postal code to sanitize.
            Can be a US format (12345 or 12345-6789) or international format.
    
    Returns:
        str: The sanitized postal code, containing only alphanumeric characters
            and hyphens, limited to 10 characters.
    """
    sanitized = re.sub(r'[^a-zA-Z0-9-]', '', postal_code or '')
    return sanitized[:10]

def generate_uuid_v4() -> str:
    """
    Generate a UUID v4 formatted string.
    
    Returns:
        str: A UUID v4 formatted string.
    """
    return str(uuid.uuid4())

def generate_customer_id() -> str:
    """
    Generate a unique customer ID using UUID v4 format.
    
    Returns:
        str: A UUID v4 formatted string for customer identification.
    """
    return generate_uuid_v4()

def generate_schedule_id() -> str:
    """
    Generate a unique schedule ID using UUID v4 format.
    
    Returns:
        str: A UUID v4 formatted string for schedule identification.
    """
    return generate_uuid_v4()

def generate_payment_method_id() -> str:
    """
    Generate a unique payment method ID using UUID v4 format.
    
    Returns:
        str: A UUID v4 formatted string for payment method identification.
    """
    return generate_uuid_v4()

@app.route('/')
def index():
    """Serve the main payment form HTML page."""
    return app.send_static_file('index.html')

@app.route('/config')
def get_config():
    """
    Provide the public API key for client-side tokenization.
    This key is used by the frontend to tokenize card data securely.
    
    Returns:
        JSON response containing the public API key.
    """
    return jsonify({
        'success': True,
        'data': {
            'publicApiKey': os.getenv('PUBLIC_API_KEY')
        }
    })

@app.route('/process-payment', methods=['POST'])
def process_payment():
    """
    Process recurring payment setup using tokenized card data.
    
    Expected form data:
        payment_token (str): Token representing the card data
        first_name, last_name, email, phone: Customer information
        street_address, city, state, billing_zip, country: Address information
        amount (str): Payment amount
    
    Returns:
        JSON response with schedule result or error message
    """
    try:
        # Validate required fields
        required_fields = [
            'payment_token', 'first_name', 'last_name', 'email', 'phone',
            'street_address', 'city', 'state', 'billing_zip', 'country', 'amount'
        ]
        
        for field in required_fields:
            if field not in request.form or not request.form[field].strip():
                raise ApiException(f'Missing required field: {field}')
        
        # Parse and validate amount
        try:
            amount = float(request.form['amount'])
            if amount <= 0:
                raise ValueError('Amount must be positive')
        except (ValueError, TypeError):
            raise ApiException('Invalid amount')

        # Create customer record with form data
        customer = Customer()
        customer.id = generate_customer_id()
        customer.first_name = request.form['first_name'].strip()
        customer.last_name = request.form['last_name'].strip()
        customer.status = 'Active'
        customer.email = request.form['email'].strip()
        customer.address = Address()
        customer.address.street_address_1 = request.form['street_address'].strip()
        customer.address.city = request.form['city'].strip()
        customer.address.province = request.form['state'].strip()
        customer.address.postal_code = sanitize_postal_code(request.form['billing_zip'])
        customer.address.country = request.form['country'].strip()
        customer.work_phone = request.form['phone'].strip()
        customer = customer.create()

        # Create payment method using tokenized card information
        card = CreditCardData()
        card.token = request.form['payment_token']

        payment_method = customer.add_payment_method(
            generate_payment_method_id(),
            card
        )
        payment_method.name_on_account = request.form['first_name'].strip() + ' ' + request.form['last_name'].strip()
        payment_method = payment_method.create()

        # Create payment schedule
        schedule = payment_method.add_schedule(
            generate_schedule_id()
        ).with_status('Active')\
         .with_amount(amount)\
         .with_currency('USD')\
         .with_start_date(date(2027, 2, 1))\
         .with_frequency(ScheduleFrequency.Weekly)\
         .with_end_date(date(2027, 4, 1))\
         .with_reprocessing_count(2)\
         .create()

        # Return success response with schedule key
        return jsonify({
            'success': True,
            'message': f'Schedule created successfully! Schedule Key: {schedule.key}',
            'data': {
                'scheduleKey': schedule.key
            }
        })
    except ApiException as e:
        # Handle API-specific exceptions and return error response
        return jsonify({
            'success': False,
            'message': 'Recurring payment schedule setup failed',
            'error': {
                'code': 'API_ERROR',
                'details': str(e)
            }
        }), 400
    except Exception as e:
        # Handle general errors
        return jsonify({
            'success': False,
            'message': 'Internal server error',
            'error': {
                'code': 'SERVER_ERROR',
                'details': str(e)
            }
        }), 500

# Start the server if this file is run directly
if __name__ == '__main__':
    port = int(os.getenv('PORT', 8000))
    app.run(host='0.0.0.0', port=port, debug=True)  # Running in debug mode for development
