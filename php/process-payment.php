<?php

declare(strict_types=1);

/**
 * Card Payment Processing Script
 *
 * This script demonstrates card payment processing using the Global Payments SDK.
 * It handles tokenized card data and billing information to process payments
 * securely through the Global Payments API.
 *
 * PHP version 7.4 or higher
 *
 * @category  Payment_Processing
 * @package   GlobalPayments_Sample
 * @author    Global Payments
 * @license   MIT License
 * @link      https://github.com/globalpayments
 */

require_once 'vendor/autoload.php';

use Dotenv\Dotenv;
use GlobalPayments\Api\Entities\Address;
use GlobalPayments\Api\Entities\Customer;
use GlobalPayments\Api\Entities\Enums\ScheduleFrequency;
use GlobalPayments\Api\Entities\Exceptions\ApiException;
use GlobalPayments\Api\PaymentMethods\CreditCardData;
use GlobalPayments\Api\ServiceConfigs\Gateways\PorticoConfig;
use GlobalPayments\Api\ServicesContainer;

ini_set('display_errors', '0');

/**
 * Configure the SDK
 *
 * Sets up the Global Payments SDK with necessary credentials and settings
 * loaded from environment variables.
 *
 * @return void
 */
function configureSdk(): void
{
    $dotenv = Dotenv::createImmutable(__DIR__);
    $dotenv->load();

    $config = new PorticoConfig();
    $config->secretApiKey = $_ENV['SECRET_API_KEY'];
    $config->developerId = '000000';
    $config->versionNumber = '0000';
    $config->serviceUrl = 'https://cert.api2.heartlandportico.com';
    
    ServicesContainer::configureService($config);
}

/**
 * Sanitize postal code by removing invalid characters
 *
 * @param string|null $postalCode The postal code to sanitize
 *
 * @return string Sanitized postal code containing only alphanumeric
 *                characters and hyphens, limited to 10 characters
 */
function sanitizePostalCode(?string $postalCode): string
{
    if ($postalCode === null) {
        return '';
    }
    
    $sanitized = preg_replace('/[^a-zA-Z0-9-]/', '', $postalCode);
    return substr($sanitized, 0, 10);
}

/**
 * Generate a UUID v4 formatted string
 *
 * @return string A UUID v4 formatted string
 */
function generateUuidV4(): string
{
    $data = random_bytes(16);
    $data[6] = chr(ord($data[6]) & 0x0f | 0x40); // Set version to 0100
    $data[8] = chr(ord($data[8]) & 0x3f | 0x80); // Set variant
    return vsprintf('%s%s-%s-%s-%s-%s%s%s', str_split(bin2hex($data), 4));
}

/**
 * Generate a unique customer ID using UUID v4 format
 *
 * @return string A UUID v4 formatted string for customer identification
 */
function generateCustomerId(): string
{
    return generateUuidV4();
}

/**
 * Generate a unique schedule ID using UUID v4 format
 *
 * @return string A UUID v4 formatted string for schedule identification
 */
function generateScheduleId(): string
{
    return generateUuidV4();
}

/**
 * Generate a unique payment method ID using UUID v4 format
 *
 * @return string A UUID v4 formatted string for payment method identification
 */
function generatePaymentMethodId(): string
{
    return generateUuidV4();
}

// Initialize SDK configuration
configureSdk();

try {
    // Validate required fields
    $requiredFields = [
        'payment_token', 'first_name', 'last_name', 'email', 'phone', 
        'street_address', 'city', 'state', 'billing_zip', 'country', 'amount'
    ];
    
    foreach ($requiredFields as $field) {
        if (!isset($_POST[$field]) || empty(trim($_POST[$field]))) {
            throw new ApiException("Missing required field: $field");
        }
    }
    
    // Parse and validate amount
    $amount = floatval($_POST['amount']);
    if ($amount <= 0) {
        throw new ApiException('Invalid amount');
    }

    // Create customer record with form data
    $customer = new Customer();
    $customer->id = generateCustomerId();
    $customer->firstName = trim($_POST['first_name']);
    $customer->lastName = trim($_POST['last_name']);
    $customer->status = 'Active';
    $customer->email = trim($_POST['email']);
    $customer->address = new Address();
    $customer->address->streetAddress1 = trim($_POST['street_address']);
    $customer->address->city = trim($_POST['city']);
    $customer->address->province = trim($_POST['state']);
    $customer->address->postalCode = sanitizePostalCode($_POST['billing_zip']);
    $customer->address->country = trim($_POST['country']);
    $customer->workPhone = trim($_POST['phone']);
    $customer = $customer->create();

    // Create payment method using tokenized card information
    $card = new CreditCardData();
    $card->token = $_POST['payment_token'];

    $paymentMethod = $customer->addPaymentMethod(
        generatePaymentMethodId(),
        $card
    )->create();

    // Create payment schedule
    $schedule = $paymentMethod->addSchedule(
        generateScheduleId(),
    )
        ->withStatus('Active')
        ->withAmount($amount)
        ->withCurrency('USD')
        ->withStartDate(\DateTime::createFromFormat('Y-m-d', '2027-02-01'))
        ->withFrequency(ScheduleFrequency::WEEKLY)
        ->withEndDate(\DateTime::createFromFormat('Y-m-d', '2027-04-01'))
        ->withReprocessingCount(2)
        ->create();

    // Return success response with transaction ID
    echo json_encode([
        'success' => true,
        'message' => 'Schedule created successfully! Schedule Key: ' . $schedule->key,
        'data' => [
            'scheduleKey' => $schedule->key
        ]
    ]);
} catch (ApiException $e) {
    // Handle payment processing errors
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'message' => 'Recurring payment schedule setup failed',
        'error' => [
            'code' => 'API_ERROR',
            'details' => $e->getMessage()
        ]
    ]);
}
