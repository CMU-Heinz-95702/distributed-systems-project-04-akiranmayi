# **Currency Converter Application**

## Overview:  
This project implements a distributed application that converts currencies using a mobile app, a web service, and a dashboard for monitoring. The application uses the Fixer API to fetch real-time currency exchange rates and logs all interactions in a MongoDB Atlas database. A web-based dashboard provides insights into app usage and system performance.

## Architecture:  
The project follows a distributed architecture with the following components:

Mobile Application (Android): A native Android app allows users to input two currencies (base and target) and displays the conversion rate.  
Web Service (Codespaces): A Java-based web service hosted on GitHub Codespaces receives HTTP requests from the Android app, queries the Fixer API, and returns the exchange rate.  
MongoDB Atlas: A cloud-hosted NoSQL database stores logs of all user requests and responses for analytics and debugging.  
Web Dashboard: A web-based dashboard displays logs and usage analytics.  
  
## Features:  

### Android App:  

Takes base and target currency inputs.
Sends an HTTP request to the web service.
Displays the conversion rate to the user.
Handles input validation and network errors gracefully.

### Web Service:  

Fetches exchange rates from the Fixer API.
Logs requests and responses to MongoDB Atlas.
Handles invalid inputs and external API failures.

### Web Dashboard:  

Displays usage analytics (e.g., top requested currencies, average API latency).
Provides formatted logs for debugging and monitoring.

## Technologies Used:  
Android Studio: For building the mobile application.  
Java: For web service and Android app development.  
MongoDB Atlas: For storing and querying logs.  
Fixer API: For fetching real-time currency exchange rates.  
GitHub Codespaces: For hosting the web service.  
JSP/Servlets: For building the web dashboard.  
Docker: For containerizing and deploying the web service.
