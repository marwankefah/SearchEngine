package com.apt;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

public class DBManager {


    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> collection;

    private static DBManager instance;

    private DBManager() {

        this.mongoClient = MongoClients.create();
        this.database = mongoClient.getDatabase("dex");
        this.collection = database.getCollection("processed-pages");
    }

    public void addProcessedPage(Page page){
        Document processedPage = new Document("_id", new ObjectId());
        processedPage.append("pageLink", page.getOrigin());
        processedPage.append("pageTitle", page.getPageTitle());
        processedPage.append("pageDescription", page.getPageDescription() == null ? "" : page.getPageDescription());
        processedPage.append("pageContent", page.getPageContent());
//        processedPage.append("pageLink", page.getOrigin());
//        processedPage.append("pageLink", page.getOrigin());
        this.collection.insertOne(processedPage);
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
