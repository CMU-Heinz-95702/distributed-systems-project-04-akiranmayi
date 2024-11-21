package ds.project4_currencyconverter;

import com.google.gson.Gson;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.Map;

/**
 * CurrencyConverterServlet handles currency conversion requests and logs detailed information
 * about each interaction to MongoDB Atlas.
 *
 * Features:
 * - Processes GET requests with parameters: 'from', 'to', and 'amount'.
 * - Calls the Fixer API for real-time exchange rates.
 * - Logs six pieces of information including request details, converted amount, and response time.
 * - Stores logs persistently in MongoDB Atlas.
 */
//@WebServlet("/convertCurrency")
public class CurrencyConverterServlet extends HttpServlet {

    private static final String FIXER_API_URL = "http://data.fixer.io/api/latest"; // Base URL for Fixer API
    private String fixerApiKey; // API key loaded from environment variables
    private MongoCollection<Document> logCollection; // MongoDB collection for storing logs

    /**
     * Initializes the servlet.
     * - Fetches the Fixer API key from environment variables.
     * - Establishes a connection to MongoDB Atlas.
     *
     * @throws ServletException if initialization fails (e.g., missing API key or MongoDB URI).
     */
    @Override
    public void init() throws ServletException {
        // Load Fixer API key from environment variables
        fixerApiKey = System.getenv("FIXER_API_KEY");
        if (fixerApiKey == null || fixerApiKey.isEmpty()) {
            throw new ServletException("Fixer API key is missing. Ensure the FIXER_API_KEY environment variable is set.");
        }

        // Connect to MongoDB Atlas
        try {
            String mongoUri = System.getenv("MONGO_URI");
            MongoClient mongoClient = MongoClients.create(mongoUri);
            MongoDatabase database = mongoClient.getDatabase("CurrencyConverterDB");
            logCollection = database.getCollection("ConversionLogs");
        } catch (Exception e) {
            throw new ServletException("Failed to connect to MongoDB", e);
        }
    }

    /**
     * Handles GET requests for currency conversion.
     * - Fetches real-time exchange rates from Fixer API.
     * - Logs request and response details to MongoDB.
     * - Responds with the converted amount in JSON format.
     *
     * @param req  the HTTP request containing parameters: 'from', 'to', and 'amount'.
     * @param resp the HTTP response with the conversion result or error message.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        // Extract query parameters
        String fromCurrency = req.getParameter("from");
        String toCurrency = req.getParameter("to");
        String amountStr = req.getParameter("amount");

        if (fromCurrency == null || toCurrency == null || amountStr == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Missing query parameters: 'from', 'to', 'amount'\"}");
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            // Parse amount parameter
            double amount = Double.parseDouble(amountStr);
            Gson gson = new Gson();

            // Call Fixer API
            String apiUrl = FIXER_API_URL + "?access_key=" + fixerApiKey;
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder apiResponse = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    apiResponse.append(line);
                }
                reader.close();

                // Parse response and compute conversion
                FixerResponse fixerResponse = gson.fromJson(apiResponse.toString(), FixerResponse.class);
                Map<String, Double> rates = fixerResponse.getRates();
                Double baseRate = rates.get(fromCurrency.toUpperCase());
                Double targetRate = rates.get(toCurrency.toUpperCase());

                if (baseRate != null && targetRate != null) {
                    double exchangeRate = targetRate / baseRate;
                    double convertedAmount = amount * exchangeRate;

                    // Log interaction
                    long responseTime = System.currentTimeMillis() - startTime;
                    logToMongo(req, fromCurrency, toCurrency, amount, convertedAmount, responseTime);

                    // Respond with the converted amount
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write(gson.toJson(Map.of("convertedAmount", convertedAmount)));
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("{\"error\": \"Invalid currency codes\"}");
                }
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"error\": \"Failed to fetch exchange rates\"}");
            }
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Invalid amount value\"}");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"Failed to process request: " + e.getMessage() + "\"}");
        }
    }

    /**
     * Logs request and response details to MongoDB Atlas.
     *
     * @param req           the HTTP request.
     * @param fromCurrency  source currency.
     * @param toCurrency    target currency.
     * @param amount        amount to convert.
     * @param convertedAmount the converted amount.
     * @param responseTime  time taken to process the request.
     */
    private void logToMongo(HttpServletRequest req, String fromCurrency, String toCurrency, double amount, double convertedAmount, long responseTime) {
        Document log = new Document()
                .append("timestamp", Instant.now().toString())
                .append("userAgent", req.getHeader("User-Agent"))
                .append("fromCurrency", fromCurrency)
                .append("toCurrency", toCurrency)
                .append("amount", amount)
                .append("convertedAmount", convertedAmount)
                .append("responseTime", responseTime);

        logCollection.insertOne(log);
    }

    /**
     * Handles POST requests by redirecting them to the GET handler.
     *
     * @param req  the HTTP POST request.
     * @param resp the HTTP response.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    /**
     * FixerResponse represents the structure of the Fixer API response.
     * Contains exchange rates as a map.
     */
    static class FixerResponse {
        private Map<String, Double> rates;

        public Map<String, Double> getRates() {
            return rates;
        }
    }
}
