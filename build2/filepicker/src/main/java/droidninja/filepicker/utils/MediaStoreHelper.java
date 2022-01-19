package droidninja.filepicker.utils;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;

import java.util.Comparator;
import java.util.List;

import droidninja.filepicker.FilePickerConst;
import droidninja.filepicker.cursors.DocScannerTask;
import droidninja.filepicker.cursors.loadercallbacks.FileMapResultCallback;
import droidninja.filepicker.cursors.loadercallbacks.FileResultCallback;
import droidninja.filepicker.cursors.loadercallbacks.PhotoDirLoaderCallbacks;
import droidninja.filepicker.models.Document;
import droidninja.filepicker.models.FileType;
import droidninja.filepicker.models.PhotoDirectory;

public class MediaStoreHelper {

    public static void getPhotoDirs(FragmentActivity activity, Bundle args, FileResultCallback<PhotoDirectory> resultCallback) {
        if (LoaderManager.getInstance(activity).getLoader(FilePickerConst.MEDIA_TYPE_IMAGE) != null)
            LoaderManager.getInstance(activity).restartLoader(FilePickerConst.MEDIA_TYPE_IMAGE, args, new PhotoDirLoaderCallbacks(activity, resultCallback));
        else
            LoaderManager.getInstance(activity).initLoader(FilePickerConst.MEDIA_TYPE_IMAGE, args, new PhotoDirLoaderCallbacks(activity, resultCallback));

    }

    public static void getVideoDirs(FragmentActivity activity, Bundle args, FileResultCallback<PhotoDirectory> resultCallback) {
        if (LoaderManager.getInstance(activity).getLoader(FilePickerConst.MEDIA_TYPE_VIDEO) != null)
            LoaderManager.getInstance(activity).restartLoader(FilePickerConst.MEDIA_TYPE_VIDEO, args, new PhotoDirLoaderCallbacks(activity, resultCallback));
        else
            LoaderManager.getInstance(activity).initLoader(FilePickerConst.MEDIA_TYPE_VIDEO, args, new PhotoDirLoaderCallbacks(activity, resultCallback));
    }

  public static void getDocs(FragmentActivity activity,
                             List<FileType> fileTypes,
                             Comparator<Document> comparator,
                             FileMapResultCallback fileResultCallback)
  {
    new DocScannerTask(activity, fileTypes, comparator, fileResultCallback).execute();
  }
}
