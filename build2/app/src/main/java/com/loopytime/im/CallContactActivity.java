package com.loopytime.im;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
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

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.loopytime.apprtc.util.AppRTCUtils;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.NetworkReceiver;
import com.loopytime.helper.SocketConnection;
import com.loopytime.model.ContactsData;
import com.loopytime.model.SaveMyContacts;
import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;
import com.loopytime.utils.Constants;
import com.loopytime.utils.GetSet;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.RECORD_AUDIO;

public class CallContactActivity extends BaseActivity implements SocketConnection.SelectContactListener, View.OnClickListener {

    private final String TAG = this.getClass().getSimpleName();
    TextView title, nullText;
    ImageView backbtn, searchbtn, optionbtn, cancelbtn;
    RecyclerView recyclerView;
    DatabaseHandler dbhelper;
    EditText searchView;
    RelativeLayout searchLay, mainLay;
    LinearLayout nullLay;
    LinearLayout buttonLayout;
    List<ContactsData.Result> contactList = new ArrayList<>();
    List<ContactsData.Result> filteredList = new ArrayList<>();
    static ApiInterface apiInterface;
    LinearLayoutManager linearLayoutManager;
    RecyclerViewAdapter recyclerViewAdapter;
    ProgressDialog progressDialog;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    SocketConnection socketConnection;
    List<ContactsData.Result> userData = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_contact);
        pref = CallContactActivity.this.getSharedPreferences("SavedPref", MODE_PRIVATE);
        editor = pref.edit();
        apiInterface = ApiClient.getClient().create(ApiInterface.class);
        dbhelper = DatabaseHandler.getInstance(this);
        socketConnection = SocketConnection.getInstance(this);
        SocketConnection.getInstance(this).setSelectContactListener(this);

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
        searchbtn.setVisibility(View.VISIBLE);
        optionbtn.setVisibility(View.VISIBLE);

        title.setText(getString(R.string.select_contact));
        backbtn.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.primarytext));

        linearLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);

        contactList.addAll(dbhelper.getStoredContacts(this));
        filteredList = new ArrayList<>();
        filteredList.addAll(contactList);

        recyclerViewAdapter = new RecyclerViewAdapter(this);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerViewAdapter.notifyDataSetChanged();

        backbtn.setOnClickListener(this);
        searchbtn.setOnClickListener(this);
        optionbtn.setOnClickListener(this);
        cancelbtn.setOnClickListener(this);

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

        nullText.setText(getString(R.string.no_contact));
        if (contactList.size() == 0) {
            nullLay.setVisibility(View.VISIBLE);
        } else {
            nullLay.setVisibility(View.GONE);
        }

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
                final PopupWindow popup = new PopupWindow(CallContactActivity.this);
                popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                popup.setContentView(layout);
                popup.setWidth(display.getWidth() * 60 / 100);
                popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                popup.setFocusable(true);

                ImageView pinImage = layout.findViewById(R.id.pinImage);
                if(ApplicationClass.isRTL()){
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
                            if (progressDialog != null) progressDialog.show();
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    new GetContactTask().execute();
                                }
                            }, 1000);
                        }
                    }
                });
                break;
            case R.id.cancelbtn:
                searchView.setText("");
                break;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class GetContactTask extends AsyncTask<Void, Integer, Void> {
        JsonArray contactsNum = new JsonArray();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Uri uri = null;
            uri = ContactsContract.CommonDataKinds.Contactables.CONTENT_URI;
            ContentResolver cr = getContentResolver();
            Cursor cur = cr.query(uri, Constants.PROJECTION, Constants.SELECTION, Constants.SELECTION_ARGS, null);

            if (cur != null) {
                try {
                    final int nameIndex = cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                    final int numberIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                    while (cur.moveToNext()) {
                        ContactsData.Result user = new ContactsData().new Result();
                        String phoneNo = cur.getString(numberIndex).replace(" ", "");
                        String name = cur.getString(nameIndex);
                        user.saved_name = name;
                        try {
                            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                            Phonenumber.PhoneNumber numberProto = phoneUtil.parse(phoneNo, Locale.getDefault().getCountry());
                            if (phoneNo != null && !phoneNo.equals("") && phoneNo.length() > 6 && phoneUtil.isPossibleNumberForType(numberProto, PhoneNumberUtil.PhoneNumberType.MOBILE)) {
                                String tempNo = ("" + numberProto.getNationalNumber()).replaceAll("[^0-9]", "");
                                if (tempNo.startsWith("0")) {
                                    tempNo = tempNo.replaceFirst("^0+(?!$)", "");
                                }
                                contactsNum.add(tempNo.replaceAll("[^0-9]", ""));
                                user.phone_no = tempNo.replaceAll("[^0-9]", "");
                                Log.v("Name", "name=" + name + " num=" + tempNo.replaceAll("[^0-9]", ""));
                            }
                        } catch (NumberParseException e) {
                            phoneNo.replaceAll("[^0-9]", "");
                            if (isValidPhoneNumber(phoneNo)) {
                                if (phoneNo.startsWith("0")) {
                                    phoneNo = phoneNo.replaceFirst("^0+(?!$)", "");
                                }
                                Log.v("Name", "excep name=" + name + " num=" + phoneNo.replaceAll("[^0-9]", ""));
                                contactsNum.add(phoneNo.replaceAll("[^0-9]", ""));
                                user.phone_no = phoneNo.replaceAll("[^0-9]", "");
                            }
                        }

                        userData.add(user);
                    }

                } finally {
                    cur.close();
                }
            }
            Log.e(TAG, "getContactList: " + contactsNum.size());
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            saveMyContacts(contactsNum);
        }
    }


    public void saveMyContacts(JsonArray contacts) {
        HashMap<String, String> map = new HashMap<>();
        map.put(Constants.TAG_USER_ID, GetSet.getUserId());
        map.put(Constants.TAG_CONTACTS,  contacts.toString().replaceAll(", 0",", "));
        Call<SaveMyContacts> call = apiInterface.saveMyContacts(GetSet.getToken(), map);
        call.enqueue(new Callback<SaveMyContacts>() {
            @Override
            public void onResponse(Call<SaveMyContacts> call, Response<SaveMyContacts> response) {
                Log.v(TAG, "saveMyContacts=" + response.isSuccessful());
                System.out.println("sxsh tes "+response.toString());
                updatemycontacts(contacts);
            }

            @Override
            public void onFailure(Call<SaveMyContacts> call, Throwable t) {
                System.out.println("sxsh err "+t.getLocalizedMessage());
                Log.e(TAG, "saveMyContacts: " + t.getMessage());
                if (progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();
                call.cancel();
            }
        });
    }

    void updatemycontacts(JsonArray contacts) {
        HashMap<String, String> map = new HashMap<>();
        map.put(Constants.TAG_USER_ID, GetSet.getUserId());
        map.put(Constants.TAG_CONTACTS, contacts.toString().replaceAll(", 0",", "));
        map.put(Constants.TAG_PHONE_NUMBER, GetSet.getphonenumber());
        Log.v(TAG, "updateMyContacts=" + map.toString());
        Call<ContactsData> call3 = apiInterface.updatemycontacts(GetSet.getToken(), map);
        call3.enqueue(new Callback<ContactsData>() {
            @Override
            public void onResponse(Call<ContactsData> call, Response<ContactsData> response) {
                try {
                    Log.v(TAG, "updateMyContacts=" + new Gson().toJson(response.body()));
                    ContactsData data = response.body();
                    if (data.status.equals("true")) {
                        new UpdateContactTask(data).execute();
                    } else if (data.status.equals("false")) {
                        if (progressDialog.isShowing())
                            progressDialog.dismiss();

                    }
                } catch (Exception e) {

                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ContactsData> call, Throwable t) {
                Log.e(TAG, "updateMyContacts: " + t.getMessage());
                call.cancel();
                if (progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();
            }
        });

    }

    @SuppressLint("StaticFieldLeak")
    private class UpdateContactTask extends AsyncTask<Void, Integer, Void> {
        ContactsData data = new ContactsData();

        public UpdateContactTask(ContactsData data) {
            this.data = data;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            for (ContactsData.Result result : data.result) {
                String name = result.user_name;
                HashMap<String, String> map = ApplicationClass.getContactrNot(getApplicationContext(), result.phone_no);
                if (map.get("isAlready").equals("true")) {
                    name = map.get(Constants.TAG_USER_NAME);
                }
                dbhelper.addContactDetails(name,result.user_id, result.user_name, result.phone_no, result.country_code, result.user_image, result.privacy_about,
                        result.privacy_last_seen, result.privacy_profile_image, result.about, result.contactstatus);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            filteredList.clear();
            contactList.clear();
            contactList.addAll(dbhelper.getStoredContacts(CallContactActivity.this));
            filteredList.addAll(contactList);
            if (recyclerViewAdapter != null)
                recyclerViewAdapter.notifyDataSetChanged();

            nullText.setText(getString(R.string.no_contact));
            if (contactList.size() == 0) {
                nullLay.setVisibility(View.VISIBLE);
            } else {
                nullLay.setVisibility(View.GONE);
            }
            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();
        }
    }

    public boolean isValidPhoneNumber(CharSequence target) {
        if (target.length() < 7 || target.length() > 15) {
            return false;
        } else {
            return android.util.Patterns.PHONE.matcher(target).matches();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (recyclerViewAdapter != null) {
            recyclerViewAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SocketConnection.getInstance(this).setSelectContactListener(null);
    }

    @Override
    public void onNetworkChange(boolean isConnected) {

    }

    @Override
    public void onUserImageChange(String user_id, String user_image) {
        Log.v("Chat", "onUserImageChange");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (recyclerViewAdapter != null && filteredList.size() > 0) {
                    for (int i = 0; i < filteredList.size(); i++) {
                        if (user_id.equals(filteredList.get(i).user_id)) {
                            filteredList.get(i).user_image = user_image;
                            recyclerViewAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onBlockStatus(JSONObject data) {
        if (recyclerViewAdapter != null && filteredList.size() > 0) {
            try {
                String sender_id = data.getString(Constants.TAG_SENDER_ID);
                String type = data.getString(Constants.TAG_TYPE);
                for (int i = 0; i < filteredList.size(); i++) {
                    if (sender_id.equals(filteredList.get(i).user_id)) {
                        filteredList.get(i).blockedme = type;
                        recyclerViewAdapter.notifyItemChanged(i);
                        break;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onPrivacyChanged(JSONObject jsonObject) {
//        Log.i(TAG, "onPrivacyChanged: " + jsonObject);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (recyclerViewAdapter != null) {
                    contactList.addAll(dbhelper.getStoredContacts(getApplicationContext()));
                    filteredList.clear();
                    filteredList.addAll(contactList);
                    recyclerViewAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter implements Filterable {

        List<ContactsData.Result> Items;
        Context context;
        private RecyclerViewAdapter.SearchFilter mFilter;

        public RecyclerViewAdapter(Context context) {
            this.context = context;
            mFilter = new RecyclerViewAdapter.SearchFilter(RecyclerViewAdapter.this);
        }

        @Override
        public Filter getFilter() {
            return mFilter;
        }

        public class SearchFilter extends Filter {
            private RecyclerViewAdapter mAdapter;

            private SearchFilter(RecyclerViewAdapter mAdapter) {
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
                        if (result.user_name != null) {
                            if (result.user_name.toLowerCase().startsWith(filterPattern)) {
                                filteredList.add(result);
                            }
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
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = null;

            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.contact_list_item, parent, false);
            return new RecyclerViewAdapter.MyViewHolder(itemView);

        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (ContextCompat.checkSelfPermission(CallContactActivity.this, READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                ((MyViewHolder) holder).name.setText(filteredList.get(position).user_name);
            } else {
                ((MyViewHolder) holder).name.setText(filteredList.get(position).phone_no);
            }

            ((MyViewHolder) holder).about.setText(filteredList.get(position).about != null ? filteredList.get(position).about : "");
            if (filteredList.get(position).blockedme.equals("block")) {
                Glide.with(context).load(R.drawable.temp)
                        .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(context, 70)))
                        .into(((MyViewHolder) holder).profileimage);
            } else {
                DialogActivity.setProfileImage(dbhelper.getContactDetail(filteredList.get(position).user_id), ((MyViewHolder) holder).profileimage, context);
            }

        }

        @Override
        public int getItemCount() {
            return filteredList.size();
        }

        public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            LinearLayout parentlay;
            TextView name, about;
            CircleImageView profileimage;
            ImageView btnVoiceCall, btnVideoCall;
            View profileview;
            LinearLayout callLayout;

            public MyViewHolder(View view) {
                super(view);

                parentlay = view.findViewById(R.id.parentlay);
                profileimage = view.findViewById(R.id.profileimage);
                name = view.findViewById(R.id.name);
                about = view.findViewById(R.id.about);
                profileview = view.findViewById(R.id.profileview);
                callLayout = view.findViewById(R.id.callLayout);
                btnVoiceCall = view.findViewById(R.id.btnVoiceCall);
                btnVideoCall = view.findViewById(R.id.btnVideoCall);

                callLayout.setVisibility(View.VISIBLE);
                profileimage.setOnClickListener(this);
                btnVoiceCall.setOnClickListener(this);
                btnVideoCall.setOnClickListener(this);

            }

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.profileimage:
                        openUserDialog(profileview, filteredList.get(getAdapterPosition()));
                        break;
                    case R.id.btnVoiceCall:
                        ContactsData.Result result = dbhelper.getContactDetail(filteredList.get(getAdapterPosition()).user_id);
                        if (ContextCompat.checkSelfPermission(CallContactActivity.this, CAMERA) != PackageManager.PERMISSION_GRANTED
                                || ContextCompat.checkSelfPermission(CallContactActivity.this, RECORD_AUDIO)
                                != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(CallContactActivity.this, new String[]{CAMERA, RECORD_AUDIO}, 100);
                        } else if (result.blockedbyme.equals("block")) {
                            blockChatConfirmDialog(result.user_id);
                        } else {
                            if (NetworkReceiver.isConnected()) {
                                ApplicationClass.preventMultiClick(btnVoiceCall);
                                AppRTCUtils appRTCUtils = new AppRTCUtils(getApplicationContext());
                                Intent video = appRTCUtils.connectToRoom(result.user_id, Constants.TAG_SEND, Constants.TAG_AUDIO);
                                startActivity(video);
                            } else {
                                makeToast(getString(R.string.no_internet_connection));
                            }
                        }
                        break;
                    case R.id.btnVideoCall:
                        result = dbhelper.getContactDetail(filteredList.get(getAdapterPosition()).user_id);
                        if (ContextCompat.checkSelfPermission(CallContactActivity.this, CAMERA) != PackageManager.PERMISSION_GRANTED
                                || ContextCompat.checkSelfPermission(CallContactActivity.this, RECORD_AUDIO)
                                != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(CallContactActivity.this, new String[]{CAMERA, RECORD_AUDIO}, 101);
                        } else if (result.blockedbyme.equals("block")) {
                            blockChatConfirmDialog(result.user_id);
                        } else {
                            if (NetworkReceiver.isConnected()) {
                                ApplicationClass.preventMultiClick(btnVideoCall);
                                AppRTCUtils appRTCUtils = new AppRTCUtils(getApplicationContext());
                                Intent video = appRTCUtils.connectToRoom(result.user_id, Constants.TAG_SEND, Constants.TAG_VIDEO);
                                startActivity(video);
                            } else {
                                makeToast(getString(R.string.no_internet_connection));
                            }
                        }
                        break;
                }
            }
        }
    }


    private void openUserDialog(View view, ContactsData.Result data) {
        Intent i = new Intent(CallContactActivity.this, DialogActivity.class);
        i.putExtra(Constants.TAG_USER_ID, data.user_id);
        i.putExtra(Constants.TAG_USER_NAME, data.user_name);
        i.putExtra(Constants.TAG_USER_IMAGE, data.user_image);
        i.putExtra(Constants.TAG_BLOCKED_ME, data.blockedme);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(CallContactActivity.this, view, getURLForResource(R.drawable.temp));
        startActivity(i, options.toBundle());
    }

    public static String getURLForResource(int resourceId) {
        return Uri.parse("android.resource://com.hitasoft.loopytime.hiddy/" + resourceId).toString();
    }

    private void blockChatConfirmDialog(String userId) {
        final Dialog dialog = new Dialog(CallContactActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.default_popup);
        dialog.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels * 90 / 100, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        TextView title = dialog.findViewById(R.id.title);
        TextView yes = dialog.findViewById(R.id.yes);
        TextView no = dialog.findViewById(R.id.no);

        yes.setText(getString(R.string.unblock));
        no.setText(getString(R.string.cancel));
        title.setText(R.string.unblock_message);

        no.setVisibility(View.VISIBLE);

        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(Constants.TAG_SENDER_ID, GetSet.getUserId());
                    jsonObject.put(Constants.TAG_RECEIVER_ID, userId);
                    jsonObject.put(Constants.TAG_TYPE, "unblock");
                    Log.v(TAG, "block=" + jsonObject);
                    socketConnection.block(jsonObject);
                    dbhelper.updateBlockStatus(userId, Constants.TAG_BLOCKED_BYME, "unblock");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}
