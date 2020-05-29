package com.apt;

import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ImageRanker {
    ConcurrentHashMap<String, ImageResult> imageResults;
    private ArrayList<ImageResult> resultsList;
    //Note: The same index that points to a value is used in the adjacency matrix...
    public ImageRanker(ConcurrentHashMap<String, ImageResult> imageResults) {
        this.imageResults = new ConcurrentHashMap<>();
        Iterator<ImageResult> list = imageResults.values().iterator();
        while(list.hasNext()){
            ImageResult entry = list.next();
            this.imageResults.put(entry.getDocumentID(), entry);
        }
        this.rank();
    }
    private void rank(){
        this.resultsList = new ArrayList<>();
        resultsList.addAll(this.imageResults.values());
        Collections.sort(resultsList, new ImageRanker.relevanceComparator());

    }


    public CopyOnWriteArrayList<ImageResult> getResultsList() {
        CopyOnWriteArrayList copyOnWriteArrayList = new CopyOnWriteArrayList<>();
        for (ImageResult result: resultsList) {
            copyOnWriteArrayList.add(result);
        }
        return copyOnWriteArrayList;
    }

    private static class relevanceComparator implements Comparator<ImageResult> {

        public int compare(ImageResult a, ImageResult b) {
            double subtraction = a.getTfIDF() - b.getTfIDF();
            if(subtraction > 0){
                return -1;
            }else if(subtraction < 0){
                return 1;
            }
            return 0;
        }

    }
}
