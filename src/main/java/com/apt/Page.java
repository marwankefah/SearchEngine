package com.apt;


import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Page {

    // This should be changed per Machine...
    // Better take it as a user input...
    // Also, this should be in an environment file
    // Also, this is wrong!
    // Also, FML
    // And I didn't need it after all....
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

    String pageTitle = null;
    String pageContent = "";
    String pageDescription = "";
    ArrayList<String> paragraphs = new ArrayList<>();
    ArrayList<Header> headers = new ArrayList<>();
    Boolean isInvalid = false;

    public Boolean getInvalid() {
        return isInvalid;
    }
//Could add keywords & author meta tags but they're not that important...

    public Page(String html, String link) {
        String CSSRegex = "<\\s*style[^>]*>(.*?)<\\s*/\\s*style>";
        String JSRegex = "<\\s*script[^>]*>(.*?)<\\s*/\\s*script>";

        this.html = html.replaceAll(CSSRegex, "");
        this.html = this.html.replaceAll(JSRegex, "");

        if(link.startsWith("https://")){
            this.protocolString = "https://";
        }else if(link.startsWith("http://")){
            this.protocolString = "http://";
        }else{
            this.isInvalid = true;
//            this.html = "";
            return;
        }
        if(link.endsWith("/")){
            link = link.substring(0, link.length() - 1);
        }
        this.origin = link;
        this.setPageTitle();
        this.setPageDescription();
        ArrayList<String> bodyTags = extractTags("body");
        if(bodyTags.size() == 0){
            this.isInvalid = true;
            return;
        }
        this.html = bodyTags.get(bodyTags.size() - 1);
        this.pageContent = Page.extractText(this.html);
        this.pageContent = this.pageContent.trim();
        this.setLinks();
        this.setImages();
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

    public String getPageContent() {
        return this.pageContent;
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
    }

    private void setPageContent() {
        ArrayList<String> pTags = this.extractTags("p");
        for (String pTag: pTags) {
            this.paragraphs.add(Page.extractText(pTag));
        }
        //Should filter the content....
        HashMap<String, String> headersHashMap = new HashMap<>();
        for (int i = 1; i <= 6; i++) {
            ArrayList<String> hTags = this.extractTags("h" + i);
            for (String hTag: hTags) {
                String text = Page.extractText(hTag);
                if(text.length() == 0) continue;
                headersHashMap.put("h"+i+text, text);
            }
        }
        Set<String> set = headersHashMap.keySet();
        Iterator iterator = set.iterator();

        while(iterator.hasNext()){
            String key = (String) iterator.next();
            String value = headersHashMap.get(key);
            char type = key.charAt(1);
            Header header = new Header(type - '0', value);
            this.headers.add(header);
        }


    }

    public void saveToDB() {
        DBManager.getInstance().addProcessedPage(this);
    }

    public ArrayList<String> getParagraphs() {
        return paragraphs;
    }

    public ArrayList<Header> getHeaders() {
        return headers;
    }

    private static String extractText(String html) {
        String tagRegex = "<[^>]+>";
        String text = html.replaceAll(tagRegex, " ");
        text = text.replaceAll("\n", " ").trim();
        text = text.replaceAll("&amp;", "&");
        text = text.replaceAll("&nbsp;", " ");
        return text.trim();
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
        //This regex is a modified version of: https://www.regexpal.com/27540
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
            absoluteURL = this.protocolString + link.substring(2);
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
        RobotsParser parser = RobotsParser.getParser(this.origin);
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
//        this.filterLinks();
    }

    private void setImages() {
        ArrayList<String> images = this.extractTagSelfClosing("img");
        ArrayList<Image> imageArrayList = new ArrayList<Image>();
        for (int i = 0; i < images.size() ; i++) {
            String link = this.extractAttribute(images.get(i), "src");
            String placeholder = this.extractAttribute(images.get(i), "alt");
            if(placeholder == null) {
                placeholder = this.extractAttribute(images.get(i), "title");
            }
            if(link == null || !this.isHTTPLink(link)) {
                continue;
            }
            Image img = new Image();
            img.setAlt(placeholder);
            img.setSrc(this.absoluteURL(link));
            imageArrayList.add(img);
        }


        this.images = imageArrayList;
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


    public static final class Header{
        int type;
        String content;
        public Header(int type, String content) {
            this.type = type;
            this.content = content;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
