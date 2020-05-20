package com.apt;


import java.io.FileNotFoundException;
import java.util.Hashtable;

import org.bson.types.ObjectId;

import com.mongodb.client.MongoCursor; 
public class Indexer {
	public static void main(String[] args) throws FileNotFoundException {
		// TODO Auto-generated method stub

		// getting hash map for the stopWprds
	 	Hashtable<String,Boolean> hashMap = new Hashtable<String, Boolean>(); 
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
				        	
				       DBManager.getInstance().updateInvertedWordIndex("mohamed",new ObjectId("5ec38efdcbe89e73c0ec8378"), 0, 0, 0);
				       return;
			
				    }
				} finally {
					documentCursor.close();
				}
			}
		
		return;
		
		
	}

}
