package com.apt;


import java.util.ArrayList;

//Takes an entry link for a web page, retrieves the page and parses it.
//Export all links found on this page to be the seed list file.
//The seed list file is in JSON format.
public class SeedListGenerator {

    final static String entryPoint = "https://www.nytimes.com/";

    public static ArrayList<String> getSeedLinks() {
        //Take Wikipedia's content page as the main entry point

//        Page entryPage = Utils.getPage(entryPoint);
//
//        if(entryPage == null) {
//            System.exit(1);
//        }
//
//        ArrayList<String> links = entryPage.getLinks();
//
//
//        return links;

        ArrayList<String> links = new ArrayList<>();
        links.add("https://www.b2byellowpages.com/directory/");
        links.add("https://www.brownbook.net/");
        links.add("https://www.blogarama.com/");
        links.add("https://www.spoke.com/");
        links.add("https://aboutus.com/");
        links.add("https://botw.org/");
        return links;

    }

}
