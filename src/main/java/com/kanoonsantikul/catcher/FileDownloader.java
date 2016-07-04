package com.kanoonsantikul.catcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class FileDownloader{
    public static final int FILE_UNAVAILABLE = 100;
    public static final int FILE_UNREACHABLE = 200;
    public static final int FILE_REACHED = 300;

    private static final int BUFFER_SIZE = 4096;

    private HttpURLConnection connection = null;
    private int responseCode = -7;
    private String urlString = null;
    private String destinationDirectory = null;
    private String fileName = null;

    //create new download task
    public static FileDownloader download(String urlString) throws
            MalformedURLException,
            IOException{
        URL url = new URL(urlString);
        HttpURLConnection connection = createConnection(url);

        FileDownloader fileDownloader = new FileDownloader();
        fileDownloader.setUrlString(urlString);
        fileDownloader.setConnection(connection);
        return fileDownloader;
    }

    //set file output file name
    public FileDownloader as(String fileName){
        this.fileName = fileName;
        return this;
    }

    //set file output directory to write data into
    public FileDownloader into(String destination){
        this.destinationDirectory = destination;

        //if directory doesn't exists create directory
        File targetDir = new File(destination);
        if(!targetDir.exists() && !targetDir.mkdirs()){
            throw new IllegalStateException("Couldn't create dir: " + targetDir);
        }

        return this;
    }

    //set custom connection to downloader
    public void setConnection(HttpURLConnection connection) throws IOException{
        this.connection = connection;
        this.responseCode = connection.getResponseCode();
    }

    //start the downloader
    public int start(boolean showStatus) throws IOException{
        int downloadStatus;

        if(responseCode == HttpURLConnection.HTTP_OK){
            String disposition = connection.getHeaderField("Content-Disposition");
            String contentType = connection.getContentType();
            int contentLength = connection.getContentLength();

            //System.out.println("Content-Type : " + contentType);
            //System.out.println("Content-Disposition : " + disposition);

            if(contentType.contains("force-download")){
                if(fileName == null){
                    fileName = getFileName(disposition);
                }

                if(showStatus){
                    System.out.println("Downloading file : " + fileName);
                    System.out.println("Content-Length : " + contentLength);
                }

                // opens input stream from the HTTP connection
                InputStream inputStream = connection.getInputStream();
                String destinationFile = destinationDirectory + "/" + fileName;

                // opens an output stream to save into file
                FileOutputStream outputStream = new FileOutputStream(
                        new File(destinationFile));

                int bytesRead = -1;
                int contentRead = 0;
                double percent;
                byte[] buffer = new byte[BUFFER_SIZE];
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    if(showStatus){
                        contentRead += bytesRead;
                        percent = (contentRead * 100.00) / contentLength ;
                        System.out.print("File downloaded : [" + percent + "%]   \r");
                    }
                }

                outputStream.close();
                inputStream.close();

                if(showStatus){
                    System.out.println("Download complete                         ");
                }

                downloadStatus = FILE_REACHED;
            } else{
                downloadStatus = FILE_UNAVAILABLE;
            }
        } else{
            downloadStatus = FILE_UNREACHABLE;
        }
        connection.disconnect();
        return downloadStatus;
    }

    //create new http connection
    private static HttpURLConnection createConnection(URL url) throws IOException{
        HttpURLConnection connection;

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

    //if custom file name doesn't set, auto generate it
    private String getFileName(String disposition){
        String fileName = null;
        if (disposition != null) { // extracts file name from header field (indirect link)
            int index = disposition.indexOf("filename=");
            if (index > 0) {
                fileName = disposition.substring(index + 9, disposition.length());
            }

        } else { // extracts file name from URL (direct link)
            fileName = urlString.substring(
                    urlString.lastIndexOf("/") + 1,
                    urlString.length());
        }
        return fileName;
    }

    //set http url to downloader instance
    private void setUrlString(String urlString){
        this.urlString = urlString;
    }
}
