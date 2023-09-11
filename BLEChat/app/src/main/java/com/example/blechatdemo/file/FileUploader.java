package com.example.blechatdemo.file;
import android.content.Context;
import android.os.AsyncTask;
import android.webkit.MimeTypeMap;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class FileUploader extends AsyncTask<Void, Integer, String> {
    private Context context;
    private File file;
    private String serverUrl;
    private FileUploadListener listener;

    public FileUploader(Context context, File file, String serverUrl, FileUploadListener listener) {
        this.context = context;
        this.file = file;
        this.serverUrl = serverUrl;
        this.listener = listener;
    }

    @Override
    protected String doInBackground(Void... params) {
        String responseString = "";

        try {
            URL url = new URL(serverUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=*****");
            conn.setRequestProperty("ENCTYPE", "multipart/form-data");

            String fileName = file.getName();
            String mimeType = getMimeType(file);

            FileInputStream fileInputStream = new FileInputStream(file);
            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

            dos.writeBytes("--*****\r\n");
            dos.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"" + fileName + "\"" + "\r\n");
            dos.writeBytes("Content-Type: " + mimeType + "\r\n");
            dos.writeBytes("\r\n");

            byte[] buffer = new byte[1024];
            int bytesRead;
            long totalBytesRead = 0;
            long fileSize = file.length();
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                int progress = (int) ((totalBytesRead * 100) / fileSize);
                publishProgress(progress);
            }

            dos.writeBytes("\r\n");
            dos.writeBytes("--*****--\r\n");

            int serverResponseCode = conn.getResponseCode();
            String serverResponseMessage = conn.getResponseMessage();

            if (serverResponseCode == HttpURLConnection.HTTP_OK) {
                responseString = "File uploaded successfully"+ ", " + serverResponseMessage;
            } else {
                responseString = "File upload failed with error code: " + serverResponseCode + ", " + serverResponseMessage;
            }

            fileInputStream.close();
            dos.flush();
            dos.close();
        } catch (MalformedURLException e) {
            responseString = "MalformedURLException: " + e.getMessage();
        } catch (IOException e) {
            responseString = "IOException: " + e.getMessage();
        }

        return responseString;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (listener != null) {
            listener.onUploadProgress(values[0]);
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (listener != null) {
            listener.onUploadCompleted(result);
        }
    }

    private String getMimeType(File file) {
        String extension = getFileExtension(file.getName());
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }
    public interface FileUploadListener {
        void onUploadProgress(int progress);
        void onUploadCompleted(String result);
    }
}
