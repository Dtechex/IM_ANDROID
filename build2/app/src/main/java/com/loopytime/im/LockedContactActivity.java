package com.loopytime.im;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.snackbar.Snackbar;
import com.loopytime.helper.DatabaseHandler;
import com.loopytime.helper.MaterialColor;
import com.loopytime.helper.MaterialColors;
import com.loopytime.helper.NetworkUtil;
import com.loopytime.model.ContactsData;
import com.loopytime.model.GroupData;
import com.loopytime.utils.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.Manifest.permission.READ_CONTACTS;

public class LockedContactActivity extends AppCompatActivity {
    MaterialColor color;
    List<String> userList = new ArrayList<>();
RecyclerViewAdapter recyclerViewAdapter;
    void setColor() {
        color = MaterialColors.CONVERSATION_PALETTE.get(this.getSharedPreferences("wall", Context.MODE_PRIVATE).getInt(MaterialColors.THEME, 0));
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(color.toActionBarColor(this)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(color.toStatusBarColor(this));
        }
        tintShape(this,color.toActionBarColor(this), ContextCompat.getDrawable(this,R.drawable.fablay_bg));
    }
    void tintShape(Context mContext, int color, Drawable background){

        if (background instanceof ShapeDrawable) {
            ((ShapeDrawable)background).getPaint().setColor(color);
        } else if (background instanceof GradientDrawable) {
            ((GradientDrawable)background).setColor(color);
        } else if (background instanceof ColorDrawable) {
            ((ColorDrawable)background).setColor(color);
        }
    }

