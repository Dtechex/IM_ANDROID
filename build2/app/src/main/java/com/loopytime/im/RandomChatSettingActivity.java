package com.loopytime.im;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.snackbar.Snackbar;
import com.loopytime.helper.MaterialColor;
import com.loopytime.helper.MaterialColors;
import com.loopytime.helper.NetworkUtil;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.loopytime.helper.NetworkUtil.NOT_CONNECT;

public class RandomChatSettingActivity extends AppCompatActivity {
    DatePickerDialog picker;
    MaterialColor color;
    TextView name, gen, bday;
    EditText country,city;
    boolean fromUser = false;

    void setColor() {
        color = MaterialColors.CONVERSATION_PALETTE.get(this.getSharedPreferences("wall", Context.MODE_PRIVATE).getInt(MaterialColors.THEME, 0));
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(color.toActionBarColor(this)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(color.toStatusBarColor(this));
        }


    }


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    SwitchCompat switchCompat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_random_chat_setting);
        getSupportActionBar().setTitle(R.string.ran_chat_set);

        setColor();
        gen = findViewById(R.id.gen);
        country = findViewById(R.id.country);
        city = findViewById(R.id.city);
        initGenPop();
        initDatePicker();

        switchCompat = ((SwitchCompat) findViewById(R.id.switchChat));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        name = findViewById(R.id.name);
        bday = findViewById(R.id.dob);

        name.setText(GetSet.getUserName());
        bday.setText(GetSet.getBday());
        gen.setText(GetSet.getGender());
        country.setText(GetSet.getCountry());
        city.setText(GetSet.getCity());
        switchCompat.setChecked(GetSet.isIsRandomOn());
        ((SwitchCompat) findViewById(R.id.switchChat)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (fromUser) {
                    findViewById(R.id.group).setVisibility(View.GONE);
                    fromUser = false;
                    return;
                }
                if (!validateValues())
                    return;
                bday.setEnabled(false);
                gen.setEnabled(false);
                switchCompat.setEnabled(false);
                findViewById(R.id.group).setVisibility(View.VISIBLE);
                if (isChecked) {
                    insertChat();
                } else {
                    deleteChat();
                }
            }
        });
    }

    public void onDOBClick(View view) {
        picker.show();
    }

    public void onGenClick(View view) {
        popupMenu.show();
    }

    PopupMenu popupMenu;

    void initGenPop() {
        popupMenu = new PopupMenu(this, gen);
        popupMenu.getMenu().add(0, 0, 0, "Male");
        popupMenu.getMenu().add(0, 0, 0, "Female");
        popupMenu.getMenu().add(0, 0, 0, "Other");
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                gen.setText(item.getTitle());
                return false;
            }
        });
    }

    boolean validateValues() {
        if (TextUtils.isEmpty(bday.getText())) {
            showAlert();
            //bday.performClick();
            switchCompat.setChecked(!switchCompat.isChecked());
            return false;
        }
        if (TextUtils.isEmpty(gen.getText())) {
            //Toast.makeText(this, R.string.valid_gen, Toast.LENGTH_SHORT).show();
            //gen.performClick();
            switchCompat.setChecked(!switchCompat.isChecked());
            showAlert();
            return false;
        }
        if (TextUtils.isEmpty(country.getText())) {
            //Toast.makeText(this, R.string.valid_gen, Toast.LENGTH_SHORT).show();
            //gen.performClick();
            switchCompat.setChecked(!switchCompat.isChecked());
            showAlert();
            return false;
        }
        if (TextUtils.isEmpty(city.getText())) {
            //Toast.makeText(this, R.string.valid_gen, Toast.LENGTH_SHORT).show();
            //gen.performClick();
            switchCompat.setChecked(!switchCompat.isChecked());
            showAlert();
            return false;
        }
        return true;
    }

    private void showAlert() {
        new AlertDialog.Builder(this).setTitle(R.string.fill_form)
                .setMessage(R.string.fill_form_msg).setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).show();
    }

    void initDatePicker() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -20);
        picker = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                cal.set(year, month, dayOfMonth);
                bday.setText(dayOfMonth + "-" + (month + 1) + "-" + year);

            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        picker.getDatePicker().setMaxDate(new Date().getTime());

    }

    private String isNetworkConnected() {
        return NetworkUtil.getConnectivityStatusString(this);
    }

    private void networkSnack() {
        fromUser = true;
        bday.setEnabled(true);
        gen.setEnabled(true);
        switchCompat.setEnabled(true);
        switchCompat.setChecked(!switchCompat.isChecked());
        Snackbar snackbar = Snackbar
                .make(findViewById(R.id.parentLay), getString(R.string.network_failure), Snackbar.LENGTH_SHORT);
        View sbView = snackbar.getView();
        TextView textView = sbView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        snackbar.show();
    }

    void insertChat() {
        ((TextView) findViewById(R.id.status)).setText(R.string.wait_pb);
        if (isNetworkConnected().equals(NOT_CONNECT)) {
            networkSnack();
            return;
        }


        MediaType MEDIA_TYPE_JPG = MediaType.parse("*/*");

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("name", GetSet.getUserName())
                .addFormDataPart("userId", GetSet.getUserId())
                .addFormDataPart("phone", GetSet.getphonenumber())
                .addFormDataPart("gender", gen.getText().toString())
                .addFormDataPart("bday", bday.getText().toString())
                .addFormDataPart("country", country.getText().toString())
                .addFormDataPart("city", city.getText().toString())
                .addFormDataPart("profile_url", GetSet.getImageUrl() == null ? "" : GetSet.getImageUrl())
                .build();


        System.out.println("ding dong tkkkkknp amzonxxnmju");
        String url = Constants.NODE_URL + "insertRandomChat";
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
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


                                     String finalRes = res;
                                     runOnUiThread(new Runnable() {
                                         @Override
                                         public void run() {
                                             try {
                                                 JSONObject obj = new JSONObject(finalRes);
                                                 if (obj.getBoolean("success")) {
                                                     Toast.makeText(ApplicationClass.getInstance(), "Random Chat Enabled", Toast.LENGTH_SHORT).show();
                                                     ApplicationClass.getInstance().setRandomChatSettings(switchCompat.isChecked(), bday.getText().toString(), gen.getText().toString(),country.getText().toString(),city.getText().toString());
                                                     findViewById(R.id.group).setVisibility(View.GONE);
                                                     setResult(Activity.RESULT_OK);
                                                     finish();

                                                 } else {
                                                     bday.setEnabled(true);
                                                     gen.setEnabled(true);
                                                     switchCompat.setEnabled(true);
                                                     //                                   reportProgress(-1);
                                                     fromUser = true;
                                                     switchCompat.setChecked(!switchCompat.isChecked());
                                                     Toast.makeText(RandomChatSettingActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                                                 }


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
                                         }
                                     });


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

    void deleteChat() {
        ((TextView) findViewById(R.id.status)).setText(R.string.wait_pb2);
        if (isNetworkConnected().equals(NOT_CONNECT)) {
            networkSnack();
            return;
        }

        MediaType MEDIA_TYPE_JPG = MediaType.parse("*/*");

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("userId", GetSet.getUserId())
                .build();


        System.out.println("ding dong tkkkkknp amzonxxnmju");
        String url = Constants.NODE_URL + "deleteRandomChat";
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
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


                                     String finalRes = res;
                                     runOnUiThread(new Runnable() {
                                         @Override
                                         public void run() {
                                             try {
                                                 JSONObject obj = new JSONObject(finalRes);
                                                 if (obj.getBoolean("success")) {
                                                     ApplicationClass.getInstance().setRandomChatSettings(switchCompat.isChecked(), bday.getText().toString(), gen.getText().toString(),country.getText().toString(),city.getText().toString());
                                                     Toast.makeText(ApplicationClass.getInstance(), "Random Chat Enabled", Toast.LENGTH_SHORT).show();
                                                     findViewById(R.id.group).setVisibility(View.GONE);
                                                     setResult(Activity.RESULT_OK);
                                                     finish();

                                                 } else {
                                                     //                                   reportProgress(-1);
                                                     bday.setEnabled(true);
                                                     gen.setEnabled(true);
                                                     switchCompat.setEnabled(true);
                                                     fromUser = true;
                                                     switchCompat.setChecked(!switchCompat.isChecked());
                                                     Toast.makeText(RandomChatSettingActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                                                 }


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
                                         }
                                     });


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