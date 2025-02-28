package org.multithreading;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcurrentWebScraper {
    private static int THREAD_COUNT;

    public static void main(String[] args) {
        loadConfig();

        List<String> urls = List.of(
                "https://techcrunch.com/",
                "https://arstechnica.com/",
                "https://www.theverge.com/",
                "https://www.technologyreview.com/",
                "https://openai.com/news/"
        );

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);

        for (String url : urls) {
            executorService.execute(() -> scrapeWebsite(url));
        }

        executorService.shutdown();
    }

    public static void loadConfig() {
        Properties properties = new Properties();
        try (FileInputStream fileInputStream = new FileInputStream("src/config.properties")) {
            properties.load(fileInputStream);
            THREAD_COUNT = Integer.parseInt(properties.getProperty("thread.count", "5"));
            System.out.println("Thread count from config file: " + THREAD_COUNT);
        } catch (IOException | NumberFormatException exception) {
            System.err.println("Error loading config: " + exception.getMessage());
            THREAD_COUNT = 5;
        }
    }

    public static void scrapeWebsite(String url) {
        try {
            System.out.println("Scraping: " + url + " by " + Thread.currentThread().getName());

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36")
                    .referrer("http://www.google.com")
                    .timeout(5000) // 5 seconds timeout
                    .ignoreHttpErrors(true)
                    .followRedirects(true)
                    .get();

            System.out.println("Title: " + doc.title());
        } catch (IOException exception) {
            System.err.println("Error scraping " + url + ": " + exception.getMessage());
        }
    }
}
