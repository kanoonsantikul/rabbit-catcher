package com.kanoonsantikul.catcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.HttpURLConnection;
import java.net.URL;

public class HtmlDigger{
    public static final int ONE_TAG = 1;
    public static final int TWO_TAG = 2;

    private HttpURLConnection connection = null;
    private StringBuffer response = null;

    public HtmlDigger(HttpURLConnection connection) throws IOException{
        this.connection = connection;
        getHtmlStringBuffer();
    }

    public HtmlDigger(String urlString) throws IOException{
        connection = createConnection(new URL(urlString));
        getHtmlStringBuffer();
    }

    public HttpURLConnection createConnection(URL url)
            throws IOException{
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        //create connection
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        connection.setFollowRedirects(true);
        connection.setDoInput(true);
        connection.setDoOutput(true);

        return connection;
    }

    public void disconnect(){
        if(connection != null){
            connection.disconnect();
        }
    }

    public ArrayList<String> findInHtml(Pattern pattern,int group){
        ArrayList<String> texts = new ArrayList<String>();
        Matcher matcher = pattern.matcher(getHtmlString());

        int i = 0;
        while(matcher.find()){
            String text = matcher.group(group);
            texts.add(text);
        }

        if(texts.size() > 0) return texts;
        else return null;
    }

    public ArrayList<String> findTag(String tagName, int tagType){
        ArrayList<String> tags = new ArrayList<String>();
        String regex = "";
        if(tagType == TWO_TAG){
            regex = "(< *?" + tagName + "\\b[^>]*>(.*?)< *?/" + tagName + " *?>)";
        } else if(tagType == ONE_TAG){
            regex = "(< *?" + tagName + "[^>]*?>)";
        }
        Pattern pattern = Pattern.compile(regex,
                Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

        tags.addAll(findInHtml(pattern, 1));
        if(tags.size() > 0) return tags;
        else return null;
    }

    public HttpURLConnection getHttpConnection(){
        return this.connection;
    }

    public String getHtmlString(){
        return response.toString();
    }

    public StringBuffer getHtmlStringBuffer() throws IOException{
        if(response == null){
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(this.connection.getInputStream()));
            String inputLine;
            response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        }
        return response;
    }

    public String getTagAttribute(String tag, String attribute){
        String tagAttribute = null;
        String regex = "<.*? ?" + attribute + "=[\'\"](.*?)[\'\"] *?[^>]*?>";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(tag);

        if(matcher.find()){
            tagAttribute =  matcher.group(1);
        }

        return tagAttribute;
    }

    public String getTagContent(String tag){
        String tagContent = null;
        String regex = "<[^>]+>(.+)<[^<]+>";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(tag);

        if(matcher.find()){
            tagContent =  matcher.group(1);
        }

        return tagContent;
    }

    public void refresh() throws IOException{
        response = null;
        response = getHtmlStringBuffer();
    }
}
