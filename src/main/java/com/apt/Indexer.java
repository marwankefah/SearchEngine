package com.apt;


import org.bson.types.ObjectId;

import com.mongodb.client.MongoCursor; 
public class Indexer {
	public static void main(String[] args) {
		// TODO Auto-generated method stub

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
