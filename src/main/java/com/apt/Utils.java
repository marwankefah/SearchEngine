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

    public static String downloadURLData(String link){
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
            String data = "";
            while((line = bufferedReader.readLine()) != null){
                data = data + line;
            }
            return data;
        } catch (Exception e){
            if(link == null) return "";
            if(link.endsWith("robots.txt")){
                System.out.println("Couldn't find: " + link);
                return "";
            }
            return null;
        }
    }


    public static Page getPage(String link) {
        String pageData = Utils.downloadURLData(link);
        if(pageData == null || pageData == ""){
            return getPage(Crawler.getAvailableLink());
        }else{
            return new Page(pageData, link);
        }
    }
    
    
    public static Hashtable<String, Boolean> getStopWords(String fileName)
    {
    	   File stopWordsFile = new File(fileName);
    	   Hashtable<String, Boolean> hashMap = new Hashtable<String, Boolean>(); 
    	   Scanner scan;
		try {
			scan = new Scanner(stopWordsFile);
			   while (scan.hasNextLine()) { 
				      hashMap.put(scan.nextLine(),true);
				  } 
				    return hashMap;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
            return new Hashtable<String, Boolean>();
//			e.printStackTrace();
		} 
		 
    }
    

}
