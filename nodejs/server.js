/**
 * Card Payment Processing Server
 * 
 * This Express application demonstrates card payment processing using the Global Payments SDK.
 * It provides endpoints for configuration and payment processing, handling tokenized card data
 * to ensure secure payment processing.
 */

import express from 'express';
import * as dotenv from 'dotenv';
import { randomBytes } from 'crypto';
import {
    ServicesContainer,
    PorticoConfig,
    Address,
    Customer,
    CreditCardData,
    ScheduleFrequency,
    EmailReceipt,
    ApiError
} from 'globalpayments-api';

// Load environment variables from .env file
dotenv.config();

/**
 * Initialize Express application with necessary middleware
 */
const app = express();
const port = process.env.PORT || 8000;

app.use(express.static('.')); // Serve static files
app.use(express.urlencoded({ extended: true })); // Parse form data
app.use(express.json()); // Parse JSON requests

// Configure Global Payments SDK with credentials and settings
const config = new PorticoConfig();
config.secretApiKey = process.env.SECRET_API_KEY;
config.serviceUrl = 'https://cert.api2.heartlandportico.com';
ServicesContainer.configureService(config);

/**
 * Sanitize postal code by removing invalid characters
 * Only allows alphanumeric characters and hyphens, limited to 10 characters
 * 
 * @param {string} postalCode - The postal code to sanitize
 * @returns {string} The sanitized postal code
 */
const sanitizePostalCode = (postalCode) => {
    return postalCode.replace(/[^a-zA-Z0-9-]/g, '').slice(0, 10);
};

/**
 * Generate a UUID v4 formatted string
 * 
 * @returns {string} A UUID v4 formatted string
 */
const generateUuidV4 = () => {
    const bytes = randomBytes(16);
    bytes[6] = (bytes[6] & 0x0f) | 0x40; // Set version to 0100
    bytes[8] = (bytes[8] & 0x3f) | 0x80; // Set variant
    return bytes.toString('hex').replace(/(.{8})(.{4})(.{4})(.{4})(.{12})/, '$1-$2-$3-$4-$5');
};

/**
 * Generate a unique customer ID using UUID v4 format
 * 
 * @returns {string} A UUID v4 formatted string for customer identification
 */
const generateCustomerId = () => {
    return generateUuidV4();
};

/**
 * Generate a unique schedule ID using UUID v4 format
 * 
 * @returns {string} A UUID v4 formatted string for schedule identification
 */
const generateScheduleId = () => {
    return generateUuidV4();
};

/**
 * Generate a unique payment method ID using UUID v4 format
 * 
 * @returns {string} A UUID v4 formatted string for payment method identification
 */
const generatePaymentMethodId = () => {
    return generateUuidV4();
};

/**
 * Config endpoint - provides public API key for client-side tokenization
 */
app.get('/config', (req, res) => {
    res.json({
        success: true,
        data: {
            publicApiKey: process.env.PUBLIC_API_KEY
        }
    });
});

/**
 * Process payment endpoint - handles recurring payment setup
 * Expects form data with customer information and payment_token
 */
app.post('/process-payment', async (req, res) => {
    try {
        // Validate required fields are present
        const requiredFields = [
            'payment_token', 'first_name', 'last_name', 'email', 'phone',
            'street_address', 'city', 'state', 'billing_zip', 'country', 'amount'
        ];
        
        for (const field of requiredFields) {
            if (!req.body[field] || !req.body[field].toString().trim()) {
                throw new Error(`Missing required field: ${field}`);
            }
        }

        // Parse and validate amount
        const amount = parseFloat(req.body.amount);
        if (isNaN(amount) || amount <= 0) {
            throw new Error('Invalid amount');
        }

        // Create customer record with form data
        const customer = new Customer();
        customer.id = generateCustomerId();
        customer.firstName = req.body.first_name.toString().trim();
        customer.lastName = req.body.last_name.toString().trim();
        customer.status = 'Active';
        customer.email = req.body.email.toString().trim();
        customer.address = new Address();
        customer.address.streetAddress1 = req.body.street_address.toString().trim();
        customer.address.city = req.body.city.toString().trim();
        customer.address.province = req.body.state.toString().trim();
        customer.address.postalCode = sanitizePostalCode(req.body.billing_zip);
        customer.address.country = req.body.country.toString().trim();
        customer.workPhone = req.body.phone.toString().trim();
        const createdCustomer = await customer.create();

        // Create payment method using tokenized card information
        const card = new CreditCardData();
        card.token = req.body.payment_token;

        const paymentMethod = await createdCustomer.addPaymentMethod(
            generatePaymentMethodId(),
            card
        ).create();

        // Create payment schedule
        const schedule = await paymentMethod.addSchedule(
            generateScheduleId()
        )
            .withStatus('Active')
            .withAmount(amount)
            .withCurrency('USD')
            .withStartDate(new Date('2027-02-01'))
            .withFrequency(ScheduleFrequency.Weekly)
            .withEndDate(new Date('2027-04-01'))
            .withReprocessingCount(2)
            .withEmailReceipt(EmailReceipt.Never)
            .create();

        // Return success response with schedule key
        res.json({
            success: true,
            message: `Schedule created successfully! Schedule Key: ${schedule.key}`,
            data: {
                scheduleKey: schedule.key
            }
        });
    } catch (error) {
        // Handle different types of errors appropriately
        console.error(error);
        switch (error.name) {
            case ApiError.constructor.name:
                // Handle API-specific errors
                res.status(400).json({
                    success: false,
                    message: 'Recurring payment schedule setup failed',
                    error: {
                        code: 'API_ERROR',
                        details: error.message
                    }
                });
                break;
            default:
                // Handle general errors
                res.status(500).json({
                    success: false,
                    message: 'Internal server error',
                    error: {
                        code: 'SERVER_ERROR',
                        details: error.message
                    }
                });
                break;
        }
    }
});

// Start the server
app.listen(port, '0.0.0.0', () => {
    console.log(`Server running at http://localhost:${port}`);
    console.log(`Server also accessible at http://127.0.0.1:${port}`);
    console.log(`Server also accessible at http://0.0.0.0:${port}`);
});