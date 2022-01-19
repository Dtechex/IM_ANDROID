package com.loopytime.im;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.model.ContactsData;
import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.READ_CONTACTS;
import static com.loopytime.utils.Constants.TAG_USER_ID;

public class BlockedContactsActivity extends BaseActivity implements View.OnClickListener {

    private final String TAG = this.getClass().getSimpleName();
    TextView title, nullText;
    ImageView backbtn, searchbtn, optionbtn, cancelbtn;
    RecyclerView recyclerView;
    LinearLayoutManager linearLayoutManager;
    BlockedContactsActivity.RecyclerViewAdapter recyclerViewAdapter;
    DatabaseHandler dbhelper;
    EditText searchView;
    RelativeLayout searchLay, mainLay;
    LinearLayout buttonLayout, nullLay;
    List<ContactsData.Result> contactList = new ArrayList<>();
    List<ContactsData.Result> filteredList = new ArrayList<>();
    String userId;
    ProgressDialog progressDialog;
    static ApiInterface apiInterface;
    SharedPreferences pref;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_contact);

        apiInterface = ApiClient.getClient().create(ApiInterface.class);
        pref = BlockedContactsActivity.this.getSharedPreferences("SavedPref", MODE_PRIVATE);
        editor = pref.edit();
        dbhelper = DatabaseHandler.getInstance(this);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.pleasewait));
        progressDialog.setCancelable(false);

        title = findViewById(R.id.title);
        backbtn = findViewById(R.id.backbtn);
        searchbtn = findViewById(R.id.searchbtn);
        optionbtn = findViewById(R.id.optionbtn);
        recyclerView = findViewById(R.id.recyclerView);
        searchView = findViewById(R.id.searchView);
        buttonLayout = findViewById(R.id.buttonLayout);
        cancelbtn = findViewById(R.id.cancelbtn);
        searchLay = findViewById(R.id.searchLay);
        mainLay = findViewById(R.id.mainLay);
        nullLay = findViewById(R.id.nullLay);
        nullText = findViewById(R.id.nullText);

        if(ApplicationClass.isRTL()){
            backbtn.setRotation(180);
        } else {
            backbtn.setRotation(0);
        }

        title.setVisibility(View.VISIBLE);
        backbtn.setVisibility(View.VISIBLE);
        searchbtn.setVisibility(View.GONE);
        optionbtn.setVisibility(View.GONE);


        title.setText(getString(R.string.blocked_contacts));
        backbtn.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.primarytext));
        nullText.setText(R.string.no_blocked_contacts);

        dbhelper = DatabaseHandler.getInstance(this);
        backbtn.setOnClickListener(this);
        searchbtn.setOnClickListener(this);
        optionbtn.setOnClickListener(this);
        cancelbtn.setOnClickListener(this);

        linearLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);

        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    cancelbtn.setVisibility(View.VISIBLE);
                } else {
                    cancelbtn.setVisibility(View.GONE);
                }
                recyclerViewAdapter.getFilter().filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @Override
    public void onNetworkChange(boolean isConnected) {

    }

    @Override
    protected void onResume() {
        contactList.clear();
        contactList.addAll(dbhelper.getBlockedContacts(this));
        filteredList.clear();
        filteredList.addAll(contactList);

        if (recyclerViewAdapter == null) {
            recyclerViewAdapter = new BlockedContactsActivity.RecyclerViewAdapter(this);
            recyclerView.setAdapter(recyclerViewAdapter);
            recyclerViewAdapter.notifyDataSetChanged();
        } else {
            recyclerViewAdapter.notifyDataSetChanged();
        }

        if (contactList.size() == 0) {
            nullLay.setVisibility(View.VISIBLE);
        } else {
            nullLay.setVisibility(View.GONE);
        }
        super.onResume();
    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter<BlockedContactsActivity.RecyclerViewAdapter.MyViewHolder> implements Filterable {

        List<ContactsData.Result> Items;
        Context context;
        private BlockedContactsActivity.RecyclerViewAdapter.SearchFilter mFilter;

        public RecyclerViewAdapter(Context context) {
            this.context = context;
            mFilter = new BlockedContactsActivity.RecyclerViewAdapter.SearchFilter(RecyclerViewAdapter.this);
        }

        @Override
        public Filter getFilter() {
            return mFilter;
        }

        public class SearchFilter extends Filter {
            private BlockedContactsActivity.RecyclerViewAdapter mAdapter;

            private SearchFilter(BlockedContactsActivity.RecyclerViewAdapter mAdapter) {
                super();
                this.mAdapter = mAdapter;
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                filteredList.clear();
                final FilterResults results = new FilterResults();
                if (constraint.length() == 0) {
                    filteredList.addAll(contactList);
                } else {
                    final String filterPattern = constraint.toString().toLowerCase().trim();
                    for (final ContactsData.Result result : contactList) {
                        if (result.user_name.toLowerCase().startsWith(filterPattern)) {
                            filteredList.add(result);
                        }
                    }
                }
                results.values = filteredList;
                results.count = filteredList.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                this.mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public BlockedContactsActivity.RecyclerViewAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_blocked_contacts, parent, false);

            return new BlockedContactsActivity.RecyclerViewAdapter.MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(final BlockedContactsActivity.RecyclerViewAdapter.MyViewHolder holder, int position) {

            if (ContextCompat.checkSelfPermission(BlockedContactsActivity.this, READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                holder.name.setText(filteredList.get(position).user_name);
            } else {
                holder.name.setText(filteredList.get(position).phone_no);
            }

            if (com.loopytime.helper.Utils.isProfileEnabled(dbhelper.getContactDetail(filteredList.get(position).user_id))) {
                Glide.with(context).load(Constants.USER_IMG_PATH + filteredList.get(position).user_image)
                        .apply(new RequestOptions().placeholder(R.drawable.temp).error(R.drawable.temp))
                        .into(holder.profileimage);
            } else {
                Glide.with(context).load(R.drawable.temp)
                        .apply(new RequestOptions().placeholder(R.drawable.temp).error(R.drawable.temp))
                        .into(holder.profileimage);
            }
        }

        @Override
        public int getItemCount() {
            return filteredList.size();
        }

        public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            LinearLayout parentlay;
            TextView name, btnUnBlock;
            ImageView profileimage;
            View profileview;

            public MyViewHolder(View view) {
                super(view);

                parentlay = view.findViewById(R.id.parentlay);
                profileimage = view.findViewById(R.id.profileimage);
                name = view.findViewById(R.id.txtName);
                btnUnBlock = view.findViewById(R.id.btnUnBlock);
                profileview = view.findViewById(R.id.profileview);

                btnUnBlock.setVisibility(View.VISIBLE);
                parentlay.setOnClickListener(this);
                profileimage.setOnClickListener(this);
                btnUnBlock.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.parentlay:
                        Intent i = new Intent(BlockedContactsActivity.this, ChatActivity.class);
                        i.putExtra(TAG_USER_ID, filteredList.get(getAdapterPosition()).user_id);
                        startActivity(i);
                        break;
                    case R.id.profileimage:
                        openUserDialog(profileview, filteredList.get(getAdapterPosition()));
                        break;
                    case R.id.btnUnBlock:
                        try {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                            jsonObject.put(Constants.TAG_RECEIVER_ID, filteredList.get(getAdapterPosition()).user_id);
                            jsonObject.put(Constants.TAG_TYPE, "unblock");
                            socketConnection.block(jsonObject);
                            dbhelper.updateBlockStatus(filteredList.get(getAdapterPosition()).user_id, Constants.TAG_BLOCKED_BYME, "unblock");
                            filteredList.remove(getAdapterPosition());
                            if (filteredList.size() == 0) {
                                nullLay.setVisibility(View.VISIBLE);
                            } else {
                                nullLay.setVisibility(View.GONE);
                            }

                            notifyDataSetChanged();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }
        }
    }

    private void openUserDialog(View view, ContactsData.Result data) {
        Intent i = new Intent(BlockedContactsActivity.this, DialogActivity.class);
        i.putExtra(TAG_USER_ID, data.user_id);
        i.putExtra(Constants.TAG_USER_NAME, data.user_name);
        i.putExtra(Constants.TAG_USER_IMAGE, data.user_image);
        //Pair<View, String> bodyPair = Pair.create(view, getURLForResource(R.drawable.temp));
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(BlockedContactsActivity.this, view, getURLForResource(R.drawable.temp));
        startActivity(i, options.toBundle());
    }

    public static String getURLForResource(int resourceId) {
        return Uri.parse("android.resource://com.hitasoft.loopytime.hiddy/" + resourceId).toString();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.backbtn:
                if (searchLay.getVisibility() == View.VISIBLE) {
                    searchView.setText("");
                    searchLay.setVisibility(View.GONE);
                    title.setVisibility(View.VISIBLE);
                    buttonLayout.setVisibility(View.VISIBLE);
                    ApplicationClass.hideSoftKeyboard(this, searchView);
                } else {
                    finish();
                }
                break;
            case R.id.searchbtn:
                title.setVisibility(View.GONE);
                searchLay.setVisibility(View.VISIBLE);
                buttonLayout.setVisibility(View.GONE);
                ApplicationClass.showKeyboard(this, searchView);
                break;
            case R.id.optionbtn:
                Display display = this.getWindowManager().getDefaultDisplay();
                ArrayList<String> values = new ArrayList<>();
                values.add(getString(R.string.refresh));

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                        R.layout.option_item, android.R.id.text1, values);
                LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View layout = layoutInflater.inflate(R.layout.option_layout, null);
                final PopupWindow popup = new PopupWindow(BlockedContactsActivity.this);
                popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                popup.setContentView(layout);
                popup.setWidth(display.getWidth() * 60 / 100);
                popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                popup.setFocusable(true);

                ImageView pinImage = layout.findViewById(R.id.pinImage);
                if(ApplicationClass.isRTL()){
                    layout.setAnimation(AnimationUtils.loadAnimation(this, R.anim.grow_from_topleft_to_bottomright));
                    pinImage.setRotation(180);
                    popup.showAtLocation(mainLay, Gravity.TOP | Gravity.START, ApplicationClass.dpToPx(this, 10), ApplicationClass.dpToPx(this, 63));
                } else {
                    layout.setAnimation(AnimationUtils.loadAnimation(this, R.anim.grow_from_topright_to_bottomleft));
                    pinImage.setRotation(0);
                    popup.showAtLocation(mainLay, Gravity.TOP | Gravity.END, ApplicationClass.dpToPx(this, 10), ApplicationClass.dpToPx(this, 63));
                }

                final ListView lv = layout.findViewById(R.id.listView);
                lv.setAdapter(adapter);
                popup.showAsDropDown(view);

                lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        popup.dismiss();
                        if (position == 0) {
//                            getContactList();
                            contactList.clear();
                            filteredList.clear();
                            contactList.addAll(dbhelper.getBlockedContacts(BlockedContactsActivity.this));
                            filteredList.addAll(contactList);
                            if (recyclerViewAdapter != null)
                                recyclerViewAdapter.notifyDataSetChanged();
                        }
                    }
                });
                break;
            case R.id.cancelbtn:
                searchView.setText("");
                break;
        }
    }

}
