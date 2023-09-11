package com.example.blechatdemo.file;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@SuppressLint("StaticFieldLeak")
//AsyncTask that copies a selected file from the device's file system to the application's cache directory
public class CopyFileToAppDirTask extends AsyncTask<Uri, Void, String> {
    private AlertDialog mAlertDialog;
    public static final String FILE_BROWSER_CACHE_DIR = "CertCache";
    private Context mcontext;
    FileTaskListener delegate;
    public CopyFileToAppDirTask(Context context,FileTaskListener callback) {
        mcontext = context;
        delegate = callback;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        AlertDialog.Builder builder = new AlertDialog.Builder(mcontext);
        builder.setMessage("Please wait...");
        builder.setCancelable(false);
        mAlertDialog = builder.create();
        mAlertDialog.show();
    }

    protected String doInBackground(Uri... uris) {
        try {
            return writeFileContent(uris[0]);
        } catch (IOException e) {
            Log.d("Failed to copy file {}", e.getMessage());
            return null;
        }
    }

    protected void onPostExecute(String cachedFilePath) {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
        }
        if (cachedFilePath != null) {
            Log.d("Cached file path {}" ,""  + cachedFilePath.length());
            delegate.onFileReceived(new File(cachedFilePath));
        } else {
            Log.d("Writing failed {}","");
        }

    }

//writing the content of the file to a specific location on the device's external storage.
    private String writeFileContent(final Uri uri) throws IOException {
        InputStream selectedFileInputStream =
                mcontext.getContentResolver().openInputStream(uri);
        if (selectedFileInputStream != null) {
            final File certCacheDir = new File(mcontext.getExternalFilesDir(null), FILE_BROWSER_CACHE_DIR);
            boolean isCertCacheDirExists = certCacheDir.exists();
            if (!isCertCacheDirExists) {
                isCertCacheDirExists = certCacheDir.mkdirs();
            }
            if (isCertCacheDirExists) {
                String filePath = certCacheDir.getAbsolutePath() + "/" + getFileDisplayName(uri);
                OutputStream selectedFileOutPutStream = new FileOutputStream(filePath);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = selectedFileInputStream.read(buffer)) > 0) {
                    selectedFileOutPutStream.write(buffer, 0, length);
                }
                selectedFileOutPutStream.flush();
                selectedFileOutPutStream.close();
                return filePath;
            }
            selectedFileInputStream.close();
        }
        return null;
    }

    // Returns file display name.
    @Nullable
    private String getFileDisplayName(final Uri uri) {
        String displayName = null;
        try (Cursor cursor = mcontext.getContentResolver()
                .query(uri, null, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                displayName = cursor.getString(
                        cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                Log.i("Display Name {}", displayName);

            }
        }

        return displayName;
    }
}

