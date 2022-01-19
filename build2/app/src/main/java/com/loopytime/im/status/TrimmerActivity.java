package com.loopytime.im.status;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.google.android.material.snackbar.Snackbar;
import com.loopytime.external.keyboard.KeyboardHeightObserver;
import com.loopytime.external.keyboard.KeyboardHeightProvider;
import com.loopytime.external.videotrimmer.HgLVideoTrimmer;
import com.loopytime.external.videotrimmer.interfaces.OnHgLVideoListener;
import com.loopytime.external.videotrimmer.interfaces.OnTrimVideoListener;
import com.loopytime.helper.NetworkUtil;
import com.loopytime.helper.ProgressRequestBody2;
import com.loopytime.helper.StorageManager;
import com.loopytime.im.ApplicationClass;
import com.loopytime.im.BaseActivity;
import com.loopytime.im.NewGroupActivity;
import com.loopytime.im.R;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.loopytime.helper.NetworkUtil.NOT_CONNECT;

public class TrimmerActivity extends BaseActivity implements OnTrimVideoListener, OnHgLVideoListener, KeyboardHeightObserver {

    private static final String TAG = TrimmerActivity.class.getSimpleName();
    private HgLVideoTrimmer mVideoTrimmer;
    private ProgressDialog mProgressDialog;
    String filePath, sourceType;
    private StorageManager storageManager;
    private EditText editText;
    private RelativeLayout bottomLay, mainLay;
    private LinearLayout editLay;
    private ImageView btnSend;
    private SharedPreferences pref;
    private KeyboardHeightProvider keyboardHeightProvider;
    private int bottomNavHeight, bottomMargin = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        setContentView(R.layout.activity_trimmer);
        pref = getSharedPreferences("SavedPref", MODE_PRIVATE);
        keyboardHeightProvider = new KeyboardHeightProvider(this);
        storageManager = StorageManager.getInstance(this);
        bottomMargin = ApplicationClass.dpToPx(this, 2);

        Intent extraIntent = getIntent();
        int maxDuration = 30;
        mVideoTrimmer = findViewById(R.id.timeLine);
        bottomLay = findViewById(R.id.bottom_lay);
        editText = findViewById(R.id.editText);
        btnSend = findViewById(R.id.send);
        mainLay = findViewById(R.id.mainLay);
        editLay = findViewById(R.id.editLay);
        mainLay.post(new Runnable() {
            public void run() {
                keyboardHeightProvider.start();
            }
        });

