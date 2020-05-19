package com.apt;

import com.mongodb.client.*;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;

public class DBManager {


    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> processedPagesCollection;
    private MongoCollection<Document> unprocessedLinksCollection;

    private static DBManager instance;

    private DBManager() {

        this.mongoClient = MongoClients.create();
        this.database = mongoClient.getDatabase("dex");
        this.processedPagesCollection = database.getCollection("processed-pages");
        this.unprocessedLinksCollection = database.getCollection("unprocessed-links");
    }

    public void addProcessedPage(Page page){
        Document processedPage = new Document("_id", new ObjectId());
        processedPage.append("pageLink", page.getOrigin());
        processedPage.append("pageTitle", page.getPageTitle());
        processedPage.append("pageDescription", page.getPageDescription() == null ? "" : page.getPageDescription());
        processedPage.append("pageContent", page.getPageContent());
        this.processedPagesCollection.insertOne(processedPage);
    }

    public void addUnprocessedLink(String link) {
        Document unprocessedLink = new Document("_id", new ObjectId());
        unprocessedLink.append("url", link);
        this.unprocessedLinksCollection.insertOne(unprocessedLink);
    }

    public ArrayList<String> getUnprocessedLinks() {
        //Get all data and then delete it...
        MongoCursor<Document> cursor = this.unprocessedLinksCollection.find().iterator();
        ArrayList<String> links = new ArrayList<>();
        while(cursor.hasNext()){
            links.add(cursor.next().get("url").toString());
        }


        this.unprocessedLinksCollection.drop();

        return links;
    }


    public static DBManager getInstance() {
        if(instance == null) {
            instance = new DBManager();
        }
        return instance;
    }

    //This is deprecated, however I can't find another way
    // to close the connection to DB upon exiting...
//    @Override
//    protected void finalize() throws Throwable {
//        super.finalize();
//        this.mongoClient.close();
//    }
}
