package com.apt;

import com.jaunt.UserAgent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * @description This class handles one seed entry
 */
public class SeedWorker implements Runnable {

    String initialLink;
    UserAgent userAgent;

    public SeedWorker() {
        this.userAgent = new UserAgent();
        this.initialLink = Crawler.getAvailableLink();
    }



    private void process(String pageLink){

        try{
            if(Thread.interrupted()){
                return;
            }

            Page page = new Page(pageLink, this.userAgent);
            if(page == null){
                return;
            }
            page.saveToDB();
            ArrayList<String> links = page.getLinks();
            Crawler.addProcessedLink(pageLink);
            if(links == null || links.size() == 0){
                System.out.println("Couldn't find any links on page: " + pageLink);
            }else{
                Crawler.addUnprocessedLinks(links);
            }
            String newLink = Crawler.getAvailableLink();
            if(newLink == null) return;
            process(Crawler.getAvailableLink());

        }catch (Exception e){
            e.printStackTrace();
            process(Crawler.getAvailableLink());
        }
    }

    @Override
    public void run() {
        process(initialLink);
    }
}
