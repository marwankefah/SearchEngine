package com.apt;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.IndexOptions;

import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import com.thoughtworks.qdox.directorywalker.Filter;
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
	private MongoCollection<Document> documentTokenCollection;

    private static DBManager instance;

    private DBManager() {

        this.mongoClient = MongoClients.create();
        this.database = mongoClient.getDatabase("dex");
        this.invertedCollection = database.getCollection("inverted-index");

        this.invertedCollection.createIndex(Indexes.ascending("token"));
        this.invertedCollection.createIndex(Indexes.ascending("token","DocumentId"),new IndexOptions().unique(true));
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
            processedPage.append("bodyCount", -1);
            processedPage.append("titleCount", -1);

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
   
    public Boolean updateIdfWordIndex(String word,ObjectId doc_id,int idf)
    {
    	if(this.invertedCollection.find(Filters.and(Filters.eq("token", word),
				Filters.eq("Documents.refid",doc_id))) !=null)
			{
			
    			return true;
			}
    	return false;
    	
    }
    public boolean incrementTermFrequency(String word,ObjectId doc_id,String index)
    {
    	UpdateResult x=null;
    	if(index=="title")
    	{
		 x=this.invertedCollection.updateOne(Filters.and(Filters.eq("token", word),
				Filters.eq("Documents.refid",doc_id))
				,new BasicDBObject("$inc", new BasicDBObject("Documents.$.tfTitle", 1)));  
    	}
    	else if(index=="body")
    	{
    		
   		 x=this.invertedCollection.updateOne(Filters.and(Filters.eq("token", word),
 				Filters.eq("Documents.refid",doc_id))
 				,new BasicDBObject("$inc", new BasicDBObject("Documents.$.tfBody", 1))); 
    		
    	}
    	else
    	{
    		return false;
    	}
    	
    	return x.wasAcknowledged();
    	
    }
    
    public int getTermFrequency(String word,ObjectId doc_id,String index)
    {
		Document doc= this.isInvertedPairExist(word, doc_id);
		if(doc!=null)
		{
			if(index=="title")
			{
				return doc.getInteger("tfTitle", -2);
			}
			else if(index=="body")
			{
				return doc.getInteger("tfBody", -2);
			}			
		}
		return -1;
 
   }
    
    public Document isInvertedPairExist(String word,ObjectId doc_id)
    {
		MongoIterable<Document> cu=this.invertedCollection.find(Filters.and(Filters.eq("token", word),
				Filters.eq("refid",doc_id)));
    	
		if(cu!=null)
		{
			return cu.first();
			
		}
		else
		{
			return null;
		}
    	
    }
    
    public boolean insertInvertedWordIndex(String word,ObjectId doc_id,int titleCount,int bodyCount,int inTitle)
    {
    	if(this.processedPagesCollection.find(Filters.eq("_id",doc_id)).limit(1)!= null)
    	{
  

    		Document Doc= new Document();
    		Doc.append("bodyCount",bodyCount);
    		Doc.append("titleCount",titleCount);
    	    BasicDBObject setNewFieldQuery = new BasicDBObject().append("$set", new BasicDBObject(Doc));
    		this.processedPagesCollection.updateOne(Filters.eq("_id",doc_id), setNewFieldQuery);

    		UpdateOptions x=new UpdateOptions();
    		Document updateQuery= new Document();
    		if(inTitle==1)
    		{
    			updateQuery.append("$inc", new BasicDBObject("tfTitle", 1));
    			updateQuery.append("$setOnInsert", new BasicDBObject("tfBody", 1));
    		}
    		else
    		{
    			updateQuery.append("$inc", new BasicDBObject("tfBody",1));
    			updateQuery.append("$setOnInsert", new BasicDBObject("tfTitle", 1));
    		}

    		return this.invertedCollection.updateOne(Filters.and(Filters.eq("token", word),
    				Filters.eq("DocumentId",doc_id))
    				,updateQuery
    				,new UpdateOptions().upsert(true)).wasAcknowledged();
    	}
    	else
    	{
    		System.out.println("Error in DB Manager Funtion UpdateInvertedWordindex"
    				+" invalid doc_id");
    		return false;
    	}
    }
    
    


    public void setIndexed(ObjectId doc_id,boolean isIndexed)
    {
    	
     this.processedPagesCollection.findOneAndUpdate(Filters.eq("_id",doc_id)
    		, new BasicDBObject("$set",new BasicDBObject("Indexed", isIndexed)));
     return;
    	
    }

    public MongoCursor<Document> getCrawledDocuments()
    {
    	return this.processedPagesCollection.find(Filters.exists("_id")).iterator();

    }

    public long getDocumentsCount()
    {

    	return this.processedPagesCollection.estimatedDocumentCount();

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
