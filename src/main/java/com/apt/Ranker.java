package com.apt;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Ranker {

    private final double EPSILON = 0.001;
    private final double PopularityFactor = 1000;
    private final double dampingFactor = 0.15;
    ConcurrentHashMap<ObjectId, PageResult> network;
    ConcurrentHashMap<ObjectId, PageResult> pageResults;
    private ArrayList<PageResult> resultsList;
    //Note: The same index that points to a value is used in the adjacency matrix...
    PageResult[] values;
    boolean[][] mat;
    public Ranker(ConcurrentHashMap<ObjectId, PageResult> pageResults) {
        this.network = pageResults;
        this.pageResults = new ConcurrentHashMap<>();
        Iterator<PageResult> list = pageResults.values().iterator();
        while(list.hasNext()){
            PageResult entry = list.next();
            this.pageResults.put(entry.getDocumentID(), entry);
        }
        //build our network....
        this.generateNetwork(null);
        this.generateAdjacencyMatrix();
        this.computePR();
        this.setScore();
        this.rank();
    }


    public ArrayList<PageResult> getResultsList() {
        return resultsList;
    }

    private void setScore() {
        Iterator<PageResult> iterator = this.pageResults.values().iterator();
        while (iterator.hasNext()){
            PageResult entry = iterator.next();
            double combinedScore = entry.getTfIDF() + entry.getPR();
            entry.setCombinedScore(combinedScore);
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
                if(network.containsKey(entry.getObjectId("_id"))){
                    continue;
                }else{
                    PageResult p = new PageResult(entry.getObjectId("_id"), -1);
                    p.setDocument(entry);
                    generateNetwork(p);
                }
            }
        }
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
    private void rank(){
        this.resultsList = new ArrayList<>();
        resultsList.addAll(this.pageResults.values());
        Collections.sort(resultsList, new ScoreComparator());

    }

    public ArrayList<PageResult> rankPopularity(ArrayList<PageResult> list){
        Collections.sort(list, new PopularityComparator());
        return list;
    }

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
    private static class PopularityComparator implements Comparator<PageResult> {

        public int compare(PageResult a, PageResult b) {
            double subtraction = a.getTfIDF() - b.getTfIDF();
            if(subtraction > 0){
                return -1;
            }else if(subtraction < 0){
                return 1;
            }
            return 0;
        }

    }

    private static class ScoreComparator implements Comparator<PageResult> {

        public int compare(PageResult a, PageResult b) {
            double subtraction = a.getTfIDF() - b.getTfIDF();
            if(subtraction > 0){
                return -1;
            }else if(subtraction < 0){
                return 1;
            }
            return 0;
        }

    }


}
