package com.apt;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Scanner;

public class Utils {

    public static String getUserInput(String msg) {
        Scanner scanner = new Scanner(System.in);
        System.out.print(msg);
        String input = scanner.nextLine();
        scanner.close();
        return input;
    }
    public static String getUserInput() {
        return getUserInput("");
    }

    public static ArrayList<String> removeDuplicates(ArrayList<String> list) {
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        set.addAll(list);
        list.clear();
        list.addAll(set);
        return list;
    }

    public static Page getPage(String link) {
        try{
            //Accept redirects
            HttpURLConnection.setFollowRedirects(true);

            URL url = new URL(link);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            //Read the response...
            InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            String stringPage = "";
            while((line = bufferedReader.readLine()) != null){
                stringPage = stringPage + line;
            }
            return new Page(stringPage, link);
        } catch (Exception e){
//            e.printStackTrace();
            return getPage(Crawler.getAvailableLink());
        }
    }
    
    
    public static Hashtable<String, Boolean> getStopWords(String fileName) throws FileNotFoundException
    {
    	   File stopWordsFile = new File(fileName);
    	   Hashtable<String, Boolean> hashMap = new Hashtable<String, Boolean>(); 
    	   Scanner scan = new Scanner(stopWordsFile); 
		    while (scan.hasNextLine()) { 
		      hashMap.put(scan.nextLine(),true);
		  } 
		    return hashMap;
    }
    

}
