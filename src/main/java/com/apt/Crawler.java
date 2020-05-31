package com.apt;

import com.jaunt.Element;
import com.jaunt.Elements;
import com.jaunt.UserAgent;

import java.util.*;

public class Crawler {

    private static final int MAX_NUMBER = 5000;

    private static ArrayList<Thread> threads;

    private static ArrayList<String> links = SeedListGenerator.getSeedLinks();
    private static Counter counter = new Counter(0);
    private static boolean finishedSeed = false;
    private static ArrayList<String> unprocessedLinks = new ArrayList<>();
    private static ArrayList<String> processedLinks = new ArrayList<String>();

    public static String getAvailableLink() {
        if(processedLinks.size() >= MAX_NUMBER){
//            Thread.currentThread().interrupt();
            return null;
        }
            if(finishedSeed){
                counter.check(unprocessedLinks.size());
                int decrementor = 0;
                if(links.size() != 0){
                    decrementor = links.size();
                }
                String link = unprocessedLinks.get(counter.getCount() - decrementor);
                counter.increment();
                DBManager.getInstance().removeUnprocessedLink(link);
                if(link == null) return getAvailableLink();
                return link;
            }else{
                if(links.size() < counter.getCount()+1){
                    finishedSeed = true;
                    return Crawler.getAvailableLink();
                }
                String link = links.get(counter.getCount());
                DBManager.getInstance().removeUnprocessedLink(link);
                counter.increment();
                if(link == null) return getAvailableLink();
                return link;
            }
    }

    public static void addUnprocessedLinks(ArrayList<String> links) {
        synchronized (unprocessedLinks){
//            if(unprocessedLinks.size() + Crawler.links.size() >= MAX_NUMBER) return;
//            if(unprocessedLinks.size() + Crawler.links.size() + links.size() >= MAX_NUMBER){
//                System.out.println("LOL");
//                int left = MAX_NUMBER - (unprocessedLinks.size() + Crawler.links.size());
//                unprocessedLinks.addAll(links.subList(0, left));
//                unprocessedLinks = Utils.removeDuplicates(unprocessedLinks);
//                DBManager.getInstance().saveUnprocessedLinks(links.subList(0, left));
//            }else{
                unprocessedLinks.addAll(links);
                unprocessedLinks = Utils.removeDuplicates(unprocessedLinks);
                //Maybe make it add just the new ones?
                DBManager.getInstance().saveUnprocessedLinks(links);
                counter.uncheck();
//            }
        }
    }

    public static void addProcessedLink(String link) {
        synchronized (processedLinks){
            processedLinks.add(link);
        }
    }

    public static void main(String[] args) {
        //Persistence...
//        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHandler()));

        int numThreads = Integer.parseInt(Utils.getUserInput("Number of threads: "));
        boolean recrawl = false;
        if(!recrawl){
            unprocessedLinks = DBManager.getInstance().getUnprocessedLinks();
            if(unprocessedLinks.size() != 0){
                links.clear();
            }
        }else{
            //Remove last time data....
            DBManager.getInstance().dropDB();
        }

        DBManager.getInstance().saveUnprocessedLinks(links);

        threads = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            Thread thread = new Thread(new SeedWorker());
            threads.add(thread);
            threads.get(i).start();
        }

//        try{
//            UserAgent userAgent = new UserAgent();         //create new userAgent (headless browser)
//            userAgent.visit("http://google.com");          //visit google
//            userAgent.doc.apply("butterflies").submit();   //apply form input and submit
//            System.out.println(userAgent.doc.getUrl());
//            Elements links = userAgent.doc.findEvery("<a>");  //find search result links
//            for(Element link : links) System.out.println(link.getAt("href"));   //print results
//        }catch (Exception e){
//            e.printStackTrace();
//        }
    }

    public static ArrayList<Thread> getRunningThreads() {
        return threads;
    }
    public static ArrayList<String> getUnprocessedLinks() {
        return unprocessedLinks;
    }


}
