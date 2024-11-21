package ds.project4_currencyconverter;

import com.mongodb.client.AggregateIterable;
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

import java.io.IOException;
import java.util.Arrays;

/**
 * DashboardServlet displays logs and analytics in a web-based dashboard.
 *
 * Features:
 * - Provides a web-based dashboard accessible at "/dashboard".
 * - Displays three analytics: total requests, average response time, and the most common source currency.
 * - Lists all interaction logs in a tabular format.
 */
@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {

    private MongoCollection<Document> logCollection;

    /**
     * Initializes the servlet by connecting to the MongoDB Atlas database.
     *
     * @throws ServletException if MongoDB connection fails.
     */
    @Override
    public void init() throws ServletException {
        try {
            // Load MongoDB URI from environment variables
            String mongoUri = System.getenv("MONGO_URI");
            MongoClient mongoClient = MongoClients.create(mongoUri);
            MongoDatabase database = mongoClient.getDatabase("CurrencyConverterDB");
            logCollection = database.getCollection("ConversionLogs");
        } catch (Exception e) {
            throw new ServletException("Failed to connect to MongoDB", e);
        }
    }

    /**
     * Handles GET requests to display the dashboard.
     *
     * - Displays analytics such as total requests, average response time, and the most common source currency.
     * - Lists all logs in a tabular format.
     *
     * @param req  the HTTP request.
     * @param resp the HTTP response with HTML content.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");
        StringBuilder html = new StringBuilder("<html><head><title>Dashboard</title></head><body>");
        html.append("<h1>Currency Conversion Dashboard</h1>");

        // Add analytics section
        html.append("<h2>Analytics</h2>");
        html.append("<ul>");
        html.append("<li>Total Requests: ").append(logCollection.countDocuments()).append("</li>");
        html.append("<li>Average Response Time (ms): ").append(getAverageResponseTime()).append("</li>");
        html.append("<li>Most Common Source Currency: ").append(getMostCommonCurrency("fromCurrency")).append("</li>");
        html.append("</ul>");

        // Add logs table
        html.append("<h2>Logs</h2>");
        html.append("<table border='1'>");
        html.append("<tr><th>Timestamp</th><th>User Agent</th><th>From</th><th>To</th><th>Amount</th><th>Converted Amount</th><th>Response Time (ms)</th></tr>");

        // Iterate over log documents and populate the table
        for (Document log : logCollection.find()) {
            html.append("<tr>")
                    .append("<td>").append(log.getString("timestamp")).append("</td>")
                    .append("<td>").append(log.getString("userAgent")).append("</td>")
                    .append("<td>").append(log.getString("fromCurrency")).append("</td>")
                    .append("<td>").append(log.getString("toCurrency")).append("</td>")
                    .append("<td>").append(log.getDouble("amount")).append("</td>")
                    .append("<td>").append(log.getDouble("convertedAmount")).append("</td>")
                    .append("<td>").append(log.getLong("responseTime")).append("</td>")
                    .append("</tr>");
        }

        html.append("</table>");
        html.append("</body></html>");

        // Write the HTML to the response
        resp.getWriter().write(html.toString());
    }

    /**
     * Calculates the average response time from the logs.
     *
     * @return the average response time in milliseconds.
     */
    private double getAverageResponseTime() {
        AggregateIterable<Document> result = logCollection.aggregate(Arrays.asList(
                new Document("$group", new Document("_id", null).append("avgResponseTime", new Document("$avg", "$responseTime")))
        ));
        Document doc = result.first();
        return doc != null ? doc.getDouble("avgResponseTime") : 0;
    }

    /**
     * Finds the most common currency field value (e.g., 'fromCurrency' or 'toCurrency').
     *
     * @param field the field name to analyze (e.g., "fromCurrency").
     * @return the most common value for the specified field.
     */
    private String getMostCommonCurrency(String field) {
        AggregateIterable<Document> result = logCollection.aggregate(Arrays.asList(
                new Document("$group", new Document("_id", "$" + field).append("count", new Document("$sum", 1))),
                new Document("$sort", new Document("count", -1)),
                new Document("$limit", 1)
        ));
        Document doc = result.first();
        return doc != null ? doc.getString("_id") : "N/A";
    }
}
