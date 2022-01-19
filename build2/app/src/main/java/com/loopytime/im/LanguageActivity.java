package com.loopytime.im;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.loopytime.helper.LocaleManager;
import com.loopytime.model.LanguageData;
import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;
import com.loopytime.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class LanguageActivity extends BaseActivity {

    private final String TAG = this.getClass().getSimpleName();
    Toolbar toolbar;
    ImageView btnBack;
    TextView txtTitle;
    RecyclerView recyclerView;
    List<LanguageData> languageList = new ArrayList<>();
    LanguageAdapter adapter;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    static ApiInterface apiInterface;
    public static boolean languageChanged = false;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);

        apiInterface = ApiClient.getClient().create(ApiInterface.class);
        pref = LanguageActivity.this.getSharedPreferences("SavedPref", MODE_PRIVATE);
        editor = pref.edit();

        toolbar = findViewById(R.id.actionbar);
        btnBack = toolbar.findViewById(R.id.backbtn);
        txtTitle = toolbar.findViewById(R.id.title);
        recyclerView = findViewById(R.id.recyclerView);

        if(ApplicationClass.isRTL()){
            btnBack.setRotation(180);
        } else {
            btnBack.setRotation(0);
        }

        initToolBar();
        getLanguage();

        dialog = new ProgressDialog(LanguageActivity.this);
        dialog.setMessage(getString(R.string.pleasewait));
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

    }

    @Override
    public void onNetworkChange(boolean isConnected) {

    }

    private void initToolBar() {
        txtTitle.setVisibility(View.VISIBLE);
        btnBack.setVisibility(View.VISIBLE);
        txtTitle.setText(R.string.app_language);
        btnBack.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.primarytext));
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    public void getLanguage() {
        LanguageData data = new LanguageData();
        data = new LanguageData();
        data.languageId = "1";
        data.language = getString(R.string.english);
        data.languageCode = Constants.LANGUAGE_ENGLISH;
        data.isSelected = data.languageCode.equalsIgnoreCase(pref.getString(Constants.TAG_LANGUAGE_CODE, Constants.TAG_DEFAULT_LANGUAGE_CODE));
        languageList.add(data);

        adapter = new LanguageAdapter(languageList, getApplicationContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.setAdapter(adapter);
    }

    private class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder> {

        List<LanguageData> languageList = new ArrayList<>();
        Context context;

        LanguageAdapter(List<LanguageData> languageList, Context context) {
            this.languageList = languageList;
            this.context = context;
        }

        @NonNull
        @Override
        public LanguageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.language_item, parent, false);
            return new LanguageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final LanguageViewHolder holder, final int position) {
            final LanguageData data = languageList.get(position);

            holder.txtLanguage.setText(data.language);
            if (data.isSelected) {
                holder.btnLanguage.setChecked(true);
            } else {
                holder.btnLanguage.setChecked(false);
            }

            holder.mainLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!holder.btnLanguage.isChecked())
                        setNewLocale(data, context);
                }
            });

            holder.btnLanguage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setNewLocale(data, context);
                }
            });
        }

        private boolean setNewLocale(LanguageData languageData, Context mContext) {

            for (LanguageData data : languageList) {
                data.isSelected = data.languageCode.equalsIgnoreCase(languageData.languageCode);
            }
            adapter.notifyDataSetChanged();
            editor.putString(Constants.TAG_LANGUAGE_CODE, languageData.languageCode).commit();

            LocaleManager.setNewLocale(mContext, languageData.languageCode);
            /*languageChanged = true;*/


           /* Locale myLocale = new Locale(languageData.languageCode);
            Resources res = getResources();
            DisplayMetrics dm = res.getDisplayMetrics();
            Configuration conf = res.getConfiguration();
            conf.locale = myLocale;
            res.updateConfiguration(conf, dm);*/
            //dialog.show();

            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

           /* if (false) {
                System.exit(0);
            } else {
//                Toast.makeText(mContext, "Activity restarted", Toast.LENGTH_SHORT).show();
            }*/
            return true;
        }

        @Override
        public int getItemCount() {
            return languageList.size();
        }

        public class LanguageViewHolder extends RecyclerView.ViewHolder {
            private RelativeLayout mainLayout;
            private TextView txtLanguage;
            private RadioButton btnLanguage;

            public LanguageViewHolder(View itemView) {
                super(itemView);
                mainLayout = itemView.findViewById(R.id.mainLayout);
                txtLanguage = itemView.findViewById(R.id.txtLanguage);
                btnLanguage = itemView.findViewById(R.id.btnLanguage);
            }
        }
    }

}
