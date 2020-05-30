package com.apt;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import io.advantageous.boon.core.Sys;
import org.bson.Document;
import org.bson.types.ObjectId;
import com.mongodb.client.MongoCursor;
import edu.stanford.nlp.process.Stemmer;
import edu.stanford.nlp.simple.Sentence;


public class Indexer {
	static Hashtable<String,Boolean> hashStopWords=Utils.getStopWords("stopwords_en.txt");;
	static Stemmer s=new Stemmer();
	static String[] docFieldsToIndex= {"pageTitle","pageDescription","pageContent"};
	static String[] ImgsFieldToIndex= {"description"};
   public Indexer()
	{

		
	}
	public static void main(String[] args) throws FileNotFoundException, InterruptedException {
		// TODO Auto-generated method stub

       int imgODoc = Integer.parseInt(Utils.getUserInput("Enter 0 to index documents, 1 for images: "));
		if(imgODoc==0)
		{
		       indexDocuments(docFieldsToIndex);
				DBManager.getInstance().normalizetfTitle();
			
		}
		else if(imgODoc==1)
		{
			// normalize tf and calculate tf_idf
			indexImages(ImgsFieldToIndex);
			DBManager.getInstance().normalizetfImages();
			
			
		}
		else
		{
			System.out.println("INPUT ERROR");
			
		}
	
		return;
	}

	
public static void indexImages(String[] fieldtoIndex)
	{
		// get documents to index
		MongoCursor<org.bson.Document> imagesCursor =DBManager.getInstance().getCrawledImages();
		
		// loop to index
		
		if(imagesCursor!=null)
		{
			double imagesCount=DBManager.getInstance().getImagesCount();
				System.out.println(imagesCursor);
				try {
				    while (imagesCursor.hasNext()) {
				    	
				    	Document document=imagesCursor.next();
				    	if(document.get("description")!=null)
				    	{
				    	//TODO CHECK if you want to add weight for description
				    	Hashtable<String, List<String>> hashImgArr = new Hashtable<String, List<String>>(1);
				    	Hashtable<String, List<String>> hashImgLemmArr = new Hashtable<String, List<String>>(1);
				    	hashImgArr=getDocumentText(getDocumentTextAsString(document,fieldtoIndex));
				    	preprocessDocumentText(hashImgArr,true);
				    	processsImages(document,hashImgArr);
				    	
//				    	hashImgLemmArr=getDocumentText(getDocumentTextAsString(document,fieldtoIndex));
//				    	preprocessDocumentText(hashImgLemmArr,false);
//				    	processsImages(document,hashImgLemmArr);
				    	DBManager.getInstance().setIndexedImgs((String)document.get("_id"),true);
				    	}
				    	}
				} finally {

					imagesCursor.close();
				}
			}
			
		return;
		
		
	}	
	
	
	
public static void indexDocuments(String[] fieldsToIndex)
{
	// get documents to index
	MongoCursor<org.bson.Document> documentCursor =DBManager.getInstance().getCrawledDocuments();
	
	// loop to index
	
	if(documentCursor!=null)
	{
		double documentCount=DBManager.getInstance().getDocumentsCount();
			System.out.println(documentCount);
			try {
				Boolean index=true;
				while (index) {
					try
					{
						index=documentCursor.hasNext();
						if(index==false)
						{
							break;
						}
					}
					catch(Exception e) {
						System.out.println("Exception occured in mongodb");
						documentCursor = DBManager.getInstance().getCrawledDocuments();
						index = true;
						continue;
					}

					Document document = documentCursor.next();
					//TODO CHECK if you want to add weight for description
					Hashtable<String, List<String>> hashDocArr = new Hashtable<String, List<String>>(4);
//					Hashtable<String, List<String>> hashLemmArr = new Hashtable<String, List<String>>(4);

					//STEMS
					hashDocArr = getDocumentText(getDocumentTextAsString(document, fieldsToIndex));
					preprocessDocumentText(hashDocArr, true);
					processDocument(document, hashDocArr);
//
//			    	//LEMMS
//			    	hashLemmArr=getDocumentText(getDocumentTextAsString(document,fieldsToIndex));
//			    	preprocessDocumentText(hashLemmArr,false);
//			    	processDocument(document,hashLemmArr);

					DBManager.getInstance().setIndexed((ObjectId) document.get("_id"), true);
				}
			} finally {

				documentCursor.close();
			}
		}
		
	return;
	
	
}
	public static Hashtable<String, List<String>> getDocumentText(Hashtable<String,String> docString)
	{
		Hashtable<String, List<String>> docText=new Hashtable<String, List<String>>(4);
		docString.forEach((key,value) -> docText.put(key
				,new LinkedList<String>(Arrays.asList(value.split("[^a-zA-Z]+")))));	
    	return docText;
	}
	
