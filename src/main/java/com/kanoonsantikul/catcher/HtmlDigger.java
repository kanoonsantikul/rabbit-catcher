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

    //get html from custom connection
    public HtmlDigger(HttpURLConnection connection) throws IOException{
        this.connection = connection;
        response = getHtmlStringBuffer();
    }

    //get html from source url
    public HtmlDigger(String urlString) throws IOException{
        createConnection(new URL(urlString));
        response = getHtmlStringBuffer();
    }

    //this connect to http server
    public void disconnect(){
        if(connection != null){
            connection.disconnect();
        }
    }

    //find something in html use given regex
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

    //get the connection to http server
    public HttpURLConnection getHttpConnection(){
        return this.connection;
    }

    //get response html as string
    public String getHtmlString(){
        return response.toString();
    }

    //get response html as string buffer
    public StringBuffer getHtmlStringBuffer() throws IOException{
        StringBuffer response;

        BufferedReader in = new BufferedReader(
                new InputStreamReader(this.connection.getInputStream()));
        String inputLine;
        response = new StringBuffer();
        while((inputLine = in.readLine()) != null){
            response.append(inputLine);
        }
        in.close();

        return response;
    }

    public ArrayList<String> getElementById(String id, int tagType){
        ArrayList<String> tags = new ArrayList<String>();
        String regex = "";
        if(tagType == TWO_TAG){
            regex = "(<.* id *= *[\'\"]" + id + "[\'\"] *[^>]*>(.*?)< */" + id + " *>)";
        } else if(tagType == ONE_TAG){
            regex = "(<.* id *= *[\'\"]" + id + "[\'\"] *[^>]*>";
        }
        Pattern pattern = Pattern.compile(
                regex,
                Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

        tags.addAll(findInHtml(pattern, 1));
        if(tags.size() > 0) return tags;
        else return null;
    }

    public ArrayList<String> getElementByTag(String tagName, int tagType){
        ArrayList<String> tags = new ArrayList<String>();
        String regex = "";
        if(tagType == TWO_TAG){
            regex = "(< *" + tagName + " *[^>]*>(.*?)< */" + tagName + " *>)";
        } else if(tagType == ONE_TAG){
            regex = "(< *" + tagName + " *[^>]*>)";
        }
        Pattern pattern = Pattern.compile(
                regex,
                Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

        tags.addAll(findInHtml(pattern, 1));
        if(tags.size() > 0) return tags;
        else return null;
    }

    //get value in tag's attribute
    public String getTagAttributeValue(String tag, String attribute){
        String tagAttribute = null;
        String regex = "<.* " + attribute + " *= *[\'\"](.*)[\'\"] *[^>]*>";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(tag);

        if(matcher.find()){
            tagAttribute =  matcher.group(1);
        }

        return tagAttribute;
    }

    //get content in between two pair of given tag
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

    //refresh html page
    public void refresh() throws IOException{
        response = null;
        response = getHtmlStringBuffer();
    }

    //create new http connection
    private void createConnection(URL url) throws IOException{
        //create connection
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        connection.setFollowRedirects(true);
        connection.setDoInput(true);
        connection.setDoOutput(true);
    }
}
