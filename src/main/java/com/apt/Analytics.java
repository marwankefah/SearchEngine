package com.apt;

import io.advantageous.boon.core.Sys;
import org.loadtest4j.LoadTester;
import org.loadtest4j.Request;
import org.loadtest4j.Result;
import org.loadtest4j.drivers.gatling.GatlingBuilder;
import org.loadtest4j.factory.LoadTesterFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Analytics {

    private static final String BASE_URI = "http://localhost:8080";
    private static final long NUM_SECONDS = 1;

    protected static ArrayList<TestingClass> tests = new ArrayList<>();

    static LoadTester getLoadTester(String uri) {
        return LoadTesterFactory.getLoadTester();
    }

    private static final class SearchLoadTester {

//        private static final String URL = BASE_URI + "search?q=travelling&cc=EG";

        public static void test(String URL, String introMsg) {
            System.out.println(introMsg);
            tests.clear();
            long startTime = System.nanoTime();
            long currentTime = System.nanoTime();
            ArrayList<Thread> threads = new ArrayList<>();
//            while ((currentTime - startTime) / 1000000000 < NUM_SECONDS) {
                TestingClass test = new TestingClass(URL);
                threads.add(new Thread(test));
                threads.get(threads.size() - 1).start();
                tests.add(test);
                currentTime = System.nanoTime();
//            }
            double totalTimeTaken = 0;
            long numSuccess = 0;
            for (int i = 0; i < tests.size(); i++) {
                if(!tests.get(i).didFinish) {
                    threads.get(i).interrupt();
                    continue;
                };;
                if(tests.get(i).didFail) continue;;
                totalTimeTaken += tests.get(i).timeTaken;
                numSuccess += 1;
            }
            System.out.println("In " + NUM_SECONDS + " seconds, " +
                    "#successRequests = " + numSuccess +
                    " & avg. time taken: " + totalTimeTaken / numSuccess);
        }


    }
    private static class TestingClass implements Runnable {
        public double timeTaken = 0;
        private String url;
        public boolean didFail = false;
        public boolean didFinish = false;
        public TestingClass(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            try{
                timeTaken = System.nanoTime();
                String data = Utils.downloadURLData(url);
                timeTaken = ((double)System.nanoTime() - timeTaken) / 1000000000;
                didFinish = true;
                if(data == null){
                    didFail = true;
                    return;
                }
                if(!data.startsWith("{")) didFail = true;
            }catch (Exception e){

            }

        }
    }

    public static void main(String[] args) {
        SearchLoadTester.test(BASE_URI + "/search?q=travelling&cc=EG", "Search Stats");
        SearchLoadTester.test(BASE_URI + "/search?q=travelling&cc=EG&img=on", "Image Search Stats");
    }

}
