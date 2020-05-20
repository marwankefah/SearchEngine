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

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.client.*;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class DBManager {


    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> collection;
    private MongoCollection<Document> invertedCollection;
    private MongoCollection<Document> processedPagesCollection;
    private MongoCollection<Document> unprocessedLinksCollection;
    private MongoCollection<Document> imagesCollection;

    private static DBManager instance;

    private DBManager() {

        this.mongoClient = MongoClients.create();
        this.database = mongoClient.getDatabase("dex");
        this.collection = database.getCollection("processed-pages");
        this.invertedCollection = database.getCollection("inverted-index");
        this.invertedCollection.createIndex(Indexes.ascending("token"),new IndexOptions().unique(true));
        this.invertedCollection.createIndex(Indexes.ascending("Documents._id"),new IndexOptions().unique(true));
        this.processedPagesCollection = database.getCollection("processed-pages");
        this.unprocessedLinksCollection = database.getCollection("unprocessed-links");
        this.imagesCollection = database.getCollection("images");
    }

    public void addProcessedPage(Page page){
        try{
            Document processedPage = new Document("_id", new ObjectId());
            processedPage.append("pageLink", page.getOrigin());
            processedPage.append("pageTitle", page.getPageTitle());
            processedPage.append("pageContent", page.getPageContent());
            processedPage.append("pageDescription", page.getPageDescription() == null ? "" : page.getPageDescription());
            BasicDBList paragraphList = new BasicDBList();
            for (String paragraph: page.getParagraphs()) {
                paragraphList.add(paragraph);
            }
            processedPage.append("pageParagraphs", paragraphList);
            BasicDBList headersList = new BasicDBList();
            for (Page.Header header: page.getHeaders()) {
                BasicDBObject headerObject = new BasicDBObject();
                headerObject.append("type", header.getType());
                headerObject.append("content", header.getContent());
                headersList.add(headerObject);
            }
            this.processedPagesCollection.insertOne(processedPage);
            saveImages(page.getImages());
        }catch (Exception e){

        }

    }

    private void saveImages(ArrayList<Image> images){
        for (Image image: images) {
            try {
                Document imageDocument = new Document("_id", image.getSrc());
                imageDocument.append("description", image.getPlaceholder());
                imageDocument.append("src", image.getSrc());
                this.imagesCollection.insertOne(imageDocument);
            }catch (Exception e){
                //Catch any exception that may happen from _id duplication.
                //We need this in order to not store the same image more than once.
                //By the "same image" I mean the same URL but not the actual bits of the image.
//                System.out.println("Error occured when trying to save image with URL: " + image.getSrc());
            }
        }

    }

    public void saveUnprocessedLinks(List<String> links){
        for (int i = 0; i < links.size(); i++) {
            if(links.get(i) == null) continue;
            this.addUnprocessedLink(links.get(i));
        }
    }

    public void deleteUnprocessedLinks(ArrayList<String> links, int endIndex){
        for (int i = 0; i <= endIndex; i++) {
            this.removeUnprocessedLink(links.get(i));
        }
    }

    public void removeUnprocessedLink(String link) {
        Document doc = new Document("_id", link);
        doc.append("url", link);
        this.unprocessedLinksCollection.findOneAndDelete(doc);
    }

    public void addUnprocessedLink(String link) {
        if(link == null) return;
        try{
            Document unprocessedLink = new Document("_id", link);
            unprocessedLink.append("url", link);
            this.unprocessedLinksCollection.insertOne(unprocessedLink);

        }catch (Exception e){

        }
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
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.mongoClient.close();
    }
}