    RecyclerView contactRecycler;
    List<ContactsData.Result> contactList = new ArrayList<>();
    List<ContactsData.Result> filteredList = new ArrayList<>();
    DatabaseHandler dbhelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locked_contact);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Select chats to lock");
        setColor();
        contactRecycler = findViewById(R.id.contactRecycler);
        dbhelper = DatabaseHandler.getInstance(this);
        initContactList();
        findViewById(R.id.btnNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               onBackPressed();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void initContactList() {
        contactRecycler.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        contactRecycler.setHasFixedSize(true);

        contactList.addAll(dbhelper.getStoredContacts(this));
        Collections.sort(contactList, new Comparator<ContactsData.Result>() {
            public int compare(ContactsData.Result obj1, ContactsData.Result obj2) {
                // ## Ascending order
                return obj1.user_name.compareToIgnoreCase(obj2.user_name); // To compare string values
            }
        });
        filteredList = new ArrayList<>();

            filteredList.addAll(contactList);
            for(ContactsData.Result res: filteredList){
                if(getSharedPreferences("wall", Context.MODE_PRIVATE).contains(res.user_id)){
                    userList.add(res.user_id);
                }
            }


        getSupportActionBar().setSubtitle(" " + userList.size() + " " + getString(R.string.of) + " " +
                filteredList.size() + " " + getString(R.string.selected));

        if (recyclerViewAdapter == null) {
            recyclerViewAdapter = new RecyclerViewAdapter(this);
            contactRecycler.setAdapter(recyclerViewAdapter);
            recyclerViewAdapter.notifyDataSetChanged();
        } else {
            recyclerViewAdapter.notifyDataSetChanged();
        }

        //nullText.setText(getString(R.string.no_contact));
        if (filteredList.size() == 0) {
          //  nullLay.setVisibility(View.VISIBLE);
        } else {
            //nullLay.setVisibility(View.GONE);
        }
    }

    private String isNetworkConnected() {
        return NetworkUtil.getConnectivityStatusString(this);
    }

    private void networkSnack() {
        Snackbar snackbar = Snackbar
                .make(findViewById(R.id.main_layout), getString(R.string.network_failure), Snackbar.LENGTH_SHORT);
        View sbView = snackbar.getView();
        TextView textView = sbView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        snackbar.show();
    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> implements Filterable {

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
                if (filteredList.size() == 0) {
                   // nullLay.setVisibility(View.VISIBLE);
                } else {
                 //   nullLay.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public RecyclerViewAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_blocked_contacts, parent, false);

            return new RecyclerViewAdapter.MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(final RecyclerViewAdapter.MyViewHolder holder, int position) {

            if (ContextCompat.checkSelfPermission(LockedContactActivity.this, READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                holder.name.setText(filteredList.get(position).user_name);
            } else {
                holder.name.setText(filteredList.get(position).phone_no);
            }

            if (filteredList.get(position).blockedme.equals("block")) {
                Glide.with(context).load(R.drawable.temp)
                        .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.temp).error(R.drawable.temp).override(ApplicationClass.dpToPx(context, 70)))
                        .into(holder.profileimage);
            } else {
                DialogActivity.setProfileImage(dbhelper.getContactDetail(filteredList.get(position).user_id), holder.profileimage, context);
            }

            if (userList.contains(filteredList.get(position).user_id)) {
                holder.btnSelect.setChecked(true);
            } else {
                holder.btnSelect.setChecked(false);
            }
        }

        @Override
        public int getItemCount() {
            return filteredList.size();
        }

        public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            LinearLayout parentlay;
            TextView name;
            ImageView profileimage;
            View profileview;
            AppCompatRadioButton btnSelect;

            public MyViewHolder(View view) {
                super(view);

                parentlay = view.findViewById(R.id.parentlay);
                profileimage = view.findViewById(R.id.profileimage);
                name = view.findViewById(R.id.txtName);
                profileview = view.findViewById(R.id.profileview);
                btnSelect = view.findViewById(R.id.btnSelect);

                btnSelect.setVisibility(View.VISIBLE);
                parentlay.setOnClickListener(this);
                btnSelect.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.btnSelect:
                    case R.id.parentlay:
                        if (!filteredList.get(getAdapterPosition()).blockedbyme.equals("block") &&
                                !filteredList.get(getAdapterPosition()).blockedme.equals("block")) {
                            if (!userList.contains(filteredList.get(getAdapterPosition()).user_id)) {
                                userList.add(filteredList.get(getAdapterPosition()).user_id);
                                btnSelect.setChecked(true);
                                getSharedPreferences("wall", Context.MODE_PRIVATE).edit().putBoolean(filteredList.get(getAdapterPosition()).user_id, true).commit();
                            } else {
                                btnSelect.setChecked(false);
                                getSharedPreferences("wall", Context.MODE_PRIVATE).edit().remove(filteredList.get(getAdapterPosition()).user_id).commit();
                                userList.remove(filteredList.get(getAdapterPosition()).user_id);
                      /*          groupList.remove(filteredList.get(getAdapterPosition()));
                                isSelectAll = false;
                                btnSelectAll.setChecked(false);*/
                            }

                            if (filteredList.size() == userList.size()) {
                                //btnSelectAll.setChecked(true);
                            } else {
                                //btnSelectAll.setChecked(false);
                            }
                            notifyDataSetChanged();
                            /*if (groupAdapter != null) {
                                groupAdapter.notifyDataSetChanged();
                            }*/
                            setTxtSubtitle(userList.size());
                        } else {
                            btnSelect.setChecked(false);
                            openAlertDialog(filteredList.get(getAdapterPosition()));
                        }
                        break;
                    /*case R.id.btnSelect:
                        if (!userList.contains(filteredList.get(getAdapterPosition()).user_id)) {
                            userList.add(filteredList.get(getAdapterPosition()).user_id);
                            groupList.add(filteredList.get(getAdapterPosition()));
                            btnSelect.setChecked(true);
                        } else {
                            btnSelect.setChecked(false);
                            userList.remove(filteredList.get(getAdapterPosition()).user_id);
                            groupList.remove(filteredList.get(getAdapterPosition()));
                            isSelectAll = false;
                            btnSelectAll.setChecked(false);
                            selectAllContact();
                        }
                        notifyDataSetChanged();

                        if (groupAdapter != null) {
                            groupAdapter.notifyDataSetChanged();
                        }
                        setTxtSubtitle(userList.size());
                        break;
*/
                }
            }
        }
    }
    public void setTxtSubtitle(int count) {
        getSupportActionBar().setSubtitle(" " + count + " " + getString(R.string.of) + " " +
                filteredList.size() + " " + getString(R.string.selected));
    }


    private void openAlertDialog(ContactsData.Result result) {
        final Dialog dialog = new Dialog(LockedContactActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setContentView(R.layout.default_popup);
        dialog.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels * 90 / 100, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        TextView title = dialog.findViewById(R.id.title);
        TextView yes = dialog.findViewById(R.id.yes);
        TextView no = dialog.findViewById(R.id.no);
        yes.setText(R.string.okay);
        no.setText(getString(R.string.nope));
        title.setText(getString(R.string.could_not_add) + " " + ApplicationClass.getContactName(getApplicationContext(), result.phone_no, result.country_code,result.user_name));
        no.setVisibility(View.GONE);

        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

}

