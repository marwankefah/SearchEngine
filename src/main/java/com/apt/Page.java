package com.apt;

import com.jaunt.Element;
import com.jaunt.Elements;
import com.jaunt.JNode;
import com.jaunt.UserAgent;
import org.apache.commons.text.StringEscapeUtils;

import java.text.SimpleDateFormat;
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

    ArrayList<String> links = new ArrayList<>();
    ArrayList<Image> images = new ArrayList<>();
    String html;
    String pageOriginalLink;
    String protocolString;
    String link;
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
    UserAgent userAgent = null;
    public Boolean getInvalid() {
        return isInvalid;
    }

    public Date getPubDate() {
        return pubDate;
    }

    Date pubDate = null;
    String countryCode = "";
//Could add keywords & author meta tags but they're not that important...

    public Page(String link, UserAgent userAgent) {
        this.link = link.substring(0, link.indexOf("?"));
        this.userAgent = userAgent;
        try{
            this.userAgent.visit(this.link);
        }catch(Exception e){
            System.out.println("Couldn't visit: " + link);
            return;
        }
        this.link = userAgent.doc.getUrl();
        if(this.link.startsWith("https://")){
            this.protocolString = "https://";
        }else if(this.link.startsWith("http://")){
            this.protocolString = "http://";
        }else{
            this.isInvalid = true;
            return;
        }
        String originLink = this.link;
        if(this.link.endsWith("/")){
            originLink = this.link.substring(0, this.link.length() - 1);
        }
        this.pageOriginalLink = originLink;

//        this.removeCSS();
        this.setPageTitle();
        this.setPageDescription();
//        ArrayList<String> bodyTags = extractTags("body");
//        if(bodyTags.size() == 0){
//            this.isInvalid = true;
//            return;
//        }
//        this.html = bodyTags.get(bodyTags.size() - 1);
//        this.pageContent = Page.extractText(this.html);
//        this.pageContent = this.pageContent.trim();
        this.setLinks();
        this.setImages();
        this.setPageContent();
        this.setPubDate();
        this.setCountryCode();
    }

    private void setCountryCode() {

        //Try the domain extension..
        String[] genericTLDList = {
                ".com",
                ".org",
                ".net",
                ".int",
                ".edu",
                ".gov",
                ".mil",
                ".tech",
                ".arpa",
                ".academy",
                ".aero",
                ".africa",
                ".agency",
                ".app",
                ".io",
                ".art",
                ".audio",
                ".auto",
                ".bar",
                ".info",
                ".biz",
                ".best",
                ".blog",
                ".bot",
                ".coffee",
                ".data",
        };
        String testingLink = RobotsParser.getOriginURL(this.pageOriginalLink);
        boolean isGeneric = false;
        for (String tld: genericTLDList) {
            if(testingLink.endsWith(tld)){
                isGeneric = true;
                break;
            }
        }
        if(!isGeneric){
            this.countryCode = testingLink.substring(testingLink.lastIndexOf(".")+1).toUpperCase();
            return;
        }

        //Try lang attribute..
        Iterator<Element> els = this.userAgent.doc.findEvery("<html>").iterator();
        while(els.hasNext()){
            Element el = els.next();
            String langAttr = el.getAtString("lang");
            if(langAttr.length() > 0){
                String[] splitted = langAttr.split("-");
                if(splitted.length > 1){
                    this.countryCode = splitted[1].toUpperCase();
                    return;
                }
            }
        }

        //Fallback to Amazon Alexa's data...
        try{
            this.userAgent.settings.genericXMLMode = true;
            userAgent.visit("http://data.alexa.com/data?cli=10&dat=snbamz&url="+this.link);
            Element el = userAgent.doc.findFirst("<COUNTRY>");
            this.countryCode = el.getAtString("CODE");
        }catch(Exception e){
            this.userAgent.settings.genericXMLMode = false;
            try{
                this.userAgent.visit(this.link);
            }catch (Exception _e){

            }
        }

    }

    public String getCountryCode() {
        return countryCode;
    }

    private void setPubDate() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYY-MM-DD");
        //Try Meta tags...
        try {
            Elements metaTags = this.userAgent.doc.getEach("<meta>");
            for (Element tag: metaTags) {
                if(tag.getAtString("article.published").length() > 0
                    || tag.getAtString("bt:pubDate").length() > 0
                    || tag.getAtString("DC.date.issued").length() > 0
                    || tag.getAtString("pubdate").length() > 0
                    || tag.getAtString("itemprop").equals("datePublished")
                ){
                    this.pubDate = simpleDateFormat.parse(tag.getAtString("content"));
                    return;
                }
            }
        }catch (Exception e){

        }
        //Try html tags...
        try {
            Elements pubTags = this.userAgent.doc.getEach("<  class=(pubdate|timestamp)>");
            for (Element tag: pubTags) {
                this.pubDate = simpleDateFormat.parse(tag.getTextContent());
                return;
            }
        }catch (Exception e){

        }

        //Try the url...
        //This regex is taken from: https://webhose.io/blog/api/articles-publication-date-extractor-an-overview/
        String regex = "([\\./\\-_]{0,1}(19|20)\\d{2})[\\./\\-_]{0,1}(([0-3]{0,1}[0-9][\\./\\-_])|(\\w{3,5}[\\./\\-_]))([0-3]{0,1}[0-9][\\./\\-]{0,1})?";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(this.link);
        //From what I've seen it's common to use the following format: YYYY -> MM -> DD
        if(matcher.find()){
            String date = matcher.group();
            String[] data = new String[0];
            if(date.contains("/")){
                data = date.split("/");
            }else if(date.contains("-")){
                data = date.split("/");
            }else if(date.contains(".")){
                data = date.split("/");
            }else if(date.contains("_")){
                data = date.split("/");
            }
            if(data.length == 3){
                try{
                    //Year is first
                    if(data[0].length() > 2){//YMD -> ISO
                        simpleDateFormat = new SimpleDateFormat("YYYY-MM-DD");
                        this.pubDate = simpleDateFormat.parse(
                                data[0] + "-" +
                                        (data[1].length() > 1 ? data[1] : "0" + data[1]) + "-" +
                                        (data[2].length() > 1 ? data[2] : "0" + data[2])
                        );
                        return;
                    }else{//MDY
                        simpleDateFormat = new SimpleDateFormat("YYYY-MM-DD");
                        this.pubDate = simpleDateFormat.parse(
                                        (data[0].length() > 1 ? data[0] : "0" + data[0]) + "-" +
                                        (data[1].length() > 1 ? data[1] : "0" + data[1]) + "-" +
                                data[2]
                        );
                        return;
                    }
                }catch (Exception e){

                }
            }
        }

        //As a last resort, check WebArchive...
        try{
            userAgent.sendGET("https://archive.org/wayback/available?url="+this.pageOriginalLink);
            JNode snapshots = userAgent.json.findFirst("archived_snapshots");
            JNode closest = snapshots.findFirst("closest");
            String timestamp = closest.findFirst("timestamp").toString();
            //Format is: yyyymmdd
            String year = timestamp.substring(0, 4);
            String month = timestamp.substring(4, 6);
            String day = timestamp.substring(6, 8);
            this.pubDate = simpleDateFormat.parse(year + "-" + month + "-" + day);
        }catch (Exception e){

        }

    }

    private void removeCSS() {
        try{
        }catch(Exception e){
            System.out.println("Couldn't get paragraphs of: " + this.link);
        }
    }

    public String getPageOriginalLink() {
        return pageOriginalLink;
    }

    private void setPageTitle() {
        try{
            this.pageTitle = this.userAgent.doc.findFirst("<title>")
                    .getTextContent(null, null, null, true, false, true, true).trim();
        }catch(Exception e){
            System.out.println("Couldn't get title of: " + this.link);
        }
//        ArrayList<String> titleTag = this.extractTags("title");
//        if(titleTag.size() > 0){
//            this.pageTitle = this.extractText(titleTag.get(titleTag.size() - 1));
//        }else{
//            this.pageTitle = "";
//        }
    }

    public String getPageContent() {
        return this.pageContent;
    }

    private void setPageDescription() {
        try{
            Elements metaElements = this.userAgent.doc.findEvery("<meta>");
            for (Element el: metaElements) {
                if(el.getAtString("name").toLowerCase()
                 == "description"){
                    this.pageDescription = el.getAtString("content").trim();
                }
            }
        }catch(Exception e){
            System.out.println("Couldn't get pageDescription of: " + this.link);
        }
//        ArrayList<String> metaTags = this.extractTagSelfClosing("meta");
//        for (String metaTag: metaTags) {
//            String name = this.extractAttribute(metaTag, "name");
//            if(name != null && name.toLowerCase() == "description"){
//                this.pageDescription = this.extractAttribute(metaTag, "content");
//                return;
//            }
//        }
    }

    private void setPageContent() {
        try{
            Elements pElements = this.userAgent.doc.findEvery("<p>");
            for (Element el: pElements) {
                this.paragraphs.add(el.getTextContent().trim());
            }
        }catch(Exception e){
            System.out.println("Couldn't get paragraphs of: " + this.link);
        }

        try{
            HashMap<String, String> headersHashMap = new HashMap<>();
            for (int i = 1; i <= 6; i++) {
                Elements hElements = this.userAgent.doc.findEvery("<h" + i + ">");
                for (Element el: hElements) {
                    String text = el.getTextContent().trim();
                    if(text.length() == 0) continue;
                    headersHashMap.put("h"+i+text, text);
                }
            }
        }catch(Exception e){
            System.out.println("Couldn't get headers of: " + this.link);
        }
//        ArrayList<String> pTags = this.extractTags("p");
//        for (String pTag: pTags) {
//            this.paragraphs.add(Page.extractText(pTag));
//        }
//        //Should filter the content....
//        HashMap<String, String> headersHashMap = new HashMap<>();
//        for (int i = 1; i <= 6; i++) {
//            ArrayList<String> hTags = this.extractTags("h" + i);
//            for (String hTag: hTags) {
//                String text = Page.extractText(hTag);
//                if(text.length() == 0) continue;
//                headersHashMap.put("h"+i+text, text);
//            }
//        }
//        Set<String> set = headersHashMap.keySet();
//        Iterator iterator = set.iterator();
//
//        while(iterator.hasNext()){
//            String key = (String) iterator.next();
//            String value = headersHashMap.get(key);
//            char type = key.charAt(1);
//            Header header = new Header(type - '0', value);
//            this.headers.add(header);
//        }
        try{

            Elements styleElements = this.userAgent.doc.findEvery("<style>");
            for (Element el: styleElements) {
                el.delete();
            }
        }catch(Exception e){
            System.out.println("Couldn't get pageContents of: " + this.link);
        }
        try{
            this.pageContent = this.userAgent.doc.getTextContent(null, null, null, true, false, true, true)
                .replaceAll("\n", " ")
                .replaceAll("\t", " ")
                .replaceAll("\r", " ")
                .replaceAll(" +", " ").trim();
        }catch(Exception e){
            System.out.println("Couldn't get pageContents of: " + this.link);
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
        text = text.replace("\n", " ").trim();
        text = text.replace("&amp;", "&");
        text = text.replace("&nbsp;", " ");
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

                absoluteURL = this.pageOriginalLink + "/" + link;

        }else if(link != null && this.relativeProtocolLink(link)){
            absoluteURL = this.protocolString + link.substring(2);
        }else if(link != null && link.startsWith("/")){
            absoluteURL = RobotsParser.getOriginURL(this.pageOriginalLink) + link;
        }else{
            absoluteURL = link.substring(0, link.indexOf("?"));
        }
        return absoluteURL;
    }

    private String removeHashLocation(String link) {
        String regex = "#.*";
        return link.replaceAll(regex, "");
    }
    private void setLinks() {
        RobotsParser parser = RobotsParser.getParser(this.pageOriginalLink);
        try{
            Elements aElements = this.userAgent.doc.findEvery("<a>");
            for (Element el: aElements) {
                String link = el.getAtString("href");
                if(link == null || !this.isHTTPLink(link)) {
                    continue;
                }
                link = this.absoluteURL(link);
                link = this.removeHashLocation(link);
                if(parser.canCrawlLink(link)){
                    links.add(link);
                }else{
                    System.out.println("Can't crawl: " + link);
                }

            }
        }catch(Exception e){
            System.out.println("Couldn't get headers of: " + this.link);
        }
    }

    private void setImages() {
        try{
            Elements imgElements = this.userAgent.doc.findEvery("<img>");
            for (Element el: imgElements) {
                String link = el.getAtString("src");
                //Some sites uses html entities... don't know why but they do.
                String alt = StringEscapeUtils.unescapeHtml4(el.getAtString("alt"));
                if(alt == null || alt.length() == 0) continue;
                if(link == null || !this.isHTTPLink(link)) {
                    continue;
                }
                link = this.absoluteURL(link);
                link = this.removeHashLocation(link);
                Image img = new Image();
                img.setAlt(alt);
                img.setSrc(link);
                this.images.add(img);
            }
        }catch(Exception e){
            System.out.println("Couldn't get images from: " + this.link);
        }
//        ArrayList<String> images = this.extractTagSelfClosing("img");
//        ArrayList<Image> imageArrayList = new ArrayList<Image>();
//        for (int i = 0; i < images.size() ; i++) {
//            String link = this.extractAttribute(images.get(i), "src");
//            String placeholder = this.extractAttribute(images.get(i), "alt");
//            if(placeholder == null) {
//                placeholder = this.extractAttribute(images.get(i), "title");
//            }
//            if(link == null || !this.isHTTPLink(link)) {
//                continue;
//            }
//            Image img = new Image();
//            img.setAlt(placeholder);
//            img.setSrc(this.absoluteURL(link));
//            imageArrayList.add(img);
//        }


//        this.images = imageArrayList;
    }

    public ArrayList<String> getLinks() {
        if(this.links != null) return this.links;

        this.setLinks();

        return this.links;
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
