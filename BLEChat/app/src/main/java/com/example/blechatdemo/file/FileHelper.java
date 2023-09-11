package com.example.blechatdemo.file;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class FileHelper {
    public static final String FILE_SAVE_LOCATION = "SavedFiles/";

    public static void dumpImageMetaData(Uri uri, Activity activity) {

        // The query, because it only applies to a single document, returns only
        // one row. There's no need to filter, sort, or select fields,
        // because we want all fields for one document.
        Cursor cursor = activity.getContentResolver()
                .query(uri, null, null, null, null, null);

        try {
            // moveToFirst() returns false if the cursor has 0 rows. Very handy for
            // "if there's anything to look at, look at it" conditionals.
            if (cursor != null && cursor.moveToFirst()) {

                // Note it's called "Display Name". This is
                // provider-specific, and might not necessarily be the file name.
                String displayName = cursor.getString(
                        cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                Log.i("TAG", "Display Name: " + displayName);

                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                // If the size is unknown, the value stored is null. But because an
                // int can't be null, the behavior is implementation-specific,
                // and unpredictable. So as
                // a rule, check if it's null before assigning to an int. This will
                // happen often: The storage API allows for remote files, whose
                // size might not be locally known.
                String size = null;
                if (!cursor.isNull(sizeIndex)) {
                    // Technically the column stores an int, but cursor.getString()
                    // will do the conversion automatically.
                    size = cursor.getString(sizeIndex);
                } else {
                    size = "Unknown";
                }
                Log.i("TAG", "Size: " + size);
            }
        } finally {
            cursor.close();
        }
    }

    public static byte[] convertBytes(File file) {
        byte[] data;
        try (FileInputStream fis = new FileInputStream(file)) {
            int fileSize = (int)(file.length());
            if (fileSize > 5 * 1024 * 1024) {
                throw new IllegalArgumentException("File size exceeds the maximum limit of 5MB.");
            }
            data = new byte[fileSize];
            fis.read(data);
        } catch (IOException e) {
            Log.e("convertBytes", "Failed to read file", e);
            return null;
        }
        return data;
    }

    private static Uri contentUri = null;

    static Context context;

    public FileHelper( Context context) {

    }

    public static String  saveFile(byte[]data, Context mcontext) {
        context = mcontext;
       String fileNameWithExt = UUID.randomUUID().toString().replace("-","").concat(".txt");
        //String fileNameWithExt = FILE_SAVE_LOCATION.concat(UUID.randomUUID().toString().replace("-","")).concat(".txt");
        final File certCacheDir = new File(context.getExternalFilesDir(null), "SavedFiles"); //fileNameWithExt was there instead of SavedFiles
        Log.d("Saved location",""+certCacheDir);

        certCacheDir.mkdirs();// new new code
        boolean isCertCacheDirExists = certCacheDir.exists();

        Log.d("Saved location exists",""+isCertCacheDirExists);// new new code
        File outputFile = new File(certCacheDir.getPath(),fileNameWithExt);// new new code

        if (!isCertCacheDirExists) {
            try {
               // certCacheDir.createNewFile();
                outputFile.createNewFile();// new new code
            } catch (IOException e) {
                e.printStackTrace();
            }
            isCertCacheDirExists = true;

//            isCertCacheDirExists = certCacheDir.mkdirs();
//            try {
//                certCacheDir.createNewFile();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }

        if (isCertCacheDirExists){
            try {
                FileOutputStream fos = new FileOutputStream(outputFile);//certCacheDir was there instead of outputFile
                fos.write(data);
                fos.close();
                Log.d("File saved", "done:" + certCacheDir.exists());//certCacheDir was there instead of outputFile
                Log.d("File saved", "Size:" + certCacheDir.length());
                return outputFile.getPath(); //certCacheDir was there instead of outputFile
            } catch (Exception e) {
                throw new RuntimeException("File could not be saved.", e);
            }
        }
        return null; //null instead of ""
    }
}


