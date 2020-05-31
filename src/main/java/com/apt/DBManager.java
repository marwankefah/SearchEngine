package com.apt;

import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.IndexOptions;

import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.client.*;

import javax.print.Doc;
import java.beans.Expression;
import java.util.*;
import java.util.regex.Pattern;


public class DBManager {


    private MongoClient mongoClient;
    private MongoDatabase database;
//    private MongoCollection<Document> collection;
    private MongoCollection<Document> invertedCollection;
    private MongoCollection<Document> processedPagesCollection;
    private MongoCollection<Document> unprocessedLinksCollection;
    private MongoCollection<Document> imagesCollection;
//	private MongoCollection<Document> documentTokenCollection;
	private MongoCollection<Document> invertedImgIndexCollection;
    private MongoCollection<Document> nersCollection;
    private MongoCollection<Document> suggestionCollection;
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
        this.nersCollection = database.getCollection("ners");
        this.suggestionCollection = database.getCollection("sugg");
        this.invertedImgIndexCollection=database.getCollection("inverted-imgs");
        this.invertedImgIndexCollection.createIndex(Indexes.ascending("token"));
        this.invertedImgIndexCollection.createIndex(Indexes.ascending("token","imageId"),new IndexOptions().unique(true));

    }


    public Document getProcessedPage(ObjectId oId){
        return this.processedPagesCollection.find(Filters.eq("_id", oId)).first();
    }

    public Document getProcessedImage(String oId){
        return this.imagesCollection.find(Filters.eq("_id", oId)).first();
    }
    public void saveNERs(ArrayList<Pair<String, String>> NERs){
        for (Pair<String, String> pair: NERs) {
            Document nerEntry = new Document("_id", new ObjectId());
            nerEntry.append("value", pair.first);
            nerEntry.append("cc", pair.second);
            this.nersCollection.insertOne(nerEntry);
        }
    }

    public void addProcessedPage(Page page){
        if(page.getInvalid()) return;
        if(page.getPageTitle() == null || page.getPageTitle().length() == 0) return;
        if(page.getPageContent() == null || page.getPageContent().length() == 0) return;
        try{
            Document processedPage = new Document("_id", new ObjectId());
            processedPage.append("pageLink", page.getPageOriginalLink());
            processedPage.append("pageTitle", page.getPageTitle());
            processedPage.append("pageContent", page.getPageContent());
            processedPage.append("pageDescription", page.getPageDescription() == null ? "" : page.getPageDescription());
            processedPage.append("bodyCount", -0.2);
            processedPage.append("titleCount",-0.2);
            if(page.getPubDate() != null){
                processedPage.append("pubDate", page.getPubDate().getTime());
            }else{
                processedPage.append("pubDate", 0);
            }
            processedPage.append("countryCode", page.getCountryCode());
            BasicDBList paragraphList = new BasicDBList();
            for (String paragraph: page.getParagraphs()) {
                paragraphList.add(paragraph);
            }
            processedPage.append("pageParagraphs", paragraphList);
            BasicDBList linksList = new BasicDBList();
            for (String link: page.getLinks()) {
                linksList.add(link);
            }
            processedPage.append("pageLinks", linksList);

            BasicDBList headersList = new BasicDBList();
            for (Page.Header header: page.getHeaders()) {
                BasicDBObject headerObject = new BasicDBObject();
                headerObject.append("type", header.getType());
                headerObject.append("content", header.getContent());
                headersList.add(headerObject);
            }
            processedPage.append("pageHeaders", headersList);
            this.processedPagesCollection.insertOne(processedPage);
            saveImages(page.getImages());
        }catch (Exception e){

        }

    }

    public void addSuggestion(String query){
        try{
            Document doc = new Document("_id", query);
            doc.append("q", query);
            this.suggestionCollection.insertOne(doc);
        }catch (Exception e){

        }
    }

    private void saveImages(ArrayList<Image> images){
        for (Image image: images) {
            try {
                Document imageDocument = new Document("_id", image.getSrc());
                String alt = image.getAlt();
                if(alt == null || alt.trim().length() == 0){
                    continue;
                }
                imageDocument.append("description", alt);
                imageDocument.append("src", image.getSrc());
                imageDocument.append("descriptionCount",-0.2);
                this.imagesCollection.insertOne(imageDocument);
            }catch (Exception e){
                //Catch any exception that may happen from _id duplication.
                //We need this in order to not store the same image more than once.
                //By the "same image" I mean the same URL but not the actual bits of the image.
//                System.out.println("Error occured when trying to save image with URL: " + image.getSrc());
            }
        }

    }



    public MongoIterable<Document> getImageTokenEntries(String token){
        return this.invertedImgIndexCollection.find(Filters.eq("token", token));
    }
    public MongoIterable<Document> getTokenEntries(String token){
        return this.invertedCollection.find(Filters.eq("token", token));
    }

    public MongoIterable<Document> getPagesPointingTo(String link) {
        String[] values = {link};
        return this.processedPagesCollection.find(
                Filters.in(
                        "pageLinks",
                        Arrays.asList(values)
                ));
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
//    public void dropUnprocessed() {
//        this.unprocessedLinksCollection.drop();
//    }

    public Boolean updateIdfWordIndex(String word,ObjectId doc_id,int idf)
    {
        if(this.invertedCollection.find(Filters.and(Filters.eq("token", word),
                Filters.eq("Documents.refid",doc_id))) !=null)
        {

            return true;
        }
        return false;

    }

    public void dropDB(){
        this.processedPagesCollection.drop();
        this.invertedCollection.drop();
        this.unprocessedLinksCollection.drop();
        this.imagesCollection.drop();
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
				return doc.getInteger("tftitle", -2);
			}
			else if(index=="body")
			{
				return doc.getInteger("tfbody", -2);
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
    
 public boolean insertInvertedWordIndexImages(String word,String doc_id,double descriptionCount)
    {
    	if(this.imagesCollection.find(Filters.eq("_id",doc_id)).limit(1)!= null)
    	{
  

    		Document Doc= new Document();
    		Doc.append("descriptionCount",descriptionCount);
    	    BasicDBObject setNewFieldQuery = new BasicDBObject().append("$set", new BasicDBObject(Doc));
    		
    	    this.imagesCollection.updateOne(Filters.eq("_id",doc_id), setNewFieldQuery);

    		UpdateOptions x=new UpdateOptions();
    		Document updateQuery= new Document();

    		updateQuery.append("$inc", new BasicDBObject("tfDescription", 1.0));
    		updateQuery.append("$setOnInsert", new BasicDBObject("tfIdf",0.0)
    				.append("normalized", false));

    		return this.invertedImgIndexCollection.updateOne(Filters.and(Filters.eq("token", word),
    				Filters.eq("imageId",doc_id))
    				,updateQuery
    				,new UpdateOptions().upsert(true)).wasAcknowledged();
    	}
    	else
    	{
    		System.out.println("Error in DB Manager Funtion images UpdateInvertedWordindex"
    				+" invalid doc_id");
    		return false;
    	}
    }
    
    public boolean insertInvertedWordIndex(String word,ObjectId doc_id,double titleCount,double bodyCount,int inTitle)
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

    			updateQuery.append("$inc", new BasicDBObject("tfTitle", 1.0));
    			updateQuery.append("$setOnInsert", new BasicDBObject("tfBody", 0.0)
    					.append("tfIdf",0.0).append("normalized", false));
    		}
    		else
    		{

    			updateQuery.append("$inc", new BasicDBObject("tfBody",1.0));
    			updateQuery.append("$setOnInsert", new BasicDBObject("tfTitle", 0.0)
    					.append("tfIdf",0.0).append("normalized", false));
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
    public void setIndexedImgs(String doc_id,boolean isIndexed)
    {
    	
     this.imagesCollection.findOneAndUpdate(Filters.eq("_id",doc_id)
    		, new BasicDBObject("$set",new BasicDBObject("Indexed", isIndexed)));
     return;
    	
    }


	public void normalizetfTitle()
    {

		// TODO need optimization to get only non normalized documents
		AggregateIterable<Document> documents
        = this.invertedCollection.aggregate(
        Arrays.asList(

        		Aggregates.lookup("processed-pages", "DocumentId", "_id","documentInfo")
        		,
        		Aggregates.match(Filters.eq("normalized",false))
      
        ));

		double totalNumberOfDocs=this.getDocumentsCount();
		for(Document a: documents)
		{
			double bodyCount=(double) ((ArrayList<Document>)a.get("documentInfo")).get(0).get("bodyCount");
			double titleCount=(double) ((ArrayList<Document>)a.get("documentInfo")).get(0).get("titleCount");
			String toknen=a.get("token").toString();
			double normalizedtfBody=(double)a.get("tfBody")/bodyCount;
			double normalizedTfTitle=(double)a.get("tfTitle")/titleCount;
			double df= this.invertedCollection.countDocuments(Filters.eq("token",toknen));
			double tf_idf=Math.log10(totalNumberOfDocs/(df+1))*(normalizedtfBody*0.3+ normalizedTfTitle*0.6);

			BasicDBObject carrier = new BasicDBObject();
			BasicDBObject set = new BasicDBObject("$set", carrier);
			carrier.put("tfIdf",tf_idf);
			carrier.put("normalized",true);
			carrier.put("tfTitle",normalizedTfTitle);
			carrier.put("tfBody",normalizedtfBody);
			ObjectId doc_id=a.getObjectId("DocumentId");
			BasicDBObject query = new BasicDBObject();
			query.put("token",toknen);

			query.put("DocumentId",doc_id);
			query.put("normalized",false);
			this.invertedCollection.updateOne(query, set);
		}


		return;
    }
    public MongoCursor<Document> getCrawledDocuments()
    {
    	return this.processedPagesCollection.find(Filters.and(Filters.exists("_id"),
    			Filters.or(Filters.eq("Indexed",false)
    			,Filters.not(Filters.exists("Indexed"))))
    			).iterator();

    }

    public MongoCursor<Document> getCrawledImages()
    {
    	return this.imagesCollection.find(Filters.and(Filters.exists("_id"),
    			Filters.or(Filters.eq("Indexed",false)
    			,Filters.not(Filters.exists("Indexed"))))
    			).iterator();

    }
    public double getDocumentsCount()
    {

    	return this.processedPagesCollection.estimatedDocumentCount();

    }

    public double getImagesCount()
    {

    	return this.imagesCollection.estimatedDocumentCount();

    }

    public ArrayList<String> getSuggestions(String currentInput){
        ArrayList<String> suggestions = new ArrayList<>();
        Pattern pattern = Pattern.compile(".*" + currentInput + ".*", Pattern.CASE_INSENSITIVE);
        MongoCursor<Document> cursor = this.suggestionCollection.find(Filters.regex("q", pattern)).iterator();
        while(cursor.hasNext()){
            Document suggestion = cursor.next();
            suggestions.add(suggestion.getString("q"));
        }
        return  suggestions;
    }

    public static DBManager getInstance() {
        if(instance == null) {
            instance = new DBManager();
        }
        return instance;
    }

    public ArrayList<Pair<String, Integer>> getTrends(String countryCode){
        MongoCursor<Document> iterator = this.nersCollection.find(
                Filters.eq("cc", countryCode)
        ).iterator();
        HashMap<String, Pair<String, Integer>> trends = new HashMap<>();
        while(iterator.hasNext()){
            Document entry = iterator.next();
            String value = entry.getString("value");
            if(trends.containsKey(value)){
                trends.get(value).setSecond(trends.get(value).second + 1);
            }else{
                Pair<String, Integer> pair = new Pair<>(value, 1);
                trends.put(value, pair);
            }
        }
        ArrayList<Pair<String, Integer>> list = new ArrayList<>();
        list.addAll(trends.values());
        return list;
    }


	public void normalizetfImages() {
		// TODO Auto-generated method stub

		// TODO need optimization to get only non normalized documents
		AggregateIterable<Document> documents
        = this.invertedImgIndexCollection.aggregate(
        Arrays.asList(
        	
        		Aggregates.lookup("images", "imageId", "_id","documentInfo"),
        		Aggregates.match(Filters.eq("normalized",false))
        ));
		
		double totalNumberOfDocs=this.getImagesCount();
		for(Document a: documents)
		{	
			// TODO need optimization
			ArrayList<Document> aggregateArray = ((ArrayList<Document>)a.get("documentInfo"));
	if(aggregateArray.size()>0)
	{
			 Document checkDescription = ((ArrayList<Document>)a.get("documentInfo")).get(0);
			 if(checkDescription.get("description")!=null)
			{
			
			 double descriptionCount=(double) checkDescription.get("descriptionCount");

			String toknen=a.get("token").toString();
			double normalizedTfTitle=(double)a.get("tfDescription")/descriptionCount;
			double df= this.invertedImgIndexCollection.countDocuments(Filters.eq("token",toknen));
			double tf_idf=Math.log10(totalNumberOfDocs/(df+1))*(normalizedTfTitle);
			
			BasicDBObject carrier = new BasicDBObject();
			BasicDBObject set = new BasicDBObject("$set", carrier);
			carrier.put("tfIdf",tf_idf);
			carrier.put("normalized",true); 
			carrier.put("tfDescription",normalizedTfTitle);

			String doc_id=(String)a.get("imageId");
			BasicDBObject query = new BasicDBObject();
			query.put("token",toknen);
			query.put("imageId",doc_id);
			query.put("normalized",false);
			this.invertedImgIndexCollection.updateOne(query, set);
			}
		}
	}
		

		return;
		
	}
	
    //This is deprecated, however I can't find another way
    // to close the connection to DB upon exiting...
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.mongoClient.close();
    }
}
