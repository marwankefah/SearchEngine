package com.apt;

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


    public SeedWorker() {
        this.initialLink = Crawler.getAvailableLink();
    }

    private void process(String pageLink){


        Page page = Utils.getPage(pageLink);
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
        process(Crawler.getAvailableLink());
    }

    @Override
    public void run() {
        process(initialLink);
    }
}
