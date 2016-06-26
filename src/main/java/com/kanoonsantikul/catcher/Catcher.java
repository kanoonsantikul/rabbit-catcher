package com.kanoonsantikul.catcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class Catcher{
    private static Scanner userInput = new Scanner(System.in);
    private HtmlDigger html;
    private String urlString;
    private String fileName = "";
    private String destinationDirectory;

    public Catcher(String urlString){
        this.urlString = urlString;

        //get html
        try{
            System.out.println("\nSending 'GET' request to URL : " + urlString);
            html = new HtmlDigger(urlString);

            //print out connection response
            HttpURLConnection connection = html.getHttpConnection();
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code : " + responseCode);
        } catch (IOException e){
            System.out.print("Cannot get html file : ");
            System.out.println(e.toString());
        }
    }

    public static void main(String[] args){
        System.out.print("Input url : ");
        String urlString = userInput.next();
        (new Catcher(urlString)).run();
    }

    public void run(){
        //get link tag
        ArrayList<String> linkTags = html.findTag("a", HtmlDigger.TWO_TAG);
        linkTags = new ArrayList(linkTags.subList(11, linkTags.size() - 2));

        ArrayList<String> fileUrls;
        try{
            fileUrls = collectFileUrls(linkTags);
            System.out.println("collect " + fileUrls.size() + "part");
            for(int i = 0; i < fileUrls.size(); i++){
                System.out.println((i+1) + " : " + fileUrls.get(i));
            }
        } catch(IOException e){
            System.out.print("Cannot collect file urls : ");
            System.out.println(e.toString());
            return;
        }

        //extract file name from file url
        fileName = html.getTagContent(linkTags.get(0));
        fileName = fileName.substring(40, fileName.length() - 7);
        destinationDirectory = "/home/" + System.getProperty("user.name") + "/Downloads/" + fileName;
        System.out.println("\nFile : " + fileName + "\nwill be download to directory : " + destinationDirectory);

        //get file start index
        System.out.print("\nStart download at file : ");
        int startAt = Integer.parseInt(userInput.next());

        //download
        String filePart;
        String fileUrl;
        for(int i = startAt - 1 ; i < fileUrls.size(); i++){
            fileUrl = fileUrls.get(i); //get file url
            filePart = "" + (i+1); //get file part as three digit
            while(filePart.length() != 3){
                filePart = "0" + filePart;
            }

            int downloadStatus = download(fileUrl, fileName + "." + filePart);
            if(downloadStatus == FileDownloader.FILE_UNREACHABLE){
                System.out.println("Connection error");
            }
        }

    }

    private ArrayList<String> collectFileUrls(ArrayList<String> linkTags) throws IOException{
        ArrayList<String> fileUrls = new ArrayList<String>();

        //get fileUrls
        for(int i = 0; i < linkTags.size(); i++){
            String s = html.getTagAttribute(linkTags.get(i), "href");
            if(s != null){
                fileUrls.add(s);
            }
        }
        return fileUrls;
    }

    private int download(String fileUrl, String fileName){
        int downloadStatus;
        try{
            do{
                downloadStatus = FileDownloader.download(fileUrl)
                        .as(fileName)
                        .into(destinationDirectory)
                        .start();

                if(downloadStatus == FileDownloader.FILE_UNAVAILABLE);{
                    try {
                        System.out.println("File not available right now : Please wait...");
                        Thread.sleep(1000 * 60 * 10);
                    } catch(InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }while(downloadStatus == FileDownloader.FILE_UNAVAILABLE);
        } catch(Exception e){
            System.out.print("Error while download file");
            System.out.println(e.toString());
            downloadStatus = FileDownloader.FILE_UNREACHABLE;
        }
        return downloadStatus;
    }
}
