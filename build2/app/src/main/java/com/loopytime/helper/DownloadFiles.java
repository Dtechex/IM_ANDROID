package com.loopytime.helper;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.loopytime.im.R;
import com.loopytime.utils.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by hitasoft on 30/6/18.
 */

public abstract class DownloadFiles extends AsyncTask<String, Void, String> {
    private Context context;
    private static final String TAG = "DownloadFiles";
    private StorageManager storageManager;

    public DownloadFiles(Context context) {
        this.context = context;
        storageManager = StorageManager.getInstance(context);
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
        Log.i(TAG, "onProgressUpdate: " + values);
    }

    @Override
    protected String doInBackground(String... strings) {
        try {
            String filename = getFileName(strings[0]);
            String type = null;
            if (strings[1].equals("audio")) {
                type = "Audios";
            } else if (strings[1].equals("video")) {
                type = "Videos";
            } else {
                type = "Files";
            }

            URL url = new URL(strings[0]);//Create Download URl
            Log.i(TAG, "doInBackground: " + url);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();//Open Url Connection
            httpConn.setRequestMethod("GET");//Set Request Method to "GET" since we are grtting data
            httpConn.connect();//connect the URL Connection
            File outputFile;

            int responseCode = httpConn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // always check HTTP response code first
                // opens input stream from the HTTP connection
                InputStream inputStream = httpConn.getInputStream();
                FileOutputStream outputStream;
                // opens an output stream to save into file
                if (strings[1].equals(Constants.TAG_STATUS)) {
                    File folderPath = new File(Environment.getExternalStorageDirectory() +
                            storageManager.getFolderPath(Constants.TAG_STATUS));
                    if (!folderPath.exists()) folderPath.mkdirs();
                    String filePath = folderPath + "/" + filename;
                    Log.i(TAG, "doInBackground: " + filePath);
                    outputFile = new File(filePath);//Create Output file in Main File
                    if (!outputFile.exists()) {
                        outputFile.createNewFile();
                    }
                    outputStream = new FileOutputStream(outputFile);
                } else {
                    File mediaStorageDir = new File(Environment.getExternalStorageDirectory() + "/" + context.getString(R.string.app_name) + "/" + Constants.FOLDER + type);
                    // Create the storage directory if it does not exist
                    if (!mediaStorageDir.exists()) {
                        mediaStorageDir.mkdirs();
                    }
                    outputFile = new File(mediaStorageDir, filename);//Create Output file in Main File
                    //Create New File if not present
                    if (!outputFile.exists()) {
                        outputFile.createNewFile();
                    }
                    outputStream = new FileOutputStream(outputFile);
                }

                int bytesRead = -1;
                byte[] buffer = new byte[1024];
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.close();
                inputStream.close();
                Utils.refreshGallery(TAG, context, outputFile);
                return outputFile.getAbsolutePath();
            }

        } catch (Exception e) {
            //Read exception if something went wrong
            e.printStackTrace();
            Log.e(TAG, "Download Error Exception " + e.getMessage());
        }
        return null;
    }

    private String getFileName(String url) {
        String imgSplit = url;
        int endIndex = imgSplit.lastIndexOf("/");
        if (endIndex != -1) {
            imgSplit = imgSplit.substring(endIndex + 1, imgSplit.length());
        }
        return imgSplit;
    }

    protected abstract void onPostExecute(String downPath);
}
