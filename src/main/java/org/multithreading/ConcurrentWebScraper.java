package org.multithreading;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.*;
import java.sql.*;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcurrentWebScraper {
    private static int THREAD_COUNT;
    private static String DB_URL, DB_USER, DB_PASSWORD;
    private static final String CSV_FILE = "scraped_data.csv";
    private static final Object LOCK = new Object(); // Synchronization lock for file writing

    public static void main(String[] args) {
        loadConfig();

        List<String> urls = List.of(
                "https://techcrunch.com/",
                "https://arstechnica.com/",
                "https://www.theverge.com/",
                "https://www.technologyreview.com/",
                "https://openai.com/news/"
        );

        // Create CSV file and add headers
        createCSVFile();

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (String url : urls) {
            executor.execute(() -> scrapeWebsite(url));
        }

        executor.shutdown();
    }

    // Load thread count from config
    private static void loadConfig() {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream("src/config.properties")) {
            properties.load(fis);
            THREAD_COUNT = Integer.parseInt(properties.getProperty("thread.count"));
            DB_URL = properties.getProperty("db.url");
            DB_USER = properties.getProperty("db.user");
            DB_PASSWORD = properties.getProperty("db.password");
            System.out.println("Config loaded successfully.");
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading config: " + e.getMessage());
            THREAD_COUNT = 5; // Default value
        }
    }

    // Scrape website and store data
    public static void scrapeWebsite(String url) {
        try {
            System.out.println("Scraping: " + url + " by " + Thread.currentThread().getName());

            // Set up Jsoup connection with agent and timeout handling
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
                    .referrer("http://www.google.com")
                    .timeout(5000) // 5 seconds timeout
                    .ignoreHttpErrors(true)
                    .followRedirects(true)
                    .get();

            String title = doc.title();

            // Write data to CSV and database
            writeToCSV(url, title);
            writeToDatabase(url, title);

        } catch (IOException e) {
            System.err.println("Error scraping " + url + ": " + e.getMessage());
            if (e.getMessage().contains("Connection timed out")) {
                System.err.println("Timeout error for " + url);
            } else if (e.getMessage().contains("403")) {
                System.err.println("Access forbidden for " + url);
            } else {
                System.err.println("General network error for " + url);
            }
        } catch (Exception e) {
            System.err.println("Unexpected error scraping " + url + ": " + e.getMessage());
        }
    }

    // Create CSV file with headers
    private static void createCSVFile() {
        try (FileWriter writer = new FileWriter(CSV_FILE)) {
            writer.append("URL,Title\n");
        } catch (IOException e) {
            System.err.println("Error creating CSV file: " + e.getMessage());
        }
    }

    // Append scraped data to CSV file (Thread-Safe)
    private static void writeToCSV(String url, String title) {
        synchronized (LOCK) {
            try (FileWriter writer = new FileWriter(CSV_FILE, true)) {
                writer.append(url).append(",").append(title).append("\n");
            } catch (IOException e) {
                System.err.println("Error writing to CSV: " + e.getMessage());
            }
        }
    }

    // Insert scraped data into MySQL database
    private static void writeToDatabase(String url, String title) {
        String query = "INSERT INTO ScrapedData (url, title) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, url);
            stmt.setString(2, title);
            stmt.executeUpdate();
            System.out.println("Inserted into DB: " + url);

        } catch (SQLException e) {
            System.err.println("Error inserting into database: " + e.getMessage());
            if (e.getMessage().contains("Communications link failure")) {
                System.err.println("Database connection error.");
            } else if (e.getMessage().contains("Access denied")) {
                System.err.println("Access denied for the database user.");
            } else {
                System.err.println("SQL error: " + e.getMessage());
            }
        }
    }
}