package com.apt;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.bson.Document;
import org.bson.types.ObjectId;

import edu.stanford.nlp.process.Stemmer;
import edu.stanford.nlp.simple.Sentence;

public class IndexThread implements Runnable {

	static Hashtable<String,Boolean> hashStopWords=Utils.getStopWords("stopwords_en.txt");;
	static Stemmer s=new Stemmer();
	
	Hashtable<String, List<String>> hashDocArr = new Hashtable<String, List<String>>(4);
	Hashtable<String, List<String>> hashLemmArr = new Hashtable<String, List<String>>(4);
	Hashtable<String,String> docString= new Hashtable<String,String>(4);
	Hashtable<String,String> docString1= new Hashtable<String,String>(4);

	Document document;
	int bodyCount=-1;
	int titleCount=-1;
	public IndexThread(Document doc)
	{
		document=new Document(doc);
		this.getDocumentTextAsString();
		this.getDocumentText();
    	bodyCount=hashDocArr.get("pageParagraphs").size()
    			+hashDocArr.get("pageContent").size();
    	titleCount=hashDocArr.get("pageTitle").size();
		return;
	}
	
	public void getDocumentTextAsString()
	{
		docString.put("pageTitle",document.get("pageTitle").toString());
		docString.put("pageDescription",document.get("pageDescription").toString());
		docString.put("pageContent",document.get("pageContent").toString());
		docString.put("pageParagraphs",document.get("pageParagraphs").toString());
		return;
		
	}
	public void	getDocumentText()
	{
		docString.forEach((key,value) -> hashDocArr.put(key
				,new LinkedList<String>(Arrays.asList(value.split("[^a-zA-Z]+")))));	
    	return;
	}
	
	public static void preprocessWordList(List<String> list,boolean stemOrLem)
	{
		ListIterator <String> i = list.listIterator();
    	while (i.hasNext()) {
    		String temp=i.next();
    		temp=temp.toLowerCase();
    		if(stemOrLem)
    		{
    		String tstem=s.stem(temp);
    		i.set(tstem);
    		}
    		else
    		{i.set(temp);}
            if(temp.length()<2  || hashStopWords.get(temp)!= null)
            {
            	i.remove();
            }
         } 	
	}
	public void preprocessDocumentText(boolean stemOrLem)
	{
		if(stemOrLem)
		{
		this.getDocumentText();
		hashDocArr.forEach((key,value) -> preprocessWordList(value,true));
		}
		else
		{
		this.getDocumentLemmas();
		hashLemmArr.forEach((key,value) -> preprocessWordList(value,false));
		}

	}
	public  void getDocumentLemmas()
	{
		docString.forEach((key,value) -> hashLemmArr.put(key,Indexer.getLemmas(value)));	
		
	}
	

	public void processDocument(boolean stemOrLem)
	{
		ObjectId doc_id=(ObjectId) document.get("_id");
		if(stemOrLem)
		{
		Indexer.iterateThroughWords(hashDocArr.get("pageTitle"),doc_id,titleCount,bodyCount,1);
    	Indexer.iterateThroughWords(hashDocArr.get("pageDescription"),doc_id,titleCount,bodyCount,0);
    	Indexer.iterateThroughWords(hashDocArr.get("pageContent"),doc_id,titleCount,bodyCount,0);
    	Indexer.iterateThroughWords(hashDocArr.get("pageParagraphs"),doc_id,titleCount,bodyCount,0);
		}
		else
		{
			Indexer.iterateThroughWords(hashLemmArr.get("pageTitle"),doc_id,titleCount,bodyCount,1);
	    	Indexer.iterateThroughWords(hashLemmArr.get("pageDescription"),doc_id,titleCount,bodyCount,0);
	    	Indexer.iterateThroughWords(hashLemmArr.get("pageContent"),doc_id,titleCount,bodyCount,0);
	    	Indexer.iterateThroughWords(hashLemmArr.get("pageParagraphs"),doc_id,titleCount,bodyCount,0);	
		}
	}

	
	public void indexStemsOrLemmas(boolean stemsOrLemmas)
	{
		
    	this.preprocessDocumentText(stemsOrLemmas);
    	this.processDocument(stemsOrLemmas);	
	
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		if(Integer.parseInt(Thread.currentThread().getName())==1)
		{
			this.indexStemsOrLemmas(true);
		}
		else if(Integer.parseInt(Thread.currentThread().getName())==2)
		{
			this.indexStemsOrLemmas(false);
			
		}
		else
		{
			System.out.print("Error in thread Name");
			return;
			
		}
	}
	
	
	

}
