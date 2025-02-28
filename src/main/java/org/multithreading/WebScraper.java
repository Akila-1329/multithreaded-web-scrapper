package org.multithreading;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public class WebScraper {
    public static void main(String[] args) {
        String url = "https://techcrunch.com/";

        try{
            Document techCrunchDoc = Jsoup.connect(url).get();
            System.out.println("Title: " + techCrunchDoc.title());
        }
        catch(IOException ioexception){
            ioexception.printStackTrace();
        }
    }
}