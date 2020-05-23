package com.apt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RobotsParser {


    private ArrayList<String> disallows = new ArrayList<>();
    private ArrayList<String> allows = new ArrayList<>();
    String origin;

    static HashMap<String, RobotsParser> parsers = new HashMap<>();

    private RobotsParser(String link) {
        this.origin = RobotsParser.getOriginURL(link);
        this.initialize(this.getTXTFile(origin));
    }

    public static RobotsParser getParser(String link){
        String origin = RobotsParser.getOriginURL(link);
        RobotsParser parser = parsers.get(origin);
        if(parser != null) return parser;
        parser = new RobotsParser(link);
        parsers.put(origin, parser);
        return parser;
    }

    //Since web masters don't know our user-agent, we'll follow the rules set for all crawlers.
    //i.e. User-Agent: *
    private void initialize(String data) {
        String[] linesArr = data.split("\n");
        List<String> lines = Arrays.asList(linesArr);
        //It may seem as O(n^2) but it's O(n)...
        for (int i = 0; i < lines.size(); i++) {
            String UALine = lines.get(i).toLowerCase();
            if(UALine.startsWith("user-agent") && UALine.contains("*")){
                //Get the allow or disallow...
                boolean foundARule = false;
                for (int j = i+1; j < lines.size(); j++) {
                    String line = lines.get(j).toLowerCase();
                    //Checks if we've already found a rule to comply user-agent grouping...
                    if(line.startsWith("user-agent") && foundARule) break;
                    if(line.startsWith("allow")){
                        String pattern = line.substring(line.indexOf(":")+1).trim();
                        if(pattern.length() == 0) continue;
                        allows.add(pattern);
                        foundARule = true;
                    }else if(line.startsWith("disallow")){
                        String pattern = line.substring(line.indexOf(":")+1).trim();
                        if(pattern.length() == 0) continue;
                        disallows.add(pattern);
                        foundARule = true;
                    }
                }
                break;
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
        //if it passes any allow rule return true
        //otherwise, check if passes any disallow rule. if so, return false.
        //otherwise return true;
        for (String allowRule: allows) {
            if(RobotsParser.doesPatternMatches(filtered, allowRule)){
                return true;
            }
        }
        for (String disallowRule: disallows) {
            if(RobotsParser.doesPatternMatches(filtered, disallowRule)){
                return false;
            }
        }

        return true;
    }

    static String getOriginURL(String link) {
        String[] parts = link.split("/");
        parts = Arrays.copyOfRange(parts,0, 3);
        return String.join("/", parts);
    }
}
