package com.loopytime.im.status;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;
import com.loopytime.helper.NetworkReceiver;
import com.loopytime.helper.NetworkUtil;
import com.loopytime.helper.ProgressRequestBody;
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
import org.w3c.dom.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit2.http.GET;

import static com.loopytime.helper.NetworkUtil.NOT_CONNECT;

public class TextStatus extends BaseActivity implements View.OnClickListener {
    private static final String TAG = "TextStatus";
    RelativeLayout parentLay, textLay, textLayout;
    ImageView changeColor, sendStatus;
    EditText editText;
    int textSize = 50, height = 0;
    double size = 1;
    StorageManager storageManager;
    InputFilter[] filters = new InputFilter[1];
    String beforeText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_status);
        parentLay = findViewById(R.id.parentLay);
        changeColor = findViewById(R.id.changeColor);
        sendStatus = findViewById(R.id.sendStatus);
        editText = findViewById(R.id.editText);
        textLay = findViewById(R.id.textLay);
        textLayout = findViewById(R.id.textLayout);
        storageManager = StorageManager.getInstance(this);
        height = editText.getLayoutParams().height;
        editText.setTextSize(textSize);
/*
        filters[0] = new InputFilter.LengthFilter(500);
        editText.setFilters(filters);*/

        changeBg();


        changeColor.setOnClickListener(this);
        sendStatus.setOnClickListener(this);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                beforeText = "" + s;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    sendStatus.setVisibility(View.VISIBLE);
                } else {
                    sendStatus.setVisibility(View.GONE);
                }

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    int l = editText.getLineCount();
                    if (l <= 16) {
                        if (l > 4 && l < 8) {
                            size = 1.8;
                        } else if (l >= 8 && l < 12) {
                            size = 2.1;
                        } else if (l >= 12) {
                            size = 2.4;
                        }
                        editText.setTextSize((float) (textSize / size));

                        if (s.length() > 500) {
                            editText.setText("" + beforeText);
                            editText.setSelection(beforeText.length() - 1);
                            ApplicationClass.showToast(TextStatus.this, getString(R.string.maximum_characters_limit_reached), Toast.LENGTH_SHORT);
                        }
                    } else {
                        editText.setText("" + beforeText);
                        editText.setSelection(beforeText.length() - 1);
                        ApplicationClass.showToast(TextStatus.this, getString(R.string.maximum_line_limit_reached), Toast.LENGTH_SHORT);
                    }

                }
                /*int l = editText.getLineCount();

                if(l<15 && s.length()<700) {
                    if (l < 10) {
                        size = 1.5;
                    }
                    editText.setTextSize((float) (textSize / size));
                } else {
                    int len= editText.getText().toString().length();
                    String temp = editText.getText().toString();
                    String str = temp.substring(0,len);
                    editText.setText(str);
                    editText.setSelection(len-1);
                    ApplicationClass.showToast(TextStatus.this,"limit Excced",Toast.LENGTH_SHORT);
                }*/

                /*editText.removeTextChangedListener(this);
                editText.setSelection(s.length());
                editText.addTextChangedListener(this);*/
            }
        });

    }


    @Override
    public void onNetworkChange(boolean isConnected) {
        ApplicationClass.showSnack(TextStatus.this, findViewById(R.id.parentLay), isConnected);
    }

    private void changeBg() {
        Random rnd = new Random();
        int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
        if (color == -1) {
            changeBg();
        } else {
            textLayout.setBackgroundColor(color);
        }
    }

    private void createImage() {
        /*textLayout.setDrawingCacheEnabled(true);
        textLayout.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        textLayout.layout(0, 0, textLayout.getWidth(), textLayout.getHeight());

        textLayout.buildDrawingCache(true);*/
        editText.clearFocus();
        Bitmap thumb = viewToBitmap(textLayout);
        String fileName = Constants.FOLDER+ "_" + System.currentTimeMillis() + ".JPG";
        File mDestDir = storageManager.getExtFilesDir();
        File mDestFile = new File(mDestDir.getPath() + File.separator + fileName);

        if (mDestDir != null) {
            try {
                FileOutputStream out = new FileOutputStream(mDestFile);
                thumb.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush();
                out.close();
                if (getIntent().getBooleanExtra(Constants.IS_POST, false)) {
                    uplodMedia(GetSet.getUserId(), "", 0, GetSet.getUserName(), GetSet.getphonenumber(), mDestFile.getAbsolutePath(), "img_");
                    return;
                }
                String imagePath = mDestFile.getAbsolutePath();
                HashMap<String, String> map = new HashMap<>();
                map.put(Constants.TAG_MESSAGE, "");
                map.put(Constants.TAG_ATTACHMENT, imagePath);
                map.put(Constants.TAG_TYPE, "image");
                Intent intent = new Intent(this, NewGroupActivity.class);
                intent.putExtra(Constants.TAG_FROM, "status");
                //intent.putExtra(Constants.IS_POST, getIntent().getBooleanExtra(Constants.IS_POST,false));
                intent.putExtra(Constants.TAG_MESSAGE_DATA, map);
                startActivityForResult(intent, Constants.STATUS_TEXT_CODE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Bitmap viewToBitmap(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        editText.setCursorVisible(true);
        return bitmap;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == Constants.STATUS_TEXT_CODE) {
            setResult(Activity.RESULT_OK);
            finish();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.changeColor:
                changeBg();
                break;
            case R.id.sendStatus:
                ApplicationClass.preventMultiClick(sendStatus);
                editText.setCursorVisible(false);
                if (NetworkReceiver.isConnected()) {
                    createImage();
                } else {
                    ApplicationClass.showSnack(TextStatus.this, findViewById(R.id.parentLay), false);
                }
                break;
        }
    }

    private String isNetworkConnected() {
        return NetworkUtil.getConnectivityStatusString(this);
    }

    private void networkSnack() {
        sendStatus.setVisibility(View.VISIBLE);
        findViewById(R.id.progressbar).setVisibility(View.GONE);
        Snackbar snackbar = Snackbar
                .make(findViewById(R.id.parentLay), getString(R.string.network_failure), Snackbar.LENGTH_SHORT);
        View sbView = snackbar.getView();
        TextView textView = sbView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        snackbar.show();
    }

    void uplodMedia(String uid, String text2, int opt, String uname, String _phoneNumber, String pending_file, String type) {
        if (isNetworkConnected().equals(NOT_CONNECT)) {
            networkSnack();
            return;
        }
        sendStatus.setVisibility(View.GONE);
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
}
