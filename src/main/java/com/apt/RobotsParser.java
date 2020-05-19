package com.apt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RobotsParser {


    private ArrayList<String> disallows = new ArrayList<>();
    private ArrayList<String> allows = new ArrayList<>();
    String origin;

    public RobotsParser(String link) {
        this.origin = RobotsParser.getOriginURL(link);
        this.initialize(this.getTXTFile(origin));
    }

    //Since we don't have a specific "knows" crawler we will follow all (dis)allow rules.
    private void initialize(String data) {
        String[] linesArr = data.split("\n");
        List<String> lines = Arrays.asList(linesArr);
        for (String line: lines) {
            line = line.toLowerCase();
            if(line.startsWith("disallow")){
                String[] disallowKV = line.split(":");
                if(disallowKV.length != 2) continue;
                disallows.add(disallowKV[1]);
            }else if(line.startsWith("allow")){
                String[] allowKV = line.split(":");
                if(allowKV.length != 2) continue;
                allows.add(allowKV[1]);
            }
        }
    }

    private String getTXTFile(String origin) {
        return Utils.downloadURLData(origin + "/robots.txt");
    }

    static String getPatternAsRegex(String pattern) {
        //Remove query params from the pattern
        pattern = pattern.substring(0, pattern.indexOf("?"));
        if(!pattern.endsWith("$") && !pattern.endsWith("*")){
            pattern = pattern + "*";
        }
        String regex = pattern.replaceAll("\\*", ".*");

        return regex;
    }

    static boolean doesPatternMatches(String link, String pattern){
        return link.matches(RobotsParser.getPatternAsRegex(pattern));
    }

    public boolean canCrawlLink(String link){
        String filtered = link.replaceFirst(this.origin, "");
        for (String disallowRule: disallows) {
            if(RobotsParser.doesPatternMatches(link, disallowRule)){
                return false;
            }
        }
        //Ask engineer Hussein what to do with the allow directives...

        return true;
    }

    static String getOriginURL(String link) {
        String[] parts = link.split("/");
        parts = Arrays.copyOfRange(parts,0, 3);
        return String.join("/", parts);
    }
}