        mainLay.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                bottomNavHeight = view.getPaddingBottom() + windowInsets.getSystemWindowInsetBottom() + bottomMargin;
                return windowInsets.consumeSystemWindowInsets();
            }
        });

        if (Locale.getDefault().getLanguage().equals("ar")) {
            btnSend.setRotation(180);
        } else {
            btnSend.setRotation(0);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                initBottomPadding(bottomNavHeight + bottomMargin);
            }
        }, 200);

        if (extraIntent != null) {
            filePath = extraIntent.getStringExtra(Constants.TAG_ATTACHMENT);
            sourceType = extraIntent.getStringExtra(Constants.TAG_FROM);
            maxDuration = extraIntent.getIntExtra("story_time", 30);
        }

        //setting progressbar
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage(getString(R.string.trimming_progress));

        if (mVideoTrimmer != null) {
            /**
             * get total duration of video file
             */
            mVideoTrimmer.setMaxDuration(maxDuration);
            mVideoTrimmer.setOnTrimVideoListener(this);
            mVideoTrimmer.setOnHgLVideoListener(this);
            //mVideoTrimmer.setDestinationPath("/storage/emulated/0/DCIM/CameraCustom/");
            mVideoTrimmer.setVideoInformationVisibility(true);
            mVideoTrimmer.setVideoURI(Uri.parse(filePath));
            findViewById(R.id.send)
                    .setOnClickListener(
                            new View.OnClickListener() {
                                @RequiresApi(api = Build.VERSION_CODES.GINGERBREAD_MR1)
                                @Override
                                public void onClick(View view) {
                                    if (isNetworkConnected().equals(NOT_CONNECT)) {
                                        ApplicationClass.showSnack(TrimmerActivity.this, findViewById(R.id.mainLay), false);
                                    } else {
                                        mVideoTrimmer.onSaveClicked();
                                    }
                                }
                            }
                    );
        }
    }

    @Override
    public void onNetworkChange(boolean isConnected) {
        ApplicationClass.showSnack(TrimmerActivity.this, findViewById(R.id.mainLay), isConnected);
    }

    @Override
    public void onTrimStarted() {
        mProgressDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        keyboardHeightProvider.setKeyboardHeightObserver(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mVideoTrimmer != null)
            mVideoTrimmer.releasePlayer();
        keyboardHeightProvider.setKeyboardHeightObserver(null);
    }

    @Override
    protected void onStop() {
        if (mVideoTrimmer != null)
            mVideoTrimmer.releasePlayer();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        keyboardHeightProvider.close();
    }

    private void initBottomPadding(int bottomPadding) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        bottomLay.setPadding(0, 0, 0, bottomPadding);
        bottomLay.setLayoutParams(params);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onKeyboardHeightChanged(int height, int orientation) {
        // color the keyboard height view, this will remain visible when you close the keyboard
        if (height > 0) {
            initBottomPadding(bottomNavHeight);
        } else if (height < 0) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    initBottomPadding(bottomNavHeight + bottomMargin);
                }
            }, 100);
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    initBottomPadding(bottomNavHeight + bottomMargin);
                }
            }, 100);
        }
    }

    private void setToImmersiveMode() {
        // set to immersive
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void getResult(final Uri contentUri) {
        try {
            if (isNetworkConnected().equals(NOT_CONNECT)) {
                ApplicationClass.showSnack(TrimmerActivity.this, findViewById(R.id.mainLay), false);
            } else {
                if (contentUri != null && contentUri.getPath() != null && !TextUtils.isEmpty(contentUri.getPath())) {
                    File file = new File(contentUri.getPath());
                    String previewFilePath = file.getAbsolutePath();
                    if (getIntent().getBooleanExtra(Constants.IS_POST, false)) {
                        uplodMedia(GetSet.getUserId(), editText.getText().toString(), 0, GetSet.getUserName(), GetSet.getphonenumber(),previewFilePath, "vid_");
                        return;
                    }
                    Log.i(TAG, "getResult: " + previewFilePath);
                    mProgressDialog.cancel();
                    HashMap<String, String> map = new HashMap<>();
                    map.put(Constants.TAG_MESSAGE, "" + editText.getText().toString());
                    map.put(Constants.TAG_ATTACHMENT, previewFilePath);
                    map.put(Constants.TAG_TYPE, "video");
                    Intent intent = new Intent(this, NewGroupActivity.class);
                    intent.putExtra(Constants.TAG_FROM, StorageManager.TAG_STATUS);
                    intent.putExtra(Constants.TAG_SOURCE_TYPE, sourceType);
                    intent.putExtra(Constants.TAG_MESSAGE_DATA, map);
                    startActivityForResult(intent, Constants.STATUS_VIDEO_CODE);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(TrimmerActivity.this, contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }

    private String isNetworkConnected() {
        return NetworkUtil.getConnectivityStatusString(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == Constants.STATUS_VIDEO_CODE) {
            setResult(Activity.RESULT_OK);
            finish();
        }
    }

    @Override
    public void cancelAction() {
        mProgressDialog.cancel();
        mVideoTrimmer.destroy();
        onBackPressed();
    }

    @Override
    public void onError(final String message) {
        mProgressDialog.cancel();
        if (message != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(TrimmerActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onVideoPrepared() {
        /*runOnUiThread(new Runnable() {
            @Override
            public void run() {
                 Toast.makeText(TrimmerActivity.this, "onVideoPrepared", Toast.LENGTH_SHORT).show();
            }
        });*/
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
    void uplodMedia(String uid, String text2, int opt, String uname, String _phoneNumber, String pending_file, String type) {
        if (isNetworkConnected().equals(NOT_CONNECT)) {
            networkSnack();
            return;
        }
        btnSend.setVisibility(View.GONE);
        findViewById(R.id.progressbar).setVisibility(View.VISIBLE);
        if (pending_file.contains("external_files")) {
            File sdDir = Environment.getExternalStorageDirectory();
            pending_file = sdDir + pending_file.replace("external_files/", "");
        }
        System.out.println("ding dong tkkkkk nmn amzons" + type + " " + uid + " " + opt + " " + uname + " " + _phoneNumber + " " + text2);
        // pending_fileName=FileProvider.getUriForFile(AndroidContext.getContext(), AndroidContext.getContext().getApplicationContext().getPackageName() + ".my.package.name.provider", new File(pending_fileName)).getPath();
        MediaType MEDIA_TYPE_JPG = MediaType.parse("*/*");

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("type", type)
                .addFormDataPart("uid", uid)
                .addFormDataPart("isPublic", opt == 0 ? "0" : "1")
                .addFormDataPart("text2", "")

                .addFormDataPart("isFeed", String.valueOf(1))
                .addFormDataPart("uname", uname)
                .addFormDataPart("uPhone", _phoneNumber)
                .addFormDataPart("upic", GetSet.getImageUrl() == null ? "" : GetSet.getImageUrl())
                .addFormDataPart("newPrivateFeed", "true")
                .addFormDataPart("sampleFile", new File(pending_file).getName(),
                        RequestBody.create(MEDIA_TYPE_JPG, new File(pending_file)))
                .build();

        System.out.println("ding dong tkkkkk nmn amzon ll" + pending_file);
        String finalPending_file = pending_file;
        ProgressRequestBody2 prb = new ProgressRequestBody2(requestBody, new ProgressRequestBody2.Listener() {
            @Override
            public void onProgress(int progress) {
                System.out.println("ding dong tkkkkknp reslalal; " + progress + "##" + new File(finalPending_file).length());
                //     reportProgress(progress);
            }

        });


        System.out.println("ding dong tkkkkknp amzonxxnmju");
        String url = Constants.NODE_URL + "set_statusHiddi";
        Request request = new Request.Builder()
                .url(url)
                .post(prb)
                .build();
        System.out.println("ding dong tkkkkknp amzonxx");
        ApplicationClass.getInstance().httpClient.newCall(request)
                .enqueue(new Callback() {
                             @Override
                             public void onFailure(Call call, IOException e) {
                                 runOnUiThread(new Runnable() {
                                     @Override
                                     public void run() {
                                         networkSnack();
                                     }
                                 });
                             }

                             @Override
                             public void onResponse(Call call, Response response) {
                                 String res = null;
                                 try {
                                     res = response.body().string();
                                     System.out.println("ding dong tkkkkknp res" + res);
                                     try {
                                         JSONObject obj = new JSONObject(res);

                                         if (obj.getBoolean("success")) {


                                         } else if (obj.has("blocked") && obj.getInt("blocked") > 0) {
                                             //                                   reportProgress(-1);
                                         }

                                         runOnUiThread(new Runnable() {
                                             @Override
                                             public void run() {
                                                 setResult(Activity.RESULT_OK);
                                                 finish();
                                             }
                                         });

                                     } catch (JSONException e) {
                                         e.printStackTrace();
                                         runOnUiThread(new Runnable() {
                                             @Override
                                             public void run() {
                                                 networkSnack();
                                             }
                                         });
                                         //                             reportError();
                                     }

                                 } catch (IOException e) {
                                     //reportError();
                                     runOnUiThread(new Runnable() {
                                         @Override
                                         public void run() {
                                             networkSnack();
                                         }
                                     });
                                     e.printStackTrace();
                                 }

                             }
                         }
                );

    }


    private void networkSnack() {
        btnSend.setVisibility(View.VISIBLE);
        findViewById(R.id.progressbar).setVisibility(View.GONE);
        Snackbar snackbar = Snackbar
                .make(findViewById(R.id.mainLay), getString(R.string.network_failure), Snackbar.LENGTH_SHORT);
        View sbView = snackbar.getView();
        TextView textView = sbView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        snackbar.show();
    }
}
