package com.loopytime.im;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.loopytime.model.HelpData;
import com.loopytime.utils.ApiClient;
import com.loopytime.utils.ApiInterface;
import com.loopytime.utils.Constants;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HelpActivity extends BaseActivity {

    private final String TAG = this.getClass().getSimpleName();
    Toolbar toolbar;
    ImageView btnBack;
    TextView txtTitle;
    RecyclerView recyclerView;
    HelpAdapter helpAdapter;
    List<HelpData.Term> helpList = new ArrayList<>();
    static ApiInterface apiInterface;
    static int VIEW_TYPE_TERM_HEADER = 1;
    static int VIEW_TYPE_HELP_HEADER = 2;
    static int VIEW_TYPE_CONTENT = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        apiInterface = ApiClient.getClient().create(ApiInterface.class);
        recyclerView = findViewById(R.id.recyclerView);
        toolbar = findViewById(R.id.actionbar);
        btnBack = toolbar.findViewById(R.id.backbtn);
        txtTitle = toolbar.findViewById(R.id.title);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        if(ApplicationClass.isRTL()){
            btnBack.setRotation(180);
        } else {
            btnBack.setRotation(0);
        }

        helpAdapter = new HelpAdapter(this, helpList);
        recyclerView.setAdapter(helpAdapter);
        helpAdapter.notifyDataSetChanged();
        initToolBar();
        getHelpLists();
    }

    @Override
    public void onNetworkChange(boolean isConnected) {

    }

    private void initToolBar() {
        txtTitle.setVisibility(View.VISIBLE);
        btnBack.setVisibility(View.VISIBLE);
        txtTitle.setText(R.string.help);
        btnBack.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.primarytext));
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void getHelpLists() {
        Call<HelpData> call3 = apiInterface.getHelpList();
        call3.enqueue(new Callback<HelpData>() {
            @Override
            public void onResponse(Call<HelpData> call, Response<HelpData> response) {
                HelpData helpData = response.body();
                if (helpData.status.equalsIgnoreCase(Constants.TRUE)) {
                    HelpData.Term term = new HelpData().new Term();
                    term.viewType = VIEW_TYPE_TERM_HEADER;/*Terms*/
                    helpList.add(term);

                    helpList.addAll(helpData.terms);

                    term = new HelpData().new Term();
                    term.viewType = VIEW_TYPE_HELP_HEADER;/*Help*/
                    helpList.add(term);
                    for (HelpData.Faq faq : helpData.faq) {
                        term = new HelpData().new Term();
                        term._id = faq._id;
                        term.title = faq.title;
                        term.description = faq.description;
                        term.type = faq.type;
                        term.viewType = VIEW_TYPE_CONTENT;
                        helpList.add(term);
                    }

                    if (helpAdapter == null) {
                        helpAdapter = new HelpAdapter(HelpActivity.this, helpList);
                        recyclerView.setAdapter(helpAdapter);
                        helpAdapter.notifyDataSetChanged();
                    } else {
                        helpAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onFailure(Call<HelpData> call, Throwable t) {
                call.cancel();
                Log.e(TAG, "getHelpList: " + t.getMessage());
            }
        });
    }

    public class HelpAdapter extends RecyclerView.Adapter {

        Context context;
        List<HelpData.Term> helpList;

        public HelpAdapter(Context context, List<HelpData.Term> helpList) {
            this.context = context;
            this.helpList = helpList;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == VIEW_TYPE_TERM_HEADER || viewType == VIEW_TYPE_HELP_HEADER) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_help_header, parent, false);
                return new HeaderViewHolder(view);
            } else {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_help, parent, false);
                return new MyViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            final HelpData.Term term = helpList.get(position);
            if (getItemViewType(position) == VIEW_TYPE_TERM_HEADER) {
                ((HeaderViewHolder) holder).txtHeader.setText(R.string.terms);
            } else if (getItemViewType(position) == VIEW_TYPE_HELP_HEADER) {
                ((HeaderViewHolder) holder).txtHeader.setText(R.string.help);
            } else {
                ((MyViewHolder) holder).txtTitle.setText(term.title);

                ((MyViewHolder) holder).txtTitle.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent web = new Intent(context, HelpViewActivity.class);
                        web.putExtra("HELP", term);
                        startActivity(web);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return helpList.size();
        }

        @Override
        public int getItemViewType(int position) {
            if (helpList.get(position).viewType == VIEW_TYPE_TERM_HEADER) {
                return VIEW_TYPE_TERM_HEADER;
            } else if (helpList.get(position).viewType == VIEW_TYPE_HELP_HEADER) {
                return VIEW_TYPE_HELP_HEADER;
            } else {
                return VIEW_TYPE_CONTENT;
            }
        }
    }

    private class MyViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle;

        public MyViewHolder(View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtTitle);
        }
    }

    public class HeaderViewHolder extends RecyclerView.ViewHolder {

        TextView txtHeader;

        public HeaderViewHolder(View view) {
            super(view);
            txtHeader = view.findViewById(R.id.txtHeader);
        }
    }
}
