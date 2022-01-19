/*
 *  Copyright 2013 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.loopytime.apprtc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.widget.ScrollView;
import android.widget.TextView;

import com.loopytime.im.BuildConfig;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Singleton helper: install a default unhandled exception handler which shows
 * an informative dialog and kills the loopytime.  Useful for apps whose
 * error-handling consists of throwing RuntimeExceptions.
 * NOTE: almost always more useful to
 * Thread.setDefaultUncaughtExceptionHandler() rather than
 * Thread.setUncaughtExceptionHandler(), to apply to background threads as well.
 */
public class UnhandledExceptionHandler implements Thread.UncaughtExceptionHandler {
  private static final String TAG = "AppRTCMobileActivity";
  private final Activity activity;

  public UnhandledExceptionHandler(final Activity activity) {
    this.activity = activity;
  }

  @Override
  public void uncaughtException(Thread unusedThread, final Throwable e) {
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        String title = "Fatal error: " + getTopLevelCauseMessage(e);
        String msg = getRecursiveStackTrace(e);
        TextView errorView = new TextView(activity);
        errorView.setText(msg);
        errorView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8);
        ScrollView scrollingContainer = new ScrollView(activity);
        scrollingContainer.addView(errorView);
        Log.e(TAG, title + "\n\n" + msg);
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
sendEmail(getRecursiveStackTrace(e));
         //   System.exit(1);
          }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title)
            .setView(scrollingContainer)
            .setPositiveButton("Exit", listener)
            .show();
      }
    });
  }
  private void sendEmail(String crash) {
    try {

      String reportContetnt = "\n\n" + "DEVICE OS VERSION CODE: " + Build.VERSION.SDK_INT + "\n" +
              "DEVICE VERSION CODE NAME: " + Build.VERSION.CODENAME + "\n" +
              "VERSION CODE: " + BuildConfig.VERSION_CODE + "\n" +
              "VERSION NAME: " + BuildConfig.VERSION_NAME + "\n" +
              "PACKAGE NAME: " + BuildConfig.APPLICATION_ID + "\n" +
              "BUILD TYPE: " + BuildConfig.BUILD_TYPE + "\n\n\n" +
              crash;

      final Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
      emailIntent.setData(Uri.parse("mailto:")); // only email apps should handle this
      emailIntent.putExtra(Intent.EXTRA_EMAIL,
              new String[]{"ashish.nautiyal102@gmail.com"});
      emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Crash Report");
      emailIntent.putExtra(Intent.EXTRA_TEXT, reportContetnt);
      try {
        //start email intent
        activity.startActivity(Intent.createChooser(emailIntent, "Email"));
      } catch (Exception e) {
        //if any thing goes wrong for example no email client application or any exception
        //get and show exception message
        e.printStackTrace();
      }
    } catch (Exception e) {
      Log.e(TAG, "sendEmail: " + e.getMessage());
    }
  }


  // Returns the Message attached to the original Cause of |t|.
  private static String getTopLevelCauseMessage(Throwable t) {
    Throwable topLevelCause = t;
    while (topLevelCause.getCause() != null) {
      topLevelCause = topLevelCause.getCause();
    }
    return topLevelCause.getMessage();
  }

  // Returns a human-readable String of the stacktrace in |t|, recursively
  // through all Causes that led to |t|.
  private static String getRecursiveStackTrace(Throwable t) {
    StringWriter writer = new StringWriter();
    t.printStackTrace(new PrintWriter(writer));
    return writer.toString();
  }
}
