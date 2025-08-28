using GlobalPayments.Api;
using GlobalPayments.Api.Entities;
using GlobalPayments.Api.Entities.Enums;
using GlobalPayments.Api.PaymentMethods;
using dotenv.net;

namespace RecurringPaymentSample;

/// <summary>
/// Recurring Payment Processing Application
/// 
/// This application demonstrates recurring payment setup using the Global Payments SDK.
/// It provides endpoints for configuration and recurring payment schedule creation,
/// handling tokenized card data and customer information to ensure secure processing.
/// </summary>
public class Program
{
    public static void Main(string[] args)
    {
        // Load environment variables from .env file
        DotEnv.Load();

        var builder = WebApplication.CreateBuilder(args);
        
        var app = builder.Build();

        // Configure static file serving for the payment form
        app.UseDefaultFiles();
        app.UseStaticFiles();
        
        // Configure the SDK on startup
        ConfigureGlobalPaymentsSDK();

        ConfigureEndpoints(app);
        
        var port = System.Environment.GetEnvironmentVariable("PORT") ?? "8000";
        app.Urls.Add($"http://0.0.0.0:{port}");
        
        app.Run();
    }

    /// <summary>
    /// Configures the Global Payments SDK with necessary credentials and settings.
    /// This must be called before processing any payments.
    /// </summary>
    private static void ConfigureGlobalPaymentsSDK()
    {
        ServicesContainer.ConfigureService(new PorticoConfig
        {
            SecretApiKey = System.Environment.GetEnvironmentVariable("SECRET_API_KEY"),
            DeveloperId = "000000",
            VersionNumber = "0000",
            ServiceUrl = "https://cert.api2.heartlandportico.com"
        });
    }

    /// <summary>
    /// Configures the application's HTTP endpoints for payment processing.
    /// </summary>
    /// <param name="app">The web application to configure</param>
    private static void ConfigureEndpoints(WebApplication app)
    {
        // Configure HTTP endpoints
        app.MapGet("/config", () => Results.Ok(new
        { 
            success = true,
            data = new {
                publicApiKey = System.Environment.GetEnvironmentVariable("PUBLIC_API_KEY")
            }
        }));

        ConfigurePaymentEndpoint(app);
    }

    /// <summary>
    /// Sanitizes postal code input by removing invalid characters.
    /// </summary>
    /// <param name="postalCode">The postal code to sanitize. Can be null.</param>
    /// <returns>
    /// A sanitized postal code containing only alphanumeric characters and hyphens,
    /// limited to 10 characters. Returns empty string if input is null or empty.
    /// </returns>
    private static string SanitizePostalCode(string postalCode)
    {
        if (string.IsNullOrEmpty(postalCode)) return string.Empty;
        
        // Remove any characters that aren't alphanumeric or hyphen
        var sanitized = new string(postalCode.Where(c => char.IsLetterOrDigit(c) || c == '-').ToArray());
        
        // Limit length to 10 characters
        return sanitized.Length > 10 ? sanitized[..10] : sanitized;
    }

    /// <summary>
    /// Generate a UUID v4 formatted string using .NET's Guid.NewGuid()
    /// </summary>
    /// <returns>A UUID v4 formatted string</returns>
    private static string GenerateUuidV4()
    {
        return Guid.NewGuid().ToString();
    }

    /// <summary>
    /// Generate a unique customer ID using UUID v4 format
    /// </summary>
    /// <returns>A UUID v4 formatted string for customer identification</returns>
    private static string GenerateCustomerId()
    {
        return GenerateUuidV4();
    }

    /// <summary>
    /// Generate a unique schedule ID using UUID v4 format
    /// </summary>
    /// <returns>A UUID v4 formatted string for schedule identification</returns>
    private static string GenerateScheduleId()
    {
        return GenerateUuidV4();
    }

    /// <summary>
    /// Generate a unique payment method ID using UUID v4 format
    /// </summary>
    /// <returns>A UUID v4 formatted string for payment method identification</returns>
    private static string GeneratePaymentMethodId()
    {
        return GenerateUuidV4();
    }

    /// <summary>
    /// Configures the payment processing endpoint that handles recurring payment setup.
    /// </summary>
    /// <param name="app">The web application to configure</param>
    private static void ConfigurePaymentEndpoint(WebApplication app)
    {
        app.MapPost("/process-payment", async (HttpContext context) =>
        {
            // Parse form data from the request
            var form = await context.Request.ReadFormAsync();
            var token = form["payment_token"].ToString();
            var firstName = form["first_name"].ToString();
            var lastName = form["last_name"].ToString();
            var email = form["email"].ToString();
            var phone = form["phone"].ToString();
            var streetAddress = form["street_address"].ToString();
            var city = form["city"].ToString();
            var state = form["state"].ToString();
            var billingZip = form["billing_zip"].ToString();
            var country = form["country"].ToString();
            var amountStr = form["amount"].ToString();

            // Validate required fields are present
            string[] requiredFields = ["payment_token", "first_name", "last_name", "email", "phone", 
                                     "street_address", "city", "state", "billing_zip", "country", "amount"];
            
            foreach (var field in requiredFields)
            {
                var value = form[field].ToString();
                if (string.IsNullOrWhiteSpace(value))
                {
                    return Results.BadRequest(new {
                        success = false,
                        message = "Recurring payment schedule setup failed",
                        error = new {
                            code = "VALIDATION_ERROR",
                            details = $"Missing required field: {field}"
                        }
                    });
                }
            }

            // Validate and parse amount
            if (!decimal.TryParse(amountStr, out var amount) || amount <= 0)
            {
                return Results.BadRequest(new {
                    success = false,
                    message = "Recurring payment schedule setup failed",
                    error = new {
                        code = "VALIDATION_ERROR",
                        details = "Amount must be a positive number"
                    }
                });
            }

            try
            {
                // Create customer record with form data
                var customer = new Customer
                {
                    Id = GenerateCustomerId(),
                    FirstName = firstName.Trim(),
                    LastName = lastName.Trim(),
                    Status = "Active",
                    Email = email.Trim(),
                    WorkPhone = phone.Trim(),
                    Address = new Address
                    {
                        StreetAddress1 = streetAddress.Trim(),
                        City = city.Trim(),
                        Province = state.Trim(),
                        PostalCode = SanitizePostalCode(billingZip),
                        Country = country.Trim()
                    }
                };

                var createdCustomer = customer.Create();

                // Create payment method using tokenized card information
                var card = new CreditCardData
                {
                    Token = token
                };

                var paymentMethod = createdCustomer.AddPaymentMethod(
                    GeneratePaymentMethodId(),
                    card
                ).Create();

                // Create payment schedule
                var schedule = paymentMethod.AddSchedule(
                    GenerateScheduleId()
                )
                    .WithStatus("Active")
                    .WithAmount(amount)
                    .WithCurrency("USD")
                    .WithStartDate(new DateTime(2027, 2, 1))
                    .WithFrequency(ScheduleFrequency.WEEKLY)
                    .WithEndDate(new DateTime(2027, 4, 1))
                    .WithReprocessingCount(2)
                    .Create();

                // Return success response with schedule key
                return Results.Ok(new
                {
                    success = true,
                    message = $"Schedule created successfully! Schedule Key: {schedule.Key}",
                    data = new {
                        scheduleKey = schedule.Key
                    }
                });
            } 
            catch (ApiException ex)
            {
                // Handle recurring payment processing errors
                return Results.BadRequest(new {
                    success = false,
                    message = "Recurring payment schedule setup failed",
                    error = new {
                        code = "API_ERROR",
                        details = ex.Message
                    }
                });
            }
        });
    }
}
