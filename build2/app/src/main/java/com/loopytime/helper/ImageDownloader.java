package com.loopytime.helper;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by hitasoft on 27/6/18.
 */

public abstract class ImageDownloader extends AsyncTask<String, String, Bitmap> {
    private Context context;
    private String TAG = "ImageDownloader";
    private Bitmap rotatedBitmap;
    int orientation = 0;

    public ImageDownloader(Context context) {
        this.context = context;
    }

    @Override
    protected Bitmap doInBackground(String... strings) {
        if (strings.length == 0 || strings[0] == null) {
            return null;
        }

        InputStream inputStream = getInputStream(strings[0]);
        ExifInterface exif = null;     //Since API Level 5
        try {
            if (inputStream != null) {
                exif = new ExifInterface(inputStream);
                orientation = exif.getRotationDegrees();
            }
            Log.e(TAG, " orientation " + orientation);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //load image directly
        Bitmap image = downloadImage(strings[0]);

        Bitmap rotatedBitmap = null;
        if (image == null) {
            return null;
        } else {
            try {
                final StorageManager imageStorage = StorageManager.getInstance(context);
                String filename = getFileName(strings[0]);
                rotatedBitmap = rotateImage(image, orientation);
                imageStorage.saveToSdCard(rotatedBitmap, strings[1], filename);

//                setOrientation(Uri.fromFile(imageStorage.getImage(strings[1], filename)), orientation, context);
            } catch (NullPointerException e) {
                e.printStackTrace();
                Log.e(TAG, "doInBackground: " + e.getMessage());
            }
        }
        return rotatedBitmap;
    }

    private Bitmap getRotatedBitmap(Bitmap image) {
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(image, 90);

            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(image, 180);

            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(image, 270);

            default:
                return image;
        }
    }


    public boolean setOrientation(Uri fileUri, int orientation, Context context) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.ORIENTATION, orientation);
        int rowsUpdated = context.getContentResolver().update(fileUri, values, null, null);
        return rowsUpdated > 0;
    }

    Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    public Bitmap downloadImage(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream inputStream = connection.getInputStream();
            return BitmapFactory.decodeStream(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public InputStream getInputStream(String src) {
        InputStream inputStream = null;
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            inputStream = connection.getInputStream();
            return inputStream;
        } catch (IOException e) {
            e.printStackTrace();
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

    protected abstract void onPostExecute(Bitmap imgBitmap);

    protected abstract void onProgressUpdate(String... progress);
}
