/**
 * Author: Kiranmayi Anupindi
 * Andrew id: kanupind
 * Project 4 Task 2
 * Last Modified: November 19, 2024
 *
 * This servlet handles currency conversion using the Fixer API.
 * It reads the API key securely from an environment variable
 * injected as a GitHub Codespaces secret.
 * The servlet provides a RESTful endpoint for converting an amount
 * from one currency to another. The response includes only the converted
 * amount to minimize data transfer.
 */

package ds.project4_currencyconverter;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * CurrencyConverterServlet handles HTTP requests for currency conversion.
 * It uses the Fixer API to fetch real-time exchange rates and calculates
 * the converted amount based on user input.
 */
@WebServlet("/convertCurrency")
public class CurrencyConverterServlet extends HttpServlet {

    // Base URL for the Fixer API
    private static final String FIXER_API_URL = "http://data.fixer.io/api/latest";

    // Variable to store the API key loaded from environment variables
    private String fixerApiKey;

    /**
     * Initializes the servlet and loads the API key from the environment variable.
     *
     * @throws ServletException If the API key is not found or is invalid.
     */
    @Override
    public void init() throws ServletException {
        // Load the API key from environment variables
        fixerApiKey = System.getenv("FIXER_API_KEY");

        if (fixerApiKey == null || fixerApiKey.isEmpty()) {
            throw new ServletException("Fixer API key is missing. Ensure the FIXER_API_KEY environment variable is set.");
        }
    }

    /**
     * Handles GET requests for currency conversion.
     * Extracts query parameters, fetches exchange rates from the Fixer API,
     * and calculates the converted amount.
     *
     * @param req  The HTTP request containing query parameters: 'from', 'to', and 'amount'.
     * @param resp The HTTP response containing the conversion result as JSON.
     * @throws ServletException If an error occurs during request processing.
     * @throws IOException      If an I/O error occurs during communication with the Fixer API.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Set response content type to JSON
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        // Extract query parameters
        String fromCurrency = req.getParameter("from");
        String toCurrency = req.getParameter("to");
        String amountStr = req.getParameter("amount");

        // Validate query parameters
        if (fromCurrency == null || toCurrency == null || amountStr == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Missing query parameters: 'from', 'to', 'amount'\"}");
            return;
        }

        try {
            // Parse the amount parameter
            double amount = Double.parseDouble(amountStr);

            // Fetch exchange rates using the API key from the environment variable
            Gson gson = new Gson();
            String apiUrl = FIXER_API_URL + "?access_key=" + fixerApiKey;
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the API response
                BufferedReader apiReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder apiResponse = new StringBuilder();
                String line;
                while ((line = apiReader.readLine()) != null) {
                    apiResponse.append(line);
                }
                apiReader.close();

                // Parse the response to extract exchange rates
                FixerResponse fixerResponse = gson.fromJson(apiResponse.toString(), FixerResponse.class);
                Map<String, Double> rates = fixerResponse.getRates();

                Double baseRate = rates.get(fromCurrency.toUpperCase());
                Double targetRate = rates.get(toCurrency.toUpperCase());

                if (baseRate != null && targetRate != null) {
                    // Calculate the converted amount
                    double exchangeRate = targetRate / baseRate;
                    double convertedAmount = amount * exchangeRate;

                    // Respond with only the converted amount
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write(gson.toJson(Map.of("convertedAmount", convertedAmount)));
                } else {
                    // Handle invalid currency codes
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("{\"error\": \"Invalid currency codes\"}");
                }
            } else {
                // Handle API response errors
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"error\": \"Failed to fetch exchange rates\"}");
            }
        } catch (NumberFormatException e) {
            // Handle invalid amount format
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Invalid amount value\"}");
        } catch (Exception e) {
            // Handle general exceptions
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Handles POST requests by redirecting them to the GET handler.
     *
     * @param req  The HTTP POST request.
     * @param resp The HTTP response.
     * @throws ServletException If a servlet-specific error occurs.
     * @throws IOException      If an I/O error occurs.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Redirect POST requests to GET for browser testing
        doGet(req, resp);
    }

    /**
     * Helper class for parsing the Fixer API response.
     * Maps the 'rates' field of the API response to a Java Map.
     */
    static class FixerResponse {
        private Map<String, Double> rates;

        /**
         * Returns the exchange rates fetched from the Fixer API.
         *
         * @return A map of currency codes to their exchange rates relative to EUR.
         */
        public Map<String, Double> getRates() {
            return rates;
        }
    }
}
