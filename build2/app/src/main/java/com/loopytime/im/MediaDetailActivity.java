package com.loopytime.im;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.tabs.TabLayout;
import com.loopytime.helper.StorageManager;
import com.loopytime.model.ChannelResult;
import com.loopytime.model.ContactsData;
import com.loopytime.model.GroupData;
import com.loopytime.utils.Constants;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MediaDetailActivity extends BaseActivity implements View.OnClickListener{
    String getId = "",from = "";
    ImageView backbtn, imageView;
    TabLayout tabs;
    ViewPager viewpager;
    ViewPagerAdapter adapter;
    CircleImageView userImg;
    TextView userName;
    RelativeLayout imageViewLay;
    StorageManager storageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_detail);

        backbtn = findViewById(R.id.backbtn);
        userImg = findViewById(R.id.userImg);
        viewpager = findViewById(R.id.viewpager);
        userName = findViewById(R.id.userName);
        tabs = findViewById(R.id.tabs);
        imageViewLay = findViewById(R.id.imageViewLay);
        imageView = findViewById(R.id.imageView);

        storageManager = StorageManager.getInstance(this);

        if(getIntent().getExtras().getString(Constants.TAG_USER_ID)!=null){
            getId = getIntent().getExtras().getString(Constants.TAG_USER_ID);
            from = getIntent().getExtras().getString(Constants.TAG_FROM);
        }

        if(ApplicationClass.isRTL()){
            backbtn.setRotation(180);
        } else {
            backbtn.setRotation(0);
        }


        if(from.equals(Constants.TAG_SINGLE)){
            ContactsData.Result results;
            results = dbhelper.getContactDetail(getId);
            userName.setText(ApplicationClass.getContactName(this, results.phone_no,results.country_code,results.user_name));
            if (!results.blockedme.equals("block")) {
                DialogActivity.setProfileImage(dbhelper.getContactDetail(getId), userImg, this);
            } else {
                Glide.with(getApplicationContext()).load(R.drawable.temp)
                        .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp))
                        .into(userImg);
            }
        } else if(from.equals(Constants.TAG_GROUP)){
            GroupData groupData = dbhelper.getGroupData(this, getId);
            userName.setText(groupData.groupName);
            Glide.with(getApplicationContext()).load(Constants.GROUP_IMG_PATH + groupData.groupImage)
                    .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.create_group).error(R.drawable.create_group))
                    .into(userImg);
        } else {
            ChannelResult.Result channelData = dbhelper.getChannelInfo(getId);
            userName.setText(channelData.channelName);
            Glide.with(getApplicationContext()).load(Constants.CHANNEL_IMG_PATH + channelData.channelImage)
                    .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp))
                    .into(userImg);
        }

        setupViewPager(viewpager);

        backbtn.setOnClickListener(this);
    }

    @Override
    public void onNetworkChange(boolean isConnected) {

    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new ViewPagerAdapter(getSupportFragmentManager(), MediaDetailActivity.this);
        adapter.addFragment(ImageFragment.newInstance(getId,from), getString(R.string.media));
        adapter.addFragment(DocumentFragment.newInstance(getId,from), getString(R.string.documents));
       // adapter.addFragment(AudioFragment.newInstance(getId,from), getString(R.string.audio));

        viewPager.setAdapter(adapter);
        tabs.setupWithViewPager(viewPager);
        // Iterate over all tabs and set the custom view
        for (int i = 0; i < tabs.getTabCount(); i++) {
            TabLayout.Tab tab = tabs.getTabAt(i);
            tab.setCustomView(adapter.getTabView(i, this));
        }
        adapter.setOnSelectView(this, tabs, 0);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.backbtn:
                finish();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        finish();
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();
        Context context;

        public ViewPagerAdapter(FragmentManager manager, Context context) {
            super(manager);
            this.context = context;
        }

        public View getTabView(int position, Context context) {
            // Given you have a custom layout in `res/layout/custom_tab.xml` with a TextView and ImageView
            View v = LayoutInflater.from(context).inflate(R.layout.tab_layout, null);
            TextView tabName = (TextView) v.findViewById(R.id.tabName);
            tabName.setText(mFragmentTitleList.get(position));
            // ImageView indication = (ImageView) v.findViewById(R.id.indication);
            return v;
        }

        public void setOnSelectView(Context mContext, TabLayout tabLayout, int position) {
            TabLayout.Tab tab = tabLayout.getTabAt(position);
            View selected = tab.getCustomView();
            TextView tabName = selected.findViewById(R.id.tabName);
            tabName.setTextColor(mContext.getResources().getColor(R.color.primarytext));
        }

        public void setUnSelectView(Context mContext, TabLayout tabLayout, int position) {
            TabLayout.Tab tab = tabLayout.getTabAt(position);
            View selected = tab.getCustomView();
            TextView iv_text = selected.findViewById(R.id.tabName);
            iv_text.setTextColor(mContext.getResources().getColor(R.color.secondarytext));
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }
}
