package com.loopytime.helper;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.loopytime.im.R;
import com.loopytime.utils.Constants;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by hitasoft on 7/3/17.
 */

public class StorageManager {

    private static StorageManager mInstance;
    private final String TAG = this.getClass().getSimpleName();
    Context context;

    /*Constants for file manipulation*/
    public static final String TAG_STATUS = "status";
    public static final String TAG_SENT = "sent";
    public static final String TAG_IMAGE_SENT = "image_sent";
    public static final String TAG_VIDEO_SENT = "video_sent";
    public static final String TAG_AUDIO_SENT = "audio_sent";
    public static final String TAG_FILE_SENT = "file_sent";
    public static final String TAG_AUDIO = "audio";
    public static final String TAG_VIDEO = "video";
    public static final String TAG_THUMB = "thumb";

    public StorageManager(Context context) {
        this.context = context;
    }

    public static synchronized StorageManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new StorageManager(context);
        }
        return mInstance;
    }

    public String saveToSdCard(Bitmap bitmap, String from, String filename) {

        String stored = "";
        File sdcard = Environment.getExternalStorageDirectory();
        String path = getFolderPath(from);
        File folder = new File(sdcard.getAbsoluteFile(), path);

        if (from.equals("sent") || from.equals(Constants.TAG_STATUS) || from.equals("profile") || from.equals(StorageManager.TAG_VIDEO_SENT)
                || from.equals(StorageManager.TAG_AUDIO_SENT)) {
            folder.mkdirs();
            File noMediaFile = new File(folder.getAbsoluteFile(), ".nomedia");
            try {
                if (!noMediaFile.exists()) {
                    noMediaFile.createNewFile();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            folder.mkdirs();
        }

        File file = new File(folder.getAbsoluteFile(), filename);
        if (file.exists())
            file.delete();

        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            stored = Constants.TAG_SUCCESS;
            if (!from.equals("sent") && !from.equals(Constants.TAG_STATUS) && !from.equals("profile") && !from.equals(StorageManager.TAG_VIDEO_SENT)
                    && !from.equals(StorageManager.TAG_AUDIO_SENT)) {
                Utils.refreshGallery(TAG, context, file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stored;
    }

    public String getFolderPath(String from) {
        String path = "";
        switch (from) {
            case "sent":
                path = "/" +Constants.FOLDER + "/Images/Sent/";
                break;
            case "profile":
                path = "/" +Constants.FOLDER + "/Images/.Profile/";
                break;
            case "thumb":
                path = "/" +Constants.FOLDER + "/Images/.thumbnails/";
                break;
            case "status":
                path = "/" +Constants.FOLDER+ "/." +  "Statuses/";
                break;
            case "audio":
                path = "/" +Constants.FOLDER + "/Audios/";
                break;
            case TAG_AUDIO_SENT:
                path = "/" +Constants.FOLDER + "/Audios/Sent/";
                break;
            case TAG_VIDEO:
                path = "/" +Constants.FOLDER + "/Videos/";
                break;
            case TAG_VIDEO_SENT:
                path = "/" +Constants.FOLDER + "/Videos/Sent/";
                break;
            case TAG_FILE_SENT:
                path = "/" +Constants.FOLDER + "/Files/Sent/";
                break;
            default:
                path = "/" +Constants.FOLDER + "/Files/";
                break;
        }
        return path;
    }

    public String getFilePath(String from, String fileName) {
        File sdcard = Environment.getExternalStorageDirectory();
        String path = getFolderPath(from) + fileName;
        File file = new File(sdcard.getAbsoluteFile(), path);
        if (file.exists()) {
            return file.getAbsolutePath();
        } else {
            return "";
        }
    }

    public boolean saveImageToSentPath(String imagePath, String imageName) {
        String path = Environment.getExternalStorageDirectory() + "/" +Constants.FOLDER + "/Images/Sent/";
        File folder = new File(path);
        if (!folder.exists()) {
            folder.mkdirs();
            File nomediaFile = new File(folder.getAbsoluteFile(), ".nomedia");
            try {
                if (!nomediaFile.exists()) {
                    nomediaFile.createNewFile();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        File from = new File(imagePath);
        File to = new File(folder + "/" + imageName);
        Log.v(TAG, "saveImageToSentPathFrom= " + from);
        Log.v(TAG, "saveImageToSentPathTo= " + to);
        if (from.exists()) {
            try {
                FileUtils.copyFile(from, to);
                Utils.refreshGallery(TAG, context, to);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.v(TAG, "moved");
        }
        return true;
    }

    public String saveThumbNail(Bitmap bitmap, String filename) {
        String stored = "";

        String path = "/" +Constants.FOLDER + "/Images/.thumbnails/";
        File sdcard = Environment.getExternalStorageDirectory();
        File folder = new File(sdcard.getAbsoluteFile(), path);
        folder.mkdirs();

        File nomediaFile = new File(folder.getAbsoluteFile(), ".nomedia");
        try {
            if (!nomediaFile.exists()) {
                nomediaFile.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        File file = new File(folder.getAbsoluteFile(), filename);
        if (file.exists())
            return Constants.TAG_SUCCESS;

        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
            out.flush();
            out.close();
            stored = Constants.TAG_SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stored;
    }

    public String saveBitmapToExtFilesDir(Bitmap bitmap, String fileName) {
        File mDestDir = getExtFilesDir();
        if (mDestDir != null) {
            File mDestFile = new File(mDestDir.getPath() + File.separator + fileName);
            try {
                FileOutputStream out = new FileOutputStream(mDestFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush();
                out.close();
                return mDestFile.getAbsolutePath();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public File getImage(String from, String imageName) {

        File mediaImage = null;
        try {
            String root = Environment.getExternalStorageDirectory().toString();
            File myDir = new File(root);
            if (!myDir.exists())
                return null;

            String path = getFolderPath(from);
            mediaImage = new File(myDir.getPath() + path + imageName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mediaImage;
    }

    public boolean checkifImageExists(String from, String imageName) {
        File file = getImage(from, imageName);
        return file.exists();
    }

    public boolean moveFilesToSentPath(Context context, String type, String filePath, String fileName) {
        String folderPath = getFolderPath(type);
        File dir = new File(Environment.getExternalStorageDirectory() + "/" + folderPath);
        if (!dir.exists()) {
            dir.mkdirs();
            File nomediaFile = new File(dir.getAbsoluteFile(), ".nomedia");
            try {
                if (!nomediaFile.exists()) {
                    nomediaFile.createNewFile();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        File from = new File(filePath);
        File to = new File(dir + "/" + fileName);
        Log.v(TAG, "moveFilesToSentPathFrom= " + from);
        Log.v(TAG, "moveFilesToSentPathTo= " + to);
        if (from.exists()) {
            try {
                FileUtils.copyFile(from, to);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.v(TAG, "moveFilesToSentPathMoved");
        }
        return false;
    }

    public File getFile(String fileName, String fileType, String from) {
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root);
        if (!myDir.exists())
            return null;

        if (fileType.equals("audio")) {
            fileType = "Audios";
        } else if (fileType.equals("video")) {
            fileType = "Videos";
        } else {
            fileType = "Files";
        }

        if (from.equals("sent")) {
            from = "Sent/";
        } else {
            from = "";
        }

        File dir = new File(myDir.getPath() + "/" + Constants.FOLDER + "/" +fileType + "/" + from + "/" + fileName);

        return dir;
    }

    public boolean checkifFileExists(String fileName, String fileType, String from) {
        File dir = getFile(fileName, fileType, from);
        if (dir.exists()) {
            Log.d(TAG, "checkIfFileExists: " + true);
            return true;
        }
        return false;
    }

    public boolean checkStatusExists(String fileName, String fileType, String from) {
        String folderPath = Environment.getExternalStorageDirectory().toString() + "/" +
                getFolderPath(from) + "/" + fileName;
        File file = new File(folderPath);
        return file.exists();
    }

    void checkDeleteFile(String name, String fileType, String from) {
        if (fileType != null) {
            Log.d(TAG, "checkDeleteFile1: " + fileType);
            File file = getImage(from, name);
            Log.d(TAG, "checkDeleteFile2: " + file.getAbsolutePath());
            if (file.exists()) {
                deleteFileFromMediaStore(file);
            } else {
                File otherFile = getFile(name, fileType, from);
                Log.d(TAG, "checkDeleteFile3: " + otherFile.getAbsolutePath());
                if (otherFile.exists()) {
                    deleteFileFromMediaStore(otherFile);
                }
            }
        }
    }

    private void deleteFileFromMediaStore(final File file) {
        final ContentResolver contentResolver = context.getContentResolver();
        String canonicalPath;
        try {
            canonicalPath = file.getCanonicalPath();
        } catch (IOException e) {
            canonicalPath = file.getAbsolutePath();
        }
        final Uri uri = MediaStore.Files.getContentUri("external");
        final int result = contentResolver.delete(uri,
                MediaStore.Files.FileColumns.DATA + "=?", new String[]{canonicalPath});
        if (result == 0) {
            final String absolutePath = file.getAbsolutePath();
            if (!absolutePath.equals(canonicalPath)) {
                contentResolver.delete(uri,
                        MediaStore.Files.FileColumns.DATA + "=?", new String[]{absolutePath});
            }
        }
    }

    public boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }

    public File createFile(String from, String fileName) {
        return new File(Environment.getExternalStorageDirectory() + getFolderPath(from) + fileName);
    }

    public String getFileName(String filePath) {
        String imgSplit = filePath;
        int endIndex = imgSplit.lastIndexOf("/");
        if (endIndex != -1) {
            imgSplit = imgSplit.substring(endIndex + 1);
        }
        return imgSplit;
    }

    public void deleteCacheDir() {
        File mDataDir = ContextCompat.getExternalFilesDirs(context, null)[0];
        if (mDataDir.exists() && mDataDir.listFiles() != null)
            for (File file : mDataDir.listFiles()) {
                file.delete();
            }
    }

    public File getExtFilesDir() {
        File mDataDir = ContextCompat.getExternalFilesDirs(context, null)[0];
        if (!mDataDir.exists()) {
            mDataDir.mkdirs();
        }
        if (mDataDir.exists()) return mDataDir;
        else return null;
    }


    public File createDirectory(String from) {
        File folder = new File(Environment.getExternalStorageDirectory() + getFolderPath(from));
        if (!folder.exists()) {
            folder.mkdirs();
        }
        if (from.equals("sent") || from.equals(Constants.TAG_STATUS) || from.equals("profile") || from.equals(StorageManager.TAG_VIDEO_SENT)
                || from.equals(StorageManager.TAG_AUDIO_SENT) || from.equals(StorageManager.TAG_FILE_SENT)) {
            File noMediaFile = new File(folder.getAbsoluteFile(), ".nomedia");
            try {
                if (!noMediaFile.exists()) {
                    noMediaFile.createNewFile();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return folder;
    }
}


