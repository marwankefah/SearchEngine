package com.apt;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.bson.Document;
import org.bson.types.ObjectId;
import com.mongodb.client.MongoCursor;
import edu.stanford.nlp.process.Stemmer;
public class Indexer {
	static Hashtable<String,Boolean> hashMap;
	public static void main(String[] args) throws FileNotFoundException {
		// TODO Auto-generated method stub
		// getting hash map for the stopWprds
		hashMap= new Hashtable<String, Boolean>();
		hashMap=Utils.getStopWords("stopwords_en.txt");
		System.out.println(hashMap);
		// hashMap.get("Example")!= null O(1)
		
		
		MongoCursor<org.bson.Document> documentCursor =DBManager.getInstance().getCrawledDocuments();
		
		if(documentCursor!=null)
		{
			
			long documentCount=DBManager.getInstance().getDocumentsCount();
				System.out.println(documentCount);
				try {
				    while (documentCursor.hasNext()) {
				    	Document document=documentCursor.next();
				    	ObjectId doc_id=(ObjectId) document.get("_id");
				    	List<String>  pageTitle=new LinkedList<String>(Arrays.asList(document.get("pageTitle").toString().split("[^a-zA-Z]+")));
				    	
				    	//TODO CHECK if you want to add wait for description
				    	List<String> pageDescription=new LinkedList<String>(Arrays.asList(document.get("pageDescription").toString().split("[^a-zA-Z]+")));

				    	List<String> pageContent=new LinkedList<String>(Arrays.asList(document.get("pageContent").toString().split("[^a-zA-Z]+")));
				    	List<String> pageParagraphs=new LinkedList<String>(Arrays.asList(document.get("pageParagraphs").toString().split("[^a-zA-Z]+")));
				    	
				    	preprocessWordList(pageTitle);
				    	preprocessWordList(pageContent);
				    	preprocessWordList(pageDescription);
				    	preprocessWordList(pageParagraphs);
				    	int bodyCount=pageParagraphs.size()+pageContent.size();
				    	int titleCount=pageTitle.size();
				    	iterateThroughWords(pageTitle,doc_id,titleCount,bodyCount,"title",true);

				    	iterateThroughWords(pageDescription,doc_id,titleCount,bodyCount,"title",false);

				    	iterateThroughWords(pageContent,doc_id,titleCount,bodyCount,"body",false);

				    	iterateThroughWords(pageParagraphs,doc_id,titleCount,bodyCount,"body",false);

				       //DBManager.getInstance().updateInvertedWordIndex("mohamed",new ObjectId("5ec38efdcbe89e73c0ec8378"), 0, 0, 0);
				     
			
				    }
				} finally {
					documentCursor.close();
				}
			}
		
		return;
		
	}
	
	public static void preprocessWordList(List<String> list)
	{
		ListIterator <String> i = list.listIterator();
    	while (i.hasNext()) {
    		String temp=i.next();
    		temp=temp.toLowerCase();
    		i.set(temp);
            if(temp.length()<2  || hashMap.get(temp)!= null)
            {
            	i.remove();
            }
         }
    	
	}
	
	public static void iterateThroughWords(List<String> list,ObjectId doc_id,
				int titleCount,int bodyCount,String tf_index,boolean inTitle)
	{
		ListIterator <String> i = list.listIterator();
    	while (i.hasNext()) {
  
    		String temp=i.next();
    			if(DBManager.getInstance().isInvertedPairExist(temp, doc_id)!=null)
    			{
    				DBManager.getInstance().incrementTermFrequency(temp,doc_id,tf_index);
    			}
    			else
    			{
	    			DBManager.getInstance().insertInvertedWordIndex(temp, doc_id, titleCount,bodyCount, inTitle);
    			}	
    	}	
    }
		
	
	

}
