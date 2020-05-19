package com.apt;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.IndexOptions;

import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;


public class DBManager {


    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> collection;
    private MongoCollection<Document> invertedCollection;

    private static DBManager instance;

    private DBManager() {

        this.mongoClient = MongoClients.create();
        this.database = mongoClient.getDatabase("dex");
        this.collection = database.getCollection("processed-pages");
        this.invertedCollection = database.getCollection("inverted-index");
        this.invertedCollection.createIndex(Indexes.ascending("token"),new IndexOptions().unique(true));
        this.invertedCollection.createIndex(Indexes.ascending("Documents._id"),new IndexOptions().unique(true));
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
    
    public void updateInvertedWordIndex(String word,ObjectId doc_id,int tf,int idf,int tf_idf)
    {
    	  
    	if(this.collection.find(Filters.eq("_id",doc_id)).limit(1)!= null)
    	{
    		UpdateOptions x=new UpdateOptions();
    		Document subDoc= new Document("_id",doc_id);
    		subDoc.append("tf",tf);
    		subDoc.append("idf", idf);
    		subDoc.append("tf_idf",tf_idf);
    		this.invertedCollection.updateOne(Filters.eq("token",word), Updates.push("Documents", subDoc)
    				,new UpdateOptions().upsert(true));	
    	}
    	else
    	{
    		System.out.println("Error in DB Manager Funtion UpdateInvertedWordindex"
    				+" invalid doc_id");
    	}

    	
    }
    
    public MongoCursor<Document> getCrawledDocuments()
    {	
    	return this.collection.find(Filters.exists("_id")).iterator();
    	
    }
    
    public long getDocumentsCount()
    {
    	
    	return this.collection.estimatedDocumentCount();
    	
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
