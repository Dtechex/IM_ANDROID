package com.loopytime.im;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import com.loopytime.helper.MaterialColor;
import com.loopytime.helper.MaterialColors;
import com.loopytime.utils.GetSet;

public class WhatStatusFragmentFeedProfileActivity extends AppCompatActivity {
    public String uid, uname;

    private void setActionBarColor() {

        MaterialColor color = MaterialColors.CONVERSATION_PALETTE.get(getSharedPreferences("wall", Context.MODE_PRIVATE).getInt(MaterialColors.THEME, 0));
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(color.toActionBarColor(this)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(color.toActionBarColor(this));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_what_status_fragment_feed_profile);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setActionBarColor();
        uid = getIntent().getStringExtra("uid");
        uname = getIntent().getStringExtra("name");
        getSupportActionBar().setTitle((!uid.equalsIgnoreCase(GetSet.getUserId()) ? uname+"'s": "My") + " posts");

    }


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {


        return super.onOptionsItemSelected(item);
    }


}
