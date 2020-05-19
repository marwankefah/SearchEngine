package com.apt;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Page {

    // This should be changed per Machine...
    // Better take it as a user input...
    // Also, this should be in an environment file
    // Also, this is wrong!
    // Also, FML
    final static String CONNECTION_STRING = "mongodb://localhost:27017/?readPreference=primary&appname=MongoDB%20Compass%20Community&ssl=false";

    ArrayList<String> links = null;
    ArrayList<Image> images = null;
    String html;
    String origin;
    String protocolString;

    public ArrayList<Image> getImages() {
        return images;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public String getPageDescription() {
        return pageDescription;
    }

    public String getPageContent() {
        return pageContent;
    }

    String pageTitle = null;
    String pageDescription = null;
    String pageContent = null;

    //Could add keywords & author meta tags but they're not that important...

    public Page(String html, String link) {
        this.html = html;
        if(link.startsWith("https://")){
            this.protocolString = "https://";
        }else if(link.startsWith("http://")){
            this.protocolString = "http://";
        }else{
            this.html = "";
        }
        if(link.endsWith("/")){
            link = link.substring(0, link.length() - 1);
        }
        this.origin = link;
        this.setLinks();
        this.setImages();
        this.setPageTitle();
        this.setPageDescription();
        this.setPageContent();
    }

    public String getOrigin() {
        return origin;
    }

    private void setPageTitle() {
        ArrayList<String> titleTag = this.extractTags("title");
        if(titleTag.size() > 0){
            this.pageTitle = this.extractText(titleTag.get(titleTag.size() - 1));
        }else{
            this.pageTitle = "";
        }
    }


    private void setPageDescription() {
        ArrayList<String> metaTags = this.extractTagSelfClosing("meta");
        for (String metaTag: metaTags) {
            String name = this.extractAttribute(metaTag, "name");
            if(name != null && name.toLowerCase() == "description"){
                this.pageDescription = this.extractAttribute(metaTag, "content");
                return;
            }
        }
        this.pageDescription = "";
    }

    private void setPageContent() {
        ArrayList<String> bodyTags = this.extractTags("body");
        if(bodyTags.size() > 0) {
            this.pageContent = Page.extractText(bodyTags.get(bodyTags.size() - 1));
        }else{
            this.pageContent = "";
        }
    }

    public void saveToDB() {
        DBManager.getInstance().addProcessedPage(this);
    }

    private static String extractText(String html) {
        String tagRegex = "<[^>]+>";

        String htmlNoJS = html.replaceAll("<\\s*script[^>]*>.+<\\s*\\/\\s*script>", "");
        String htmlNoCSS = htmlNoJS.replaceAll("<\\s*style[^>]*>.+<\\s*\\/\\s*style>", "");
        String text = htmlNoCSS.replaceAll(tagRegex, "");

        return text.replaceAll("\n", "");
    }

    private ArrayList<String> extractTags(String tagName){
        //This regex is taken from: https://www.regexpal.com/27540
        String regex = "<\\s*"+tagName+"[^>]*>(.*?)<\\s*/\\s*"+tagName+">";
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(this.html);
        ArrayList<String> tags = new ArrayList<String>();
        while(matcher.find()) {
            tags.add(matcher.group());
        }
        return tags;
    }

    private ArrayList<String> extractTagSelfClosing(String tagName){
        //This regex is taken from: https://www.regexpal.com/27540
        String regex = "<\\s*"+tagName+"[^>]*\\/?>";
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(this.html);
        ArrayList<String> tags = new ArrayList<String>();
        while(matcher.find()) {
            tags.add(matcher.group());
        }
        return tags;
    }

    private String extractAttribute(String tag, String attributeName){
        Pattern pattern = Pattern.compile(attributeName+"=((\"[^\"]+\")|('[^']+')|[^ ]+)");
        Matcher matcher = pattern.matcher(tag);
        String attribute = null;
        //Sometimes the same attribute is written multiple times.
        //So we take the last one as the valid one.
        while(matcher.find()) {
            attribute = matcher.group();
        }
        if(attribute != null){
            attribute = attribute.replace("\"", "");
            attribute = attribute.replace("'", "");
            attribute = attribute.replace(attributeName + "=", "");
        }

        return attribute;
    }

    private boolean isHTTPLink(String link){
        if(link.contains("://")){
            return link.startsWith("http://")
                    || link.startsWith("https://");
        }else if(link.contains(":")){
            return false;
        }
        return true;
    }
    private boolean isRelativeLink(String link){
        //"//" means that the link is protocol relative

        return !link.startsWith("http://") && !link.startsWith("https://") && !link.startsWith("/");
    }

    private boolean relativeProtocolLink(String link) {
        return link.startsWith("//");
    }

    private String absoluteURL(String link) {
        String absoluteURL;
        if(link != null && this.isRelativeLink(link)){

                absoluteURL = this.origin + "/" + link;

        }else if(link != null && this.relativeProtocolLink(link)){
            absoluteURL = this.protocolString + link;
        }else if(link != null && link.startsWith("/")){
            absoluteURL = RobotsParser.getOriginURL(this.origin) + link;
        }else{
            absoluteURL = link;
        }
        return absoluteURL;
    }

    private String removeHashLocation(String link) {
        String regex = "#.*";
        return link.replaceAll(regex, "");
    }
    private void setLinks() {
        ArrayList<String> links = this.extractTags("a");
        RobotsParser parser = new RobotsParser(this.origin);
        for (int i = 0; i < links.size() ; i++) {
            String link = this.extractAttribute(links.get(i), "href");
            if(link == null || !this.isHTTPLink(link)) {
                links.set(i,null);
                continue;
            }
            link = this.absoluteURL(link);
            link = this.removeHashLocation(link);
            if(parser.canCrawlLink(link)){
                links.set(i,link);
            }else{
                System.out.println("Can't crawl: " + link);
            }
        }


        this.links = Utils.removeDuplicates(links);
        this.filterLinks();
    }

    private void setImages() {
        ArrayList<String> images = this.extractTagSelfClosing("img");
        ArrayList<Image> imageArrayList = new ArrayList<Image>();
        for (int i = 0; i < images.size() ; i++) {
            String link = this.extractAttribute(images.get(i), "src");
            String placeholder = this.extractAttribute(images.get(i), "alt");
            if(link == null || !this.isHTTPLink(link)) {
                continue;
            }
            Image img = new Image();
            img.setPlaceholder(placeholder);
            img.setSrc(this.absoluteURL(link));
            imageArrayList.add(img);
        }


        this.images = imageArrayList;
//        this.filterImages();
    }

    public ArrayList<String> getLinks() {
        if(this.links != null) return this.links;

        this.setLinks();

        return this.links;
    }

    private void filterLinks() {
        for (int i = 0; i < this.links.size() ; i++) {
            if(this.links.get(i) == null){
                this.links.remove(i);
                i--;
            }
        }
    }

    private void filterImages() {
        for (int i = 0; i < this.images.size() ; i++) {
            if(this.images.get(i) == null){
                this.images.remove(i);
                i--;
            }
        }
    }
}
