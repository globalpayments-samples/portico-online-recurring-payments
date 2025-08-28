package com.globalpayments.example;

import com.global.api.ServicesContainer;
import com.global.api.entities.Address;
import com.global.api.entities.Customer;
import com.global.api.entities.Transaction;
import com.global.api.entities.enums.ScheduleFrequency;
import com.global.api.entities.exceptions.ApiException;
import com.global.api.entities.exceptions.ConfigurationException;
import com.global.api.paymentMethods.CreditCardData;
import com.global.api.serviceConfigs.PorticoConfig;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

/**
 * Recurring Payment Processing Servlet
 * 
 * This servlet demonstrates recurring payment processing using the Global Payments SDK.
 * It provides endpoints for configuration and recurring payment schedule setup, handling 
 * tokenized card data and customer information to create secure payment schedules.
 * 
 * Endpoints:
 * - GET /config: Returns the public API key for client-side tokenization
 * - POST /process-payment: Creates recurring payment schedules using tokenized data
 * 
 * @author Global Payments
 * @version 1.0
 */

@WebServlet(urlPatterns = {"/process-payment", "/config"})
public class ProcessPaymentServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    private final Dotenv dotenv = Dotenv.load();
    
    /**
     * Initializes the servlet and configures the Global Payments SDK.
     * This must be called before processing any payments.
     * 
     * @throws ServletException if there's an error initializing the servlet
     */
    @Override
    public void init() throws ServletException {
        try {
            // Configure the Global Payments SDK with credentials and settings
            PorticoConfig config = new PorticoConfig();
            config.setSecretApiKey(dotenv.get("SECRET_API_KEY"));
            config.setDeveloperId("000000");
            config.setVersionNumber("0000");
            config.setServiceUrl("https://cert.api2.heartlandportico.com");

            ServicesContainer.configureService(config);
        } catch (ConfigurationException e) {
            // Log configuration errors and propagate as ServletException
            throw new ServletException("Failed to configure Global Payments SDK", e);
        }
    }

    /**
     * Handles GET requests to /config endpoint.
     * Returns the public API key needed for client-side tokenization.
     *
     * @param request The HTTP request
     * @param response The HTTP response
     * @throws ServletException If there's an error in servlet processing
     * @throws IOException If there's an I/O error
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (request.getServletPath().equals("/config")) {
        response.setContentType("application/json");
        String publicKey = dotenv.get("PUBLIC_API_KEY");
        String jsonResponse = String.format(
            "{\"success\":true,\"data\":{\"publicApiKey\":\"%s\"}}", 
            publicKey
        );
        response.getWriter().write(jsonResponse);
        }
    }

    /**
     * Sanitizes postal code input by removing invalid characters.
     * Only allows alphanumeric characters and hyphens, limited to 10 characters.
     *
     * @param postalCode The postal code to sanitize, can be null
     * @return A sanitized postal code containing only alphanumeric characters
     *         and hyphens, limited to 10 characters. Returns empty string if input is null.
     */
    private String sanitizePostalCode(String postalCode) {
        if (postalCode == null) {
            return "";
        }
        String sanitized = postalCode.replaceAll("[^a-zA-Z0-9-]", "");
        return sanitized.length() > 10 ? sanitized.substring(0, 10) : sanitized;
    }

    /**
     * Generates a UUID v4 formatted string.
     *
     * @return A UUID v4 formatted string
     */
    private String generateUuidV4() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generates a unique customer ID using UUID v4 format.
     *
     * @return A UUID v4 formatted string for customer identification
     */
    private String generateCustomerId() {
        return generateUuidV4();
    }

    /**
     * Generates a unique schedule ID using UUID v4 format.
     *
     * @return A UUID v4 formatted string for schedule identification
     */
    private String generateScheduleId() {
        return generateUuidV4();
    }

    /**
     * Generates a unique payment method ID using UUID v4 format.
     *
     * @return A UUID v4 formatted string for payment method identification
     */
    private String generatePaymentMethodId() {
        return generateUuidV4();
    }

    /**
     * Handles POST requests to /process-payment endpoint.
     * Creates recurring payment schedules using tokenized card data and customer information.
     *
     * @param request The HTTP request containing payment and customer details
     * @param response The HTTP response
     * @throws ServletException If there's an error in servlet processing
     * @throws IOException If there's an I/O error
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");

        try {
            // Validate required customer and payment fields
            String[] requiredFields = {
                "payment_token", "first_name", "last_name", "email", "phone",
                "street_address", "city", "state", "billing_zip", "country", "amount"
            };
            
            for (String field : requiredFields) {
                String value = request.getParameter(field);
                if (value == null || value.trim().isEmpty()) {
                    throw new ApiException("Missing required field: " + field);
                }
            }

            // Parse and validate amount
            BigDecimal amount;
            try {
                amount = new BigDecimal(request.getParameter("amount"));
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ApiException("Amount must be a positive number");
                }
            } catch (NumberFormatException e) {
                throw new ApiException("Invalid amount format");
            }

            // Create customer record with form data
            Customer customer = new Customer();
            customer.setId(generateCustomerId());
            customer.setFirstName(request.getParameter("first_name").trim());
            customer.setLastName(request.getParameter("last_name").trim());
            customer.setStatus("Active");
            customer.setEmail(request.getParameter("email").trim());
            customer.setWorkPhone(request.getParameter("phone").trim());
            
            Address customerAddress = new Address();
            customerAddress.setStreetAddress1(request.getParameter("street_address").trim());
            customerAddress.setCity(request.getParameter("city").trim());
            customerAddress.setProvince(request.getParameter("state").trim());
            customerAddress.setPostalCode(sanitizePostalCode(request.getParameter("billing_zip")));
            customerAddress.setCountry(request.getParameter("country").trim());
            customer.setAddress(customerAddress);
            
            customer = customer.create();

            // Create payment method using tokenized card information
            CreditCardData card = new CreditCardData();
            card.setToken(request.getParameter("payment_token"));
            
            var paymentMethod = customer.addPaymentMethod(
                generatePaymentMethodId(),
                card
            ).create();

            // Create payment schedule with weekly frequency from 2027-02-01 to 2027-04-01
            Date startDate = Date.from(LocalDate.of(2027, 2, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date endDate = Date.from(LocalDate.of(2027, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            
            var schedule = paymentMethod.addSchedule(generateScheduleId())
                    .withStatus("Active")
                    .withAmount(amount)
                    .withCurrency("USD")
                    .withStartDate(startDate)
                    .withFrequency(ScheduleFrequency.Weekly)
                    .withEndDate(endDate)
                    .withReprocessingCount(2)
                    .create();

            // Return success response with schedule key
            String successResponse = String.format(
                "{\"success\":true,\"message\":\"Schedule created successfully! Schedule Key: %s\",\"data\":{\"scheduleKey\":\"%s\"}}", 
                schedule.getKey(),
                schedule.getKey()
            );
            response.getWriter().write(successResponse);

        } catch (ApiException e) {
            // Handle recurring payment processing errors
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            String errorResponse = String.format(
                "{\"success\":false,\"message\":\"Recurring payment schedule setup failed\",\"error\":{\"code\":\"API_ERROR\",\"details\":\"%s\"}}", 
                e.getMessage()
            );
            response.getWriter().write(errorResponse);
        }
    }
}
