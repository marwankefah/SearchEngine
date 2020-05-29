package com.apt;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.Pair;
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

public class TrendsHandler extends HttpServlet {


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


    public static String convertToJson(List<Pair<String, Integer>> results) {

        String jsonResult = "[";

        for (int i = 0; i < results.size(); i++) {
            Pair<String, Integer> pair = results.get(i);
            String jsonObject = "{";

            jsonObject += "\"" + "value" + "\":";
            jsonObject += "\"" + escape(pair.first) + "\",";
            jsonObject += "\"" + "count" + "\":";
            jsonObject += pair.second + "}";

            jsonResult += jsonObject;
            if(i+1 < results.size()){
                jsonResult += ",";
            }
        }

        jsonResult += "]";
        return jsonResult;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String countryCode;
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "*");
        resp.setHeader("Access-Control-Allow-Headers", "*");
        try {
            //Decode the query to get the actual string the user has used.
            //Since in the request is url encoded.
            countryCode = URLDecoder.decode(req.getParameter("cc"), "UTF-8");
            if(countryCode == null || countryCode.length() == 0){
                throw new Exception();
            }
        }catch (Exception e){
            resp.getWriter().println(respErrorWriter("Couldn't parse the query"));
            return;
        }

        ArrayList<Pair<String, Integer>> Trends = DBManager.getInstance().getTrends(countryCode);
        resp.getWriter().println(convertToJson(Trends));

    }



}

