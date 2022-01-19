package com.loopytime.helper;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.snackbar.Snackbar;
import com.loopytime.im.R;
import com.loopytime.model.ChannelResult;
import com.loopytime.model.ContactsData;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;
import static com.loopytime.utils.Constants.TAG_MY_CONTACTS;
import static com.loopytime.utils.Constants.TAG_NOBODY;
import static com.loopytime.utils.Constants.TRUE;

public class Utils {

    private static String TAG = Utils.class.getSimpleName();
    private final Context context;
    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    public static boolean isInteger(String str) {
        int length = str.length();
        if (str == null) {
            return false;
        }
        if (str.isEmpty()) {
            return false;
        }
        int i = 0;

        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
    public static  void launchMarket(Context ctx) {
        Uri uri = Uri.parse("market://details?id=" + ctx.getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            ctx.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + ctx.getPackageName())));
            Toast.makeText(ctx, "couldn't launch the market", Toast.LENGTH_LONG).show();
        }
    }
    public Utils(Context context) {
        this.context = context;
        pref = context.getSharedPreferences("SavedPref", MODE_PRIVATE);
        editor = pref.edit();
    }

    public static String getURLForResource(int resourceId) {
        return Uri.parse("android.resource://com.hitasoft.loopytime.hiddy/" + resourceId).toString();
    }
    public static boolean feedMediaExist(String fileName) {
        String externalPath = Environment.getExternalStorageDirectory().getAbsolutePath();

        File dest = new File(externalPath + "/Indian-Messenger/feeds/");
        dest.mkdirs();

        // String prefix = !fileName.contains("avatar") ? "" : fileName.substring(fileName.lastIndexOf('.'));

        File res = new File(dest, fileName);
        return res.exists();

    }
    public static String getFeedUrl(String fileName) {
        String externalPath = Environment.getExternalStorageDirectory().getAbsolutePath();

        File dest = new File(externalPath + "/Indian-Messenger/feeds/");
        dest.mkdirs();

        // String prefix = !fileName.contains("avatar") ? "" : fileName.substring(fileName.lastIndexOf('.'));

        File res = new File(dest, fileName);
        return res.getAbsolutePath();

    }
    public static void shareFeedMedia(String filePath, boolean isImage, Context ctx, boolean isWhatsApp) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        if (isWhatsApp) {
            shareIntent.setPackage("com.whatsapp");
        }
        Uri contentUri = Uri.parse(filePath);// FileProvider.getUriForFile(AndroidContext.getContext(), AndroidContext.getContext().getApplicationContext().getPackageName() + ".my.package.name.provider", (res));
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.putExtra(Intent.EXTRA_TEXT, ctx.getString(R.string.share_app));
        if (isImage)
            shareIntent.setType("image/*");
        else
            shareIntent.setType("video/*");
        ctx.startActivity(Intent.createChooser(shareIntent, "Share to:"));
    }

    public static String feedMediaDrawableUrl(String fileName) {
        String externalPath = Environment.getExternalStorageDirectory().getAbsolutePath();

        File dest = new File(externalPath + "/Indian-Messenger/feeds/");
        dest.mkdirs();

        // String prefix = !fileName.contains("avatar") ? "" : fileName.substring(fileName.lastIndexOf('.'));

        File res = new File(dest, fileName);
        return res.getAbsolutePath();
    }
    public static String getFormattedDate(Context context, long smsTimeInMilis) {
        Calendar smsTime = Calendar.getInstance();
        smsTime.setTimeInMillis(smsTimeInMilis * 1000L);

        Calendar now = Calendar.getInstance();

        final String timeFormatString = "h:mm aa";
        final String dateTimeFormatString = "EEE, MMM d";
        final long HOURS = 60 * 60 * 60;
        if (now.get(Calendar.DATE) == smsTime.get(Calendar.DATE)) {
            return getDate(timeFormatString, smsTime.getTimeInMillis());
        } else if (now.get(Calendar.DATE) - smsTime.get(Calendar.DATE) == 1) {
            return context.getString(R.string.yesterday);
        } else if (now.get(Calendar.YEAR) == smsTime.get(Calendar.YEAR)) {
            return getDate(dateTimeFormatString, smsTime.getTimeInMillis());
        } else {
            return getDate("MMM dd yyyy", smsTime.getTimeInMillis());
        }
    }

    private static String getDate(String format, long time) {
        java.text.DateFormat sdf = new SimpleDateFormat(format, Locale.ENGLISH);
        Date netDate = (new Date(time));
        return sdf.format(netDate);
    }

    public static String getCreatedFormatDate(Context context, long smsTimeInMilis) {
        Calendar smsTime = Calendar.getInstance();
        smsTime.setTimeInMillis(smsTimeInMilis * 1000L);

        Calendar now = Calendar.getInstance();

        final String timeFormatString = "h:mm aa";
        final String dateTimeFormatString = "EEE, MMM d";
        final long HOURS = 60 * 60 * 60;
        if (now.get(Calendar.DATE) == smsTime.get(Calendar.DATE)) {
            return String.valueOf(context.getString(R.string.today) + " " + getDate(timeFormatString, smsTime.getTimeInMillis()));
        } else if (now.get(Calendar.DATE) - smsTime.get(Calendar.DATE) == 1) {
            return context.getString(R.string.yesterday);
        } else if (now.get(Calendar.YEAR) == smsTime.get(Calendar.YEAR)) {
            return getDate(dateTimeFormatString, smsTime.getTimeInMillis());
        } else {
            return getDate("MMM dd yyyy", smsTime.getTimeInMillis());
        }
    }

    public byte[] getBytes(InputStream is) throws IOException {
        ByteArrayOutputStream byteBuff = new ByteArrayOutputStream();
        int buffSize = 1024;
        byte[] buff = new byte[buffSize];
        int len = 0;
        while ((len = is.read(buff)) != -1) {
            byteBuff.write(buff, 0, len);
        }
        return byteBuff.toByteArray();
    }

    public static String isNetworkConnected(Context context) {
        return NetworkUtil.getConnectivityStatusString(context);
    }

    public static void networkSnack(CoordinatorLayout mainLay, Context context) {
        Snackbar snackbar = Snackbar
                .make(mainLay, context.getString(R.string.network_failure), Snackbar.LENGTH_SHORT);
        View sbView = snackbar.getView();
        TextView textView = sbView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        snackbar.show();
    }

    public static Spanned fromHtml(String html) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT);
        } else {
            return Html.fromHtml(html);
        }
    }

    public static boolean isUserAdminInChannel(ChannelResult.Result channelData) {
        if (channelData.channelAdminId != null && channelData.channelAdminId.equalsIgnoreCase(GetSet.getUserId())) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isChannelAdmin(ChannelResult.Result channelData, String userId) {
        return channelData.channelAdminId != null && channelData.channelAdminId.equalsIgnoreCase(userId);
    }

    public static boolean isProfileEnabled(ContactsData.Result result) {
        if (result.privacy_profile_image.equalsIgnoreCase(TAG_MY_CONTACTS)) {
            return result.contactstatus != null && result.contactstatus.equalsIgnoreCase(TRUE);
        } else return !result.privacy_profile_image.equalsIgnoreCase(TAG_NOBODY);
    }

    public static boolean isLastSeenEnabled(ContactsData.Result result) {
        if (result.privacy_last_seen.equalsIgnoreCase(TAG_MY_CONTACTS)) {
            return result.contactstatus != null && result.contactstatus.equalsIgnoreCase(TRUE);
        } else return !result.privacy_last_seen.equalsIgnoreCase(TAG_NOBODY);
    }

    public static boolean isAboutEnabled(ContactsData.Result result) {
        if (result.privacy_about.equalsIgnoreCase(TAG_MY_CONTACTS)) {
            return result.contactstatus != null && result.contactstatus.equalsIgnoreCase(TRUE);
        } else return !result.privacy_about.equalsIgnoreCase(TAG_NOBODY);
    }

    static void refreshGallery(String TAG, Context context, File file) {

        try {
            final Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            final Uri contentUri = Uri.fromFile(file);
            scanIntent.setData(contentUri);
            context.sendBroadcast(scanIntent);
            MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, null, new MediaScannerConnection.OnScanCompletedListener() {
                @Override
                public void onScanCompleted(String path, Uri uri) {
                    Log.e(TAG, "Finished scanning " + file.getAbsolutePath());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getNavigationBarHeight() {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            int height = resources.getDimensionPixelSize(resourceId);
            editor.putInt(Constants.TAG_NAV_HEIGHT, height);
            editor.commit();
            return height;
        }
        return 0;
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        editor.putInt(Constants.TAG_STATUS_HEIGHT, result);
        editor.commit();
        return result;
    }
}
