package com.apt;

import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Ranker {

    private static final double EPSILON = 1;
    ConcurrentHashMap<ObjectId, PageResult> network;
    private static HashMap<ObjectId, PageResult> staticNetwork;
    private static PageResult[] staticValues;
    ConcurrentHashMap<ObjectId, PageResult> pageResults;
    private ArrayList<PageResult> resultsList;

    //Note: The same index that points to a value is used in the adjacency matrix...
    PageResult[] values;
    boolean[][] mat;
    public Ranker() {
        System.out.println("Started initialization");
        staticNetwork = initialize();
        System.out.println("Finished Initialization");
        staticValues = generateNetworkArray();
        System.out.println("Started PR computation, Will take around 5 minutes!");
        computePRStatic();
        System.out.println("Finished PR computation");
    }


    public CopyOnWriteArrayList<PageResult> getResultsList() {
        CopyOnWriteArrayList copyOnWriteArrayList = new CopyOnWriteArrayList<>();
        for (PageResult result: resultsList) {
            copyOnWriteArrayList.add(result);
        }
        return copyOnWriteArrayList;
    }

    private static void setScore(CopyOnWriteArrayList<PageResult> pageResults, String countryCode) {
        Iterator<PageResult> iterator = pageResults.iterator();
        while (iterator.hasNext()){
            PageResult entry = iterator.next();
            double combinedScore = (5 * entry.getTfIDF()) + entry.getPR();
            if(countryCode.equals(entry.getCountryCode())){
                combinedScore += 0.01 * combinedScore;
            }
            String pubDateType = entry.getPubDate().getClass().toString();
            long pubDate;
            if(pubDateType.contains("Integer")){
                pubDate = ((Integer) entry.getPubDate()).longValue();
            }else{
                pubDate = (long) entry.getPubDate();
            }
            if(pubDate > 0){
                Date currentDate = new Date();
                combinedScore += (((double)currentDate.getTime() - pubDate)
                        / (currentDate.getTime() * 100)) * 0.1;
            }
            entry.setCombinedScore(combinedScore * 10000000);
        }
    }

    private void generateNetwork(PageResult page) {
        if(page == null){
            Iterator<PageResult> iterator = this.network.values().iterator();
            while (iterator.hasNext()){
                generateNetwork(iterator.next());
            }
        }else{
            network.put(page.getDocumentID(), page);
            Iterator entries = DBManager.getInstance().getPagesPointingTo(page.getLink()).iterator();
            while(entries.hasNext()){
                Document entry = (Document) entries.next();
                ObjectId oId = entry.getObjectId("_id");
                if(network.containsKey(oId)){
                    continue;
                }else{
                    PageResult p = new PageResult(oId, -1);
                    p.setDocument(entry);
                    generateNetwork(p);
                }
            }
        }
    }

    private static HashMap<ObjectId, PageResult> initialize(){
        //Get all documents in the database...
        HashMap<ObjectId, PageResult> initialNetwork = new HashMap<>();
        MongoCursor<Document> entries = DBManager.getInstance().getAllPages();
        while (entries.hasNext()) {
            Document entry = entries.next();
            ObjectId oId = entry.getObjectId("_id");
            PageResult p = new PageResult(oId, -1);
            p.setDocument(entry);
            initialNetwork.put(p.getDocumentID(), p);
        }
        return  initialNetwork;
    }

    private static String[] getUrlArray(ArrayList<Document> pointers) {
        String[] urls = new String[pointers.size()];
        for (int i = 0; i < pointers.size(); i++) {
            urls[i] = pointers.get(i).getString("pageLink");
        }
        return urls;
    }

    private static boolean contains(ArrayList<String> arr, String url){
        for (String _url: arr) {
            if(_url.equals(url)) return true;
        }
        return false;
    }
    private static PageResult[] generateNetworkArray() {
        return staticNetwork.values().toArray(PageResult[]::new);
    }
    private void generateAdjacencyMatrix() {
        //Create a network.length() x network.length() matrix;
        this.values = network.values().toArray(PageResult[]::new);
//        int size = values.length;
//        mat = new boolean[size][size];
//        for (int i = 0; i < size; i++) {
//            ArrayList<Document> pointers = values[i].getPointingTo();
//            //Get the urls...
//            String[] Urls = getUrlArray(pointers);
//            for (int j = 0; j < size; j++) {
//                if(i == j){
//                    mat[i][j] = false;
//                    continue;
//                }
//                //Get the pages that the current page point to..
//                if(contains(Urls, values[j].getLink())){
//                    mat[i][j] = true;
//                }
//            }
//        }
    }

    private static void computePRStatic() {
        //Initialize page ranks...
        for (int i = 0; i < staticValues.length; i++) {
            staticValues[i].setPR((double)1 / staticValues.length);
        }
//        double currentEpsilon = 100;
//        double currentPR = (double)1 / staticValues.length;
        //Iterate for 5 times is enough.. (For our use case)
        int counter = 0;
        while(counter++ < 5){
//            double PRSum = 0;
            for (int i = 0; i < staticValues.length; i++) {
                double PR = 0;

                Iterator entries = DBManager.getInstance().getPagesPointingTo(staticValues[i].getLink()).iterator();
                while(entries.hasNext()){
                    Document entry = (Document) entries.next();
                    ObjectId oId = entry.getObjectId("_id");
                    PR += staticNetwork.get(oId).getPR() / staticNetwork.get(oId).getPointingToHashMap().size();
                }
//                for (int j = 0; j < staticValues.length; j++) {
//                    if(i == j) continue;
//                    Boolean exists = staticValues[j].getPointingToHashMap().get(staticValues[i].getLink());
//                    if(exists != null && exists == true){
//                        PR += staticValues[j].getPR() / staticValues[j].getPointingToHashMap().size();
//                    }
//                    if(staticValues[j].getPointingToHashMap().containsKey(staticValues[i].getLink())){
//                    }
//                    if(contains(staticValues[j].getPointingToHashMap(), staticValues[i].getLink())) {
//                    }
//                }
                staticValues[i].setPR(PR);
//                PRSum += PR;
            }
//            PRSum /= staticValues.length;
//            currentEpsilon = Math.abs(PRSum - currentPR) / currentPR * 100;
//            currentPR = PRSum;
        }
    }

    //This uses the simplified PageRank algorithm not the actual one with the damping factor.
    private void computePR(){
        //Initialize page ranks...
        for (int i = 0; i < values.length; i++) {
            values[i].setPR((double)1 / values.length);
        }
        double currentEpsilon = 1;
        double currentPR = (double)1 / values.length;
        while(currentEpsilon > EPSILON){
            double PRSum = 0;
            for (int i = 0; i < values.length; i++) {
                double PR = 0;
                for (int j = 0; j < values.length; j++) {
                    if(i == j) continue;
                    if(contains(values[j].getPointingTo(), values[i].getLink())) {
                        PR += values[j].getPR() / values[j].getPointingTo().size();
                    }
                }
                values[i].setPR(PR);
                PRSum += PR;
            }
            PRSum /= values.length;
            currentEpsilon = Math.abs(PRSum - currentPR);
            currentPR = PRSum;
        }
    }

    //Should rank according to popularity & relevance..
    public static CopyOnWriteArrayList<PageResult> rank(ConcurrentHashMap<ObjectId, PageResult> pageResults, String countryCode){
        CopyOnWriteArrayList<PageResult> resultsList = new CopyOnWriteArrayList<>();
        //Filter the results...
        Iterator<PageResult> iterator = pageResults.values().iterator();
//        for (PageResult result: pageResults) {
//            resultsList.add(staticNetwork.get(result.getDocumentID()));
//        }
        while(iterator.hasNext()){
            PageResult result = iterator.next();
//            PageResult pageResult = new PageResult(oId, entry.getDouble("tfIdf"));
            result.setPR(staticNetwork.get(result.getDocumentID()).getPR());
            resultsList.add(result);
        }
//        resultsList.addAll(this.pageResults.values());
        setScore(resultsList, countryCode);
        try{
            Collections.sort(resultsList, new ScoreComparator());
        }catch (Exception e){
            Comparator<PageResult> comparator = new ScoreComparator();
            for (PageResult p1: resultsList) {
                for (PageResult p2: resultsList) {
                    if(comparator.compare(p1,p2) != -comparator.compare(p2,p1)){
                        System.out.println("Loooohl");
                    }
                }
            }
        }
        return resultsList;

    }
//
//    public ArrayList<PageResult> rankPopularity(ArrayList<PageResult> list){
//        Collections.sort(list, new PopularityComparator());
//        return list;
//    }

    private void PR(PageResult page){
//        Iterator entries = DBManager.getInstance().getPagesPointingTo(page.getLink()).iterator();
//        if(page.getPR() == 0) page.setPR(INITIAL_PR);

    }

    private ArrayList<PageResult> calculatePR(ArrayList<PageResult> pages){
        for (PageResult p: pages) {
            //Initial iteration..
            Iterator entries = DBManager.getInstance().getPagesPointingTo(p.getLink()).iterator();

            double PR = 0;
            while(entries.hasNext()){
                Document entry = (Document) entries.next();
//                PR =
            }

        }
        return pages;
    }


    private static class ScoreComparator implements Comparator<PageResult> {

        public int compare(PageResult a, PageResult b) {
            return Double.compare(a.getCombinedScore(), b.getCombinedScore());
        }

    }


}
