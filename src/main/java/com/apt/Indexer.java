package com.apt;

import java.io.FileNotFoundException;
import java.util.ArrayList;
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
import edu.stanford.nlp.simple.Sentence;


public class Indexer {
	static Hashtable<String,Boolean> hashStopWords;
	static Stemmer s=new Stemmer();
	public Indexer()
	{
		// getting hash map for the stopWprds
		hashStopWords= new Hashtable<String, Boolean>();
		
	}
	public static void main(String[] args) throws FileNotFoundException, InterruptedException {
		// TODO Auto-generated method stub

		Indexer indexer=new Indexer();
		hashStopWords=Utils.getStopWords("stopwords_en.txt");
		

		// get documents to index
		MongoCursor<org.bson.Document> documentCursor =DBManager.getInstance().getCrawledDocuments();
		
		// loop to index
		
		if(documentCursor!=null)
		{
			long documentCount=DBManager.getInstance().getDocumentsCount();
				System.out.println(documentCount);
				try {
				    while (documentCursor.hasNext()) {
				    	
				    	Document document=documentCursor.next();
				    	//TODO CHECK if you want to add weight for description
				    	Hashtable<String, List<String>> hashDocArr = new Hashtable<String, List<String>>(4);
				    	Hashtable<String, List<String>> hashLemmArr = new Hashtable<String, List<String>>(4);
				    	hashDocArr=getDocumentText(getDocumentTextAsString(document));
				    	preprocessDocumentText(hashDocArr,true);
				    	processDocument(document,hashDocArr);
				
				    	hashLemmArr=getDocumentText(getDocumentTextAsString(document));
				    	preprocessDocumentText(hashLemmArr,false);
				    	processDocument(document,hashLemmArr);
				    	DBManager.getInstance().setIndexed((ObjectId)document.get("_id"),true);
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
    	int bodyCount=hashDocArr.get("pageParagraphs").size()
    			+hashDocArr.get("pageContent").size();
    	int titleCount=hashDocArr.get("pageTitle").size();
    	iterateThroughWords(hashDocArr.get("pageTitle"),doc_id,titleCount,bodyCount,1);
    	iterateThroughWords(hashDocArr.get("pageDescription"),doc_id,titleCount,bodyCount,0);
    	iterateThroughWords(hashDocArr.get("pageContent"),doc_id,titleCount,bodyCount,0);
    	iterateThroughWords(hashDocArr.get("pageParagraphs"),doc_id,titleCount,bodyCount, 0);	
	
	}
	public static Hashtable<String,String> getDocumentTextAsString(Document document)
	{
		Hashtable<String,String> docString= new Hashtable<String,String>(4);
		docString.put("pageTitle",document.get("pageTitle").toString());
		docString.put("pageDescription",document.get("pageDescription").toString());
		docString.put("pageContent",document.get("pageContent").toString());
		docString.put("pageParagraphs",document.get("pageParagraphs").toString());
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
                if(temp.length()<2 || hashStopWords.get(temp)!= null)
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
				int titleCount,int bodyCount,int inTitle)
	{
		ListIterator <String> i = list.listIterator();
    	while (i.hasNext()) {
  
    		String temp=i.next();
    		DBManager.getInstance().insertInvertedWordIndex(temp, doc_id, titleCount,bodyCount, inTitle);

    	}	
    }
		
	

}
