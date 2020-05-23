package com.apt;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class QueryProcessor extends HttpServlet {

    private String respErrorWriter(String errMsg){
        return "{" +
                "\"status\": \"error\","+
                "\"msg\":\""+ errMsg + "\"" +
                "}";
    }

    public static String convertToJson(ArrayList<PageResult> results) {

        String jsonResult = "[";

        for (int i = 0; i < results.size(); i++) {
            String jsonObject = "{";

            jsonObject += "link: \"" + results.get(i).getLink() + "\",";
            jsonObject += "content: \"" + results.get(i).getPageContent() + "\",";
            jsonObject += "title: \"" + results.get(i).getPageTitle() + "\"";

            jsonObject += "}";
            if(i+1 < results.size()){
                jsonObject += ",";
            }
            jsonResult += jsonObject;
        }

        jsonResult += "]";
        return jsonResult;
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String query;
        Boolean isImgSearch;
        try {
            //Decode the query to get the actual string the user has used.
            //Since in the request is url encoded.
            query = URLDecoder.decode(req.getParameter("q"), "UTF-8");
            String imgParam = req.getParameter("img");
            isImgSearch = imgParam != null && imgParam.equals("on");
            if(query.trim().length() <= 0){
                throw new Exception();
            }
        }catch (Exception e){
            resp.getWriter().println(respErrorWriter("Couldn't parse the query"));
            return;
        }
        String result = "";
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

        List<String> wordsList =  new LinkedList<String>(Arrays.asList(query.split(" ")));;
        Indexer.preprocessStemWordList(wordsList);
        Indexer.preprocessLemWordList(wordsList);
//        //Should we process them?...
////        Indexer.preprocessStemWordList(phrased);
////        Indexer.preprocessLemWordList(phrased);
//        //Now we have our tokens...
//        //Preprocess using the indexer...
//        //Get the entries which have any token from the list...
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
        Ranker ranker = new Ranker(pageResults);
        resp.getWriter().println(convertToJson(ranker.getResultsList()));


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


}

