package com.apt;

import org.bson.Document;
import org.bson.types.ObjectId;

import javax.print.Doc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PageResult {
    ObjectId documentID;
    double tfIDF;
    ArrayList<String> tokens;
    Document doc;
    double PR = 0;
    double combinedScore;
    private ArrayList<ObjectId> pagesPointingToThis;
    public double getCombinedScore() {
        return combinedScore;
    }

    public void setCombinedScore(double combinedScore) {
        this.combinedScore = combinedScore;
    }

    ArrayList<String> pointingTo = null;
    HashMap<String, Boolean> pointingToHashmap = new HashMap<>();

    public double getPR() {
        return PR;
    }

    public void setPR(double PR) {
        this.PR = PR;
    }

    public PageResult(ObjectId documentID, double tfIDF) {
        this.documentID = documentID;
        this.tfIDF = tfIDF;
        tokens = new ArrayList<>();
    }

    public void setDocument(Document doc) {
        this.doc = doc;
        //Generate the pointing hashmap earlier...
        List<String> pageLinks = (List<String>) this.doc.get("pageLinks");
        for (String link: pageLinks) {
            if(link == null) continue;;
            this.pointingToHashmap.put(link, true);
        }
    }

    public void addPagePointingToMe(ObjectId oId) {
        if(this.pagesPointingToThis == null) this.pagesPointingToThis = new ArrayList<>();
        this.pagesPointingToThis.add(oId);
    }

    public boolean isPointingPagesSet() {
        return this.pagesPointingToThis != null;
    }

    public ArrayList<ObjectId> getPagesPointingToThis() {
        return this.pagesPointingToThis;
    }

    public ArrayList<String> getPointingTo() {
        if(this.pointingTo != null) return this.pointingTo;
        this.pointingTo = new ArrayList<>();
        List<String> pageLinks = (List<String>) this.doc.get("pageLinks");
        for (String link: pageLinks) {
            if(link == null) continue;;
            this.pointingTo.add(link);
        }
        return this.pointingTo;
    }


//    public void addLink(Document doc){
//        pointingTo.add(doc);
//    }

    public String getLink() {
        return this.doc.getString("pageLink");
    }

    public String getPageTitle() {
        return this.doc.getString("pageTitle");
    }

    public String getPageContent() {
        return this.doc.getString("pageContent");
    }

    public ObjectId getDocumentID() {
            return documentID;
    }

    public double getTfIDF() {
        return tfIDF;
    }

    public ArrayList<String> tokens() {
        return tokens;
    }

    public void sumIDF(double num) {
        this.tfIDF += num;
    }

    public void addToken(String token) {
        tokens.add(token);
    }
    public HashMap<String, Boolean> getPointingToHashMap() {
        return this.pointingToHashmap;
    }

    public String getCountryCode() {
        if(this.doc == null) return "";
        return this.doc.getString("countryCode");
    }


    public Object getPubDate() {
        if(this.doc == null) return 0;
        return this.doc.get("pubDate");
    }
}
