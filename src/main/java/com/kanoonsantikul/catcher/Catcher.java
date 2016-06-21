package com.kanoonsantikul.catcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.HttpURLConnection;
import java.net.URL;

public class Catcher{
    private static final int BUFFER_SIZE = 4096;
    private static final int FILE_UNAVAILABLE_CODE = 1;
    private static final int FILE_UNREACHABLE_CODE = 2;
    private static final int FILE_REACHED_CODE = 3;

    private static String SAVE_DIR;

    public static void main(String[] args){

        Scanner userInput = new Scanner(System.in);
        URL url;
        HttpURLConnection connection;
        String hostUrl;
        ArrayList<String> fileUrls = new ArrayList<String>();

        System.out.print("Input url : ");
        hostUrl = userInput.next();

        try{
            //get connection
            connection = createConnection(new URL(hostUrl));

            int responseCode = connection.getResponseCode();
            System.out.println("\nSending 'GET' request to URL : " + hostUrl);
    		System.out.println("Response Code : " + responseCode);

            //get html text
            StringBuffer response = getResponseHtml(connection);

            //create pattern matcher
            Pattern MY_PATTERM = Pattern.compile(
                    "<a class='list-group-item' href='([^>]+)'(.+?)>(.+?)</a>",
                    Pattern.MULTILINE);
            Matcher matcher = MY_PATTERM.matcher(response.toString());

            //extract name
            matcher.find();
            String itemName = matcher.group(3);
            itemName = itemName.substring(14, itemName.length() - 7);
            SAVE_DIR = "/home/" + System.getProperty("user.name") + "/Downloads/" + itemName;
            println("\nFounds item : " + itemName + "\nDownload to directory : " + SAVE_DIR);

            //extract link from html
            matcher.reset();
            if(matcher.groupCount() != 0){
                println("\nFound urls : ");
                int i = 0;
                while(matcher.find()){
                    String s = matcher.group(1);
                    fileUrls.add(s);
                    println(++i + " : " + s);
                }
            }

            if(fileUrls.size() > 0){
                //create directory
                File targetDir = new File(SAVE_DIR);
                if(!targetDir.exists() && !targetDir.mkdirs()){
                    throw new IllegalStateException("Couldn't create dir: " + SAVE_DIR);
                }

                //get file start index
                System.out.print("\nStart at file : ");
                int startAt = Integer.parseInt(userInput.next());

                String fileUrl;
                for(int i = startAt - 1 ; i < fileUrls.size(); i++){
                    fileUrl = fileUrls.get(i);
                    String filePart = "" + (i+1);
                    while(filePart.length() != 3){
                        filePart = "0" + filePart;
                    }

                    do{
                        int returnCode = downloadFile(fileUrl, SAVE_DIR, itemName + "." + filePart);
                        if(returnCode == FILE_UNAVAILABLE_CODE);{
                            try {
                                println("File not available right now : Please wait...");
                                Thread.sleep(1000 * 60 * 10);
                            } catch(InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }while(returnCode == FILE_UNAVAILABLE_CODE);

                    if(returnCode == FILE_UNREACHABLE_CODE){
                        println("\nNo file to download. Server replied HTTP code: " + responseCode);
                        println("Connection error");
                        break;
                    }
                }
            }

            connection.disconnect();

        }catch(Exception e){
            if(e != null) println(e.toString());
        }
    }

    public static HttpURLConnection createConnection(URL url)
            throws Exception{
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

    public static StringBuffer getResponseHtml(HttpURLConnection connection)
            throws Exception{
        //get result
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response;
    }

    public static int downloadFile(String fileURL, String saveDir, String fileName)
            throws Exception{
        HttpURLConnection connection = createConnection(new URL(fileURL));
        int responseCode = connection.getResponseCode();

        if(responseCode == HttpURLConnection.HTTP_OK){
            String disposition = connection.getHeaderField("Content-Disposition");
            String contentType = connection.getContentType();
            int contentLength = connection.getContentLength();

            if(contentType.contains("force-download")){
                if(fileName == null){
                    if (disposition != null) {
                        // extracts file name from header field (indeirect link)
                        int index = disposition.indexOf("filename=");
                        if (index > 0) {
                            fileName = disposition.substring(index + 9, disposition.length());
                        }
                    } else {
                        // extracts file name from URL (direct link)
                        fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1, fileURL.length());
                    }
                }

                println("\nDownloading file : " + fileName);
                println("Content-Length : " + contentLength);
                println("Content-Type : " + contentType);
                println("Content-Disposition : " + disposition);
                println("");

                // opens input stream from the HTTP connection
                InputStream inputStream = connection.getInputStream();
                String saveFilePath = saveDir + File.separator + fileName;

                // opens an output stream to save into file
                FileOutputStream outputStream = new FileOutputStream(new File(saveFilePath));

                int bytesRead = -1;
                byte[] buffer = new byte[BUFFER_SIZE];
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.close();
                inputStream.close();

                println("File downloaded");
                connection.disconnect();
                return FILE_REACHED_CODE;

            } else {
                connection.disconnect();
                return FILE_UNAVAILABLE_CODE;
            }
        } else{
            connection.disconnect();
            return FILE_UNREACHABLE_CODE;
        }
    }

    public static void println(String text){
        System.out.println(text);
    }
}
