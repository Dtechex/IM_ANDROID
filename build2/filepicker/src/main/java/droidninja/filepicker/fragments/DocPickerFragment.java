package droidninja.filepicker.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import droidninja.filepicker.PickerManager;
import droidninja.filepicker.R;
import droidninja.filepicker.adapters.SectionsPagerAdapter;
import droidninja.filepicker.cursors.loadercallbacks.FileMapResultCallback;
import droidninja.filepicker.models.Document;
import droidninja.filepicker.models.FileType;
import droidninja.filepicker.utils.MediaStoreHelper;
import droidninja.filepicker.utils.TabLayoutHelper;

public class DocPickerFragment extends BaseFragment {

    private static final String TAG = DocPickerFragment.class.getSimpleName();

    TabLayout tabLayout;
    boolean enableTab = true;
    ViewPager viewPager;
    private ProgressBar progressBar;
    private DocPickerFragmentListener mListener;

    public DocPickerFragment() {
        // Required empty public constructor
    }

    public static DocPickerFragment newInstance(boolean enableTab) {
        DocPickerFragment docPickerFragment = new DocPickerFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean("enableTab", enableTab);
        docPickerFragment.setArguments(bundle);
        return docPickerFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_doc_picker, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof DocPickerFragmentListener) {
            mListener = (DocPickerFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement DocPickerFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setViews(view);
        initView();
    }

    private void initView() {
        setUpViewPager();
        setData();
    }

    private void setViews(View view) {
        tabLayout = view.findViewById(R.id.tabs);
        viewPager = view.findViewById(R.id.viewPager);
        progressBar = view.findViewById(R.id.progress_bar);

        if (getArguments() != null) {
            enableTab = getArguments().getBoolean("enableTab");
        }

        if (enableTab) {
            tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
            tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        } else {
            tabLayout.setVisibility(View.GONE);
        }

    }

    private void setData() {
        MediaStoreHelper.getDocs(getActivity(),
                PickerManager.getInstance().getFileTypes(),
                PickerManager.getInstance().getSortingType().getComparator(),
                new FileMapResultCallback() {
                    @Override
                    public void onResultCallback(Map<FileType, List<Document>> files) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);
                        setDataOnFragments(files);
                    }
                }
        );
    }

    private void setDataOnFragments(Map<FileType, List<Document>> filesMap) {
        SectionsPagerAdapter sectionsPagerAdapter = (SectionsPagerAdapter) viewPager.getAdapter();
        if (sectionsPagerAdapter != null && filesMap!=null) {
            for (int index = 0; index < sectionsPagerAdapter.getCount(); index++) {
                DocFragment docFragment = (DocFragment) getChildFragmentManager()
                        .findFragmentByTag(
                                "android:switcher:" + R.id.viewPager + ":" + index);
                if (docFragment != null) {
                    FileType fileType = docFragment.getFileType();
                    if (fileType != null) {
                        List<Document> filesFilteredByType = filesMap.get(fileType);
                        if (filesFilteredByType != null)
                            docFragment.updateList(filesFilteredByType);
                    }
                }
            }
        }
    }

    private void setUpViewPager() {
        SectionsPagerAdapter adapter = new SectionsPagerAdapter(getChildFragmentManager());
        ArrayList<FileType> supportedTypes = PickerManager.getInstance().getFileTypes();
        for (int index = 0; index < supportedTypes.size(); index++) {
            adapter.addFragment(DocFragment.newInstance(supportedTypes.get(index)), supportedTypes.get(index).title);
        }

        viewPager.setOffscreenPageLimit(supportedTypes.size());
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);

        TabLayoutHelper mTabLayoutHelper = new TabLayoutHelper(tabLayout, viewPager);
        mTabLayoutHelper.setAutoAdjustTabModeEnabled(true);
    }

    public interface DocPickerFragmentListener {
    }
}
