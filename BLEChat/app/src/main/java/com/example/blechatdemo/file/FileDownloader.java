package com.example.blechatdemo.file;

import android.os.AsyncTask;
import android.os.Environment;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FileDownloader extends AsyncTask<String, Integer, Boolean> {
    private static final int BUFFER_SIZE = 4096;

    private FileDownloadListener listener;

    public FileDownloader(FileDownloadListener listener) {
        this.listener = listener;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        String fileUrl = params[0];
        String saveFilePath = params[1];

        try {
            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            // Check if the server returned a successful response code
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return false;
            }

            // Get the file size
            int fileSize = connection.getContentLength();

            // Create a file object to save the downloaded file
            File file = new File(saveFilePath);

            // Create directories if needed
            File directory = file.getParentFile();
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Create a buffered input stream for efficient reading
            InputStream inputStream = new BufferedInputStream(connection.getInputStream());

            // Create a file output stream to save the downloaded file
            FileOutputStream fileOutputStream = new FileOutputStream(file);

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long totalBytesRead = 0;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                // Calculate the progress percentage
                int progress = (int) ((totalBytesRead * 100) / fileSize);

                // Publish the progress
                publishProgress(progress);
            }

            // Close the streams
            fileOutputStream.close();
            inputStream.close();
            connection.disconnect();

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (listener != null) {
            listener.onDownloadProgress(values[0]);
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (listener != null) {
            if (result) {
                listener.onDownloadCompleted();
            } else {
                listener.onDownloadFailed();
            }
        }
    }

    public interface FileDownloadListener {
        void onDownloadProgress(int progress);
        void onDownloadCompleted();
        void onDownloadFailed();
    }
}
