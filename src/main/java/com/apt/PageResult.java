package com.apt;

import org.bson.Document;
import org.bson.types.ObjectId;

import javax.print.Doc;
import java.util.ArrayList;
import java.util.List;

public class PageResult {
    ObjectId documentID;
    double tfIDF;
    ArrayList<String> tokens;
    Document doc;
    double PR = 0;
    double combinedScore;

    public double getCombinedScore() {
        return combinedScore;
    }

    public void setCombinedScore(double combinedScore) {
        this.combinedScore = combinedScore;
    }

    ArrayList<String> pointingTo = null;
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
}
