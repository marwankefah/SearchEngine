package com.apt;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class ImageResult {

    String documentID;
    double tfIDF;
    ArrayList<String> tokens;
    Document doc;



    public ImageResult(String documentID, double tfIDF) {
        this.documentID = documentID;
        this.tfIDF = tfIDF;
        tokens = new ArrayList<>();
    }

    public void setDocument(Document doc) {
        this.doc = doc;
    }

    public String getLink() {
        return this.doc.getString("src");
    }

    public String getDescription() {
        return this.doc.getString("description");
    }

    public String getDocumentID() {
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
