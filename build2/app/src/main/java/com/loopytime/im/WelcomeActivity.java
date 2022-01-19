package com.loopytime.im;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.loopytime.external.CirclePageIndicator;
import com.loopytime.helper.NetworkReceiver;

public class WelcomeActivity extends BaseActivity implements View.OnClickListener {

    static ViewPager desPager;
    TextView agree;
    CirclePageIndicator pagerIndicator;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        agree = findViewById(R.id.agree);
        pagerIndicator = findViewById(R.id.pagerIndicator);
        desPager = findViewById(R.id.desPager);
        agree.setOnClickListener(this);
        String[] names = {getString(R.string.welcome_des1), getString(R.string.welcome_des2), getString(R.string.welcome_des3)};

        DesPagerAdapter desPagerAdapter = new DesPagerAdapter(WelcomeActivity.this, names);

        desPager.setAdapter(desPagerAdapter);
        pagerIndicator.setViewPager(desPager);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.pleasewait));
        progressDialog.setCancelable(false);


    }

    @Override
    public void onNetworkChange(boolean isConnected) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (progressDialog != null) {
            progressDialog.dismiss();
        }

    }

    String[] list = {"India","Other"};
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.agree:
                if (NetworkReceiver.isConnected()) {
                    if (progressDialog != null && !progressDialog.isShowing()) {
                        progressDialog.show();
                    }
                    new AlertDialog.Builder(WelcomeActivity.this).setTitle("Choose Country")
                            .setItems(list, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startActivity(new Intent(WelcomeActivity.this, which==0?MobileNumberActivity.class:MobileNumberActivity2.class));
                                }
                            }).show();
                    //startActivity(new Intent(this, MobileNumberActivity.class));
                } else {
                    makeToast(getString(R.string.no_internet_connection));
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    class DesPagerAdapter extends PagerAdapter {

        Context context;
        LayoutInflater inflater;
        String[] names;

        public DesPagerAdapter(Context act, String[] names) {
            this.names = names;
            this.context = act;
            inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return names.length;
        }

        @Override
        public Object instantiateItem(ViewGroup collection, final int position) {
            ViewGroup itemView = (ViewGroup) inflater.inflate(R.layout.welcome_des_text,
                    collection, false);

            TextView name = itemView.findViewById(R.id.name);
            TextView title = itemView.findViewById(R.id.title);
            ImageView image = itemView.findViewById(R.id.image);
            if (position == 0) {
                image.setImageDrawable(getResources().getDrawable(R.drawable.introscreen_01));
                title.setText(getString(R.string.welcome_title1));
            } else if (position == 1) {
                image.setImageDrawable(getResources().getDrawable(R.drawable.introscreen_02));
                title.setText(getString(R.string.favourites));
            } else if (position == 2) {
                image.setImageDrawable(getResources().getDrawable(R.drawable.introscreen_03));
                title.setText(getString(R.string.welcome_title3));
            }
            name.setText(names[position]);

            collection.addView(itemView, 0);
            return itemView;
        }

        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            collection.removeView((ViewGroup) view);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Parcelable saveState() {
            return null;
        }
    }
}
