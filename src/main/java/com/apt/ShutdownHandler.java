package com.apt;

import java.util.ArrayList;

public class ShutdownHandler implements Runnable {

    public ShutdownHandler() {
    }

    @Override
    public void run() {
        System.out.println("Exiting...");
        ArrayList<Thread> threads = Crawler.getRunningThreads();
        for (Thread thread: threads) {
            thread.interrupt();
        }

        for(String link : Crawler.getUnprocessedLinks()) {
            DBManager.getInstance().addUnprocessedLink(link);
        }
    }
}
