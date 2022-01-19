package com.loopytime.im;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class FeedDetailPagerAdapter extends FragmentStatePagerAdapter {
    private final List<Fragment> mFragmentList = new ArrayList<>();

    public FeedDetailPagerAdapter(FragmentManager manager) {
        super(manager);
        mFragmentList.clear();
    }

    @Override
    public int getItemPosition(Object object) {
        // refresh all fragments when data set changed
        return FeedDetailPagerAdapter.POSITION_NONE;
    }

    public void removePages() {
        mFragmentList.clear();
    }

    public void removeFrag(int pos) {
        mFragmentList.remove(pos);
        notifyDataSetChanged();
    }

    @Override
    public Fragment getItem(int position) {
        return mFragmentList.get(position);
    }

    @Override
    public int getCount() {
        return mFragmentList.size();
    }

    public void addFrag(Fragment fragment) {
        mFragmentList.add(fragment);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        Fragment frag = (Fragment) super.instantiateItem(container, position);
        mFragmentList.set(position, frag);
        return frag;
    }

}