	public static void preprocessDocumentText(Hashtable<String, List<String>> docText,boolean stemOLem)
	{
		if(stemOLem==true)
		{
			docText.forEach((key,value) -> preprocessStemWordList(value));
		}
		else
		{
			docText.forEach((key,value) -> preprocessLemWordList(value));

		}
	}

	public static Hashtable<String, List<String>> getDocumentLemmas(Hashtable<String,String> docString)
	{
		Hashtable<String, List<String>> docLemmas=new Hashtable<String, List<String>>(4);
		docString.forEach((key,value) -> docLemmas.put(key,getLemmas(value)));	
    	return docLemmas;
		
	}
	
	public static void processDocument(Document document,Hashtable<String, List<String>> hashDocArr)
	{
		ObjectId doc_id=(ObjectId) document.get("_id");
    	double bodyCount=hashDocArr.get("pageContent").size();
    	double titleCount=hashDocArr.get("pageTitle").size();
    	iterateThroughWords(hashDocArr.get("pageTitle"),doc_id,titleCount,bodyCount,1);
    	iterateThroughWords(hashDocArr.get("pageDescription"),doc_id,titleCount,bodyCount,0);
    	iterateThroughWords(hashDocArr.get("pageContent"),doc_id,titleCount,bodyCount,0);
    	//iterateThroughWords(hashDocArr.get("pageParagraphs"),doc_id,titleCount,bodyCount, 0);

	}
	public static void processsImages(Document document,Hashtable<String, List<String>> hashDocArr)
	{
		String doc_id=(String) document.get("_id");
    	double descriptionCount=hashDocArr.get("description").size();
    	iterateThroughWordsImages(hashDocArr.get("description"),doc_id,descriptionCount);	
	}
	public static Hashtable<String,String> getDocumentTextAsString(Document document,String[] fieldsName)
	{
		Hashtable<String,String> docString= new Hashtable<String,String>(fieldsName.length);

		for(int i=0;i<fieldsName.length;i++)
		{
			docString.put(fieldsName[i],document.get(fieldsName[i]).toString());
		}

		return docString;
		
	}
	
	
	public static void preprocessLemWordList(List<String> list)
	{
		ListIterator <String> i = list.listIterator();
    	while (i.hasNext()) {
    		String temp=i.next();
    		temp=temp.toLowerCase();

    		i.set(temp);
            if(temp.length()<2 || hashStopWords.get(temp)!= null)
            {
            	i.remove();
            }
         } 	
	}
	
    	public static void preprocessStemWordList(List<String> list)
    	{
    		ListIterator <String> i = list.listIterator();
        	while (i.hasNext()) {
        		String temp=i.next();
        		if(temp == null) continue;
                if(temp.length()<2 || hashStopWords.contains(temp))
                {
                	i.remove();
                }
                else
                {
            		temp=temp.toLowerCase();
            		String tstem=s.stem(temp);
            		i.set(tstem);
                }
             } 	
    	}	
	public static List<String> getLemmas(String document)
	{
		Sentence s=new Sentence(document);
		return s.lemmas();		
	}
	
	
	public static void iterateThroughWords(List<String> list,ObjectId doc_id,
				double titleCount,double bodyCount,int inTitle)
	{
		ListIterator <String> i = list.listIterator();
    	while (i.hasNext()) {
  
    		String temp=i.next();
    		DBManager.getInstance().insertInvertedWordIndex(temp, doc_id, titleCount,bodyCount, inTitle);

    	}	
    }

	
	public static void iterateThroughWordsImages(List<String> list,String doc_id,
				double descriptionCount)
	{
		ListIterator <String> i = list.listIterator();
    	while (i.hasNext()) {
    		String temp=i.next();
    		DBManager.getInstance().insertInvertedWordIndexImages(temp, doc_id, descriptionCount);
    	}	
    }
	

}
