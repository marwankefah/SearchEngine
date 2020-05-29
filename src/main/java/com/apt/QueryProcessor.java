package com.apt;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import edu.stanford.nlp.simple.*;

public class QueryProcessor extends HttpServlet {

    static final int PAGINATION_SIZE = 20;

    static LinkedHashMap<String, CopyOnWriteArrayList<PageResult>> qCache = new LinkedHashMap<>();
    static LinkedHashMap<String, CopyOnWriteArrayList<PageResult>> imgCache = new LinkedHashMap<>();

    private String respErrorWriter(String errMsg){
        return "{" +
                "\"status\": \"error\","+
                "\"msg\":\""+ errMsg + "\"" +
                "}";
    }
    public static String escape(String str){
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    static CopyOnWriteArrayList<CopyOnWriteArrayList<PageResult>> queryPaginations = new CopyOnWriteArrayList<>();
    static CopyOnWriteArrayList<CopyOnWriteArrayList<ImageResult>> imagePaginations = new CopyOnWriteArrayList<>();

    public static String imageConvertToJson(List<ImageResult> results, boolean isLast, int idx){
        String jsonResult = "{\"idx\": " + idx + ", \"isLast\": " + (isLast ? "true" : "false") + ", \"data\":";

        jsonResult += "[";

        for (int i = 0; i < results.size(); i++) {
            String jsonObject = "{";

            jsonObject += "\"link\": \"" + results.get(i).getLink() + "\",";
            jsonObject += "\"description\": \"" + escape(results.get(i).getDescription()) + "\"";

            jsonObject += "}";
            if(i+1 < results.size()){
                jsonObject += ",";
            }
            jsonResult += jsonObject;
        }

        jsonResult += "]}";
        return jsonResult;
    }

    public static String convertToJson(List<PageResult> results, boolean isLast, int idx) {

        String jsonResult = "{\"idx\": " + idx + ", \"isLast\": " + (isLast ? "true" : "false") + ", \"data\":";

        jsonResult += "[";

        for (int i = 0; i < results.size(); i++) {
            String jsonObject = "{";

            jsonObject += "\"link\": \"" + results.get(i).getLink() + "\",";
            jsonObject += "\"content\": \"" + escape(results.get(i).getPageContent()) + "\",";
            jsonObject += "\"title\": \"" + escape(results.get(i).getPageTitle()) + "\"";

            jsonObject += "}";
            if(i+1 < results.size()){
                jsonObject += ",";
            }
            jsonResult += jsonObject;
        }

        jsonResult += "]}";
        return jsonResult;
    }

    public static void handleNer(String query, String countryCode){

        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        CoreDocument doc = new CoreDocument(query);
        pipeline.annotate(doc);
        ArrayList<Pair<String, String>> NERs = new ArrayList<>();
        boolean wasPrevAPerson = false;
        int i = 0;
        for (CoreLabel label: doc.tokens()) {
            if(!label.ner().equals("PERSON")) {
                wasPrevAPerson = false;
                continue;
            }
            if(wasPrevAPerson){
                NERs.get(i-1).setFirst(NERs.get(i-1).first + " " + label.word());
                wasPrevAPerson = false;
            }else{
                Pair<String, String> pair = new Pair(label.word(), countryCode);
                NERs.add(pair);
                wasPrevAPerson = true;
            }
            i++;
        }
        DBManager.getInstance().saveNERs(NERs);
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String query;
        String countryCode;
        boolean isImgSearch;
        int idx = -1;
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "*");
        resp.setHeader("Access-Control-Allow-Headers", "*");
        try {
            //Decode the query to get the actual string the user has used.
            //Since in the request is url encoded.
            query = URLDecoder.decode(req.getParameter("q"), "UTF-8");
            if(req.getParameter("idx") != null){
                idx = Integer.parseInt(req.getParameter("idx"));
            }
            countryCode = req.getParameter("cc");
            if(countryCode == null){
                countryCode = "";
            }
            String imgParam = req.getParameter("img");
            isImgSearch = imgParam != null && imgParam.equals("on");
            if(query.trim().length() <= 0){
                throw new Exception();
            }
        }catch (Exception e){
            resp.getWriter().println(respErrorWriter("Couldn't parse the query"));
            return;
        }
        if(idx > -1){
            resp.getWriter().println(getResponse(idx));
        }else if(!isImgSearch && qCache.containsKey(query+countryCode)){
            handleNer(query, countryCode);
            queryPaginations.add((CopyOnWriteArrayList<PageResult>) qCache.get(query+countryCode).clone());
            resp.getWriter().println(getResponse(queryPaginations.size() - 1));
        }else if(isImgSearch && imgCache.containsKey(query)){
            handleNer(query, countryCode);
            imagePaginations.add((CopyOnWriteArrayList<ImageResult>) imgCache.get(query).clone());
            resp.getWriter().println(getResponse(imagePaginations.size() - 1));
        }else{
            handleNer(query, countryCode);
            DBManager.getInstance().addSuggestion(query);
            List <String> phrased = new LinkedList<>();
            Pattern pattern = Pattern.compile("\"[^\"]*\"");
            Matcher matcher = pattern.matcher(query);
            //Sometimes the same attribute is written multiple times.
            //So we take the last one as the valid one.
            while(matcher.find()) {
                String matched = matcher.group();
                if(matched.length() == 0) continue;
                phrased.add(matched.replaceAll("\"", ""));
            }

            List<String> wordsList =  new LinkedList<String>(Arrays.asList(query.replace("\"", "").split(" ")));;
            Indexer.preprocessStemWordList(wordsList);
//            Indexer.preprocessLemWordList(wordsList);
//        //Should we process them?...
////        Indexer.preprocessStemWordList(phrased);
////        Indexer.preprocessLemWordList(phrased);
//        //Now we have our tokens...
//        //Preprocess using the indexer...
//        //Get the entries which have any token from the list...
            if(!isImgSearch){
                ConcurrentHashMap<ObjectId, PageResult> pageResults;
                if(phrased.size() != 0){
                    pageResults = getPageResults(phrased);
                    //Get only the documents which has all phrased values...
                    Iterator<Map.Entry<ObjectId, PageResult>> iter = pageResults.entrySet().iterator();
                    while(iter.hasNext()){
                        Map.Entry<ObjectId, PageResult> entry = iter.next();
                        ArrayList<String> tokens = entry.getValue().tokens();
                        int i = 0;
                        while (i < phrased.size() && tokens.contains(phrased.get(i))){
                            i++;
                        }
                        if(i <= phrased.size()){
                            iter.remove();
                        }
                    }

                }else{
                    pageResults = getPageResults(wordsList);
                }

                //Rank -> Convert to JSON -> Send
                Ranker ranker = new Ranker(pageResults, countryCode);
                CopyOnWriteArrayList<PageResult> list = ranker.getResultsList();
                queryPaginations.add(list);
                qCache.put(query+countryCode, (CopyOnWriteArrayList<PageResult>) list.clone());
                resp.getWriter().println(getResponse(queryPaginations.size() - 1));
            }else{
                handleNer(query, countryCode);
                ConcurrentHashMap<String, ImageResult> imageResults = getImageResults(wordsList);
                ImageRanker ranker = new ImageRanker(imageResults);
                CopyOnWriteArrayList<ImageResult> list = ranker.getResultsList();
                imagePaginations.add(list);
                imgCache.put(query, (CopyOnWriteArrayList<PageResult>) list.clone());
                resp.getWriter().println(getImageResponse(imagePaginations.size() - 1));
            }

        }


    }

    private static String getResponse(int idx) {
        int size = queryPaginations.get(idx).size();
        if(size < PAGINATION_SIZE){
            CopyOnWriteArrayList<PageResult> list = new CopyOnWriteArrayList<>();
            for (int i = 0; i < size; i++) {
                list.add(queryPaginations.get(idx).get(i));
            }
            queryPaginations.remove(idx);
            return convertToJson(list, true, idx);
        }else{
            CopyOnWriteArrayList<PageResult> list = new CopyOnWriteArrayList<>();
            for (int i = 0; i < PAGINATION_SIZE; i++) {
                list.add(queryPaginations.get(idx).get(i));
                queryPaginations.get(idx).remove(i);
            }
            return convertToJson(list, false, idx);
        }


    }
    private static String getImageResponse(int idx) {
        int size = imagePaginations.get(idx).size();
        if (size < PAGINATION_SIZE) {
            CopyOnWriteArrayList<ImageResult> list = new CopyOnWriteArrayList<>();
            for (int i = 0; i < size; i++) {
                list.add(imagePaginations.get(idx).get(i));
            }
            imagePaginations.remove(idx);
            return imageConvertToJson(list, true, idx);
        } else {
            CopyOnWriteArrayList<ImageResult> list = new CopyOnWriteArrayList<>();
            for (int i = 0; i < PAGINATION_SIZE; i++) {
                list.add(imagePaginations.get(idx).get(i));
                imagePaginations.get(idx).remove(i);
            }
            return imageConvertToJson(list, false, idx);
        }
    }

    private ConcurrentHashMap<ObjectId, PageResult> getPageResults(List<String> wordsList) {
        ConcurrentHashMap<ObjectId, PageResult> pageResults = new ConcurrentHashMap<>();
        for (String word: wordsList) {
            Iterator entries = DBManager.getInstance().getTokenEntries(word).iterator();
            while(entries.hasNext()){
                Document entry = (Document) entries.next();
                ObjectId oId = entry.getObjectId("DocumentId");
                if(pageResults.containsKey(oId)){
                    pageResults.get(oId).addToken(word);
                    pageResults.get(oId).sumIDF(entry.getDouble("tfIdf"));
                }else{
                    PageResult pageResult = new PageResult(oId, entry.getDouble("tfIdf"));
                    pageResult.setDocument(DBManager.getInstance().getProcessedPage(oId));
                    pageResult.addToken(entry.getString("token"));
                    pageResults.put(oId, pageResult);

                }
            }
        }
        return pageResults;
    }

    private ConcurrentHashMap<String, ImageResult> getImageResults(List<String> wordsList) {
        ConcurrentHashMap<String, ImageResult> imageResults = new ConcurrentHashMap<>();
        for (String word: wordsList) {
            Iterator entries = DBManager.getInstance().getImageTokenEntries(word).iterator();
            while(entries.hasNext()){
                Document entry = (Document) entries.next();
                String oId = entry.getString("imageId");
                if(imageResults.containsKey(oId)){
                    imageResults.get(oId).addToken(word);
                    imageResults.get(oId).sumIDF(entry.getDouble("tfIdf"));
                }else{
                    ImageResult imageResult = new ImageResult(oId, entry.getDouble("tfIdf"));
                    imageResult.setDocument(DBManager.getInstance().getProcessedImage(oId));
                    imageResult.addToken(entry.getString("token"));
                    imageResults.put(oId, imageResult);

                }
            }
        }
        return imageResults;
    }


}

