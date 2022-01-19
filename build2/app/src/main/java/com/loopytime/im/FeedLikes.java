package com.loopytime.im;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.loopytime.helper.ExecuteServices;
import com.loopytime.helper.MaterialColor;
import com.loopytime.helper.MaterialColors;
import com.loopytime.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FeedLikes extends AppCompatActivity {
    RecyclerView list;
    int[] drr = {R.drawable.ic_like, R.drawable.ic_haha, R.drawable.ic_love, R.drawable.ic_sad, R.drawable.ic_wow, R.drawable.ic_angry, R.drawable.ic_ya};

    MaterialColor color;

    void setColor() {
        color = MaterialColors.CONVERSATION_PALETTE.get(this.getSharedPreferences("wall", Context.MODE_PRIVATE).getInt(MaterialColors.THEME, 0));
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(color.toActionBarColor(this)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(color.toStatusBarColor(this));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_likes);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Reactions");
        setColor();
        list = findViewById(R.id.list);
        list.setHasFixedSize(true);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new RecyclerView.Adapter<ViewHolder>() {
            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new ViewHolder(LayoutInflater.from(FeedLikes.this).inflate(R.layout.bottom_status_item, parent, false));
            }

            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                holder.react.setVisibility(View.VISIBLE);

                    try {
                        Glide.with(FeedLikes.this).applyDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.temp)).load(jAr.getJSONObject(position).getString("upic")).
                                into(holder.messengerImageView);
                        holder.name.setText(jAr.getJSONObject(position).getString("uname"));
                    } catch (JSONException e1) {
                        e1.printStackTrace();
                    }


                try {
                    //holder.name.setText(""+param2.getJSONObject(position).getString("status_id"));
                    holder.react.setImageResource(drr[jAr.getJSONObject(position).getInt("reaction")]);
                } catch (Exception e1) {
                    e1.printStackTrace();
                    holder.react.setImageDrawable(AppCompatResources.getDrawable(FeedLikes.this, R.drawable.ic_insert_emoticon_black_24dp));
                }

            }

            @Override
            public int getItemCount() {
                return jAr == null ? 0 : jAr.length();
            }
        });
        getStatusReaction(getIntent().getStringExtra("post_id"));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        ImageView react;
        ImageView messengerImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.name);
            react = (ImageView) itemView.findViewById(R.id.react);
            messengerImageView = (ImageView) itemView.findViewById(R.id.avatarView);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    JSONArray jAr = null;

    void getStatusReaction(String postid) {
        ProgressDialog pd = ProgressDialog.show(this, "", "Getting Reactions", true, false);
        new ExecuteServices().execute(Constants.NODE_URL_GET +"get_feed_reaction_hiddy/" + postid, new ExecuteServices.OnServiceExecute() {
            @Override
            public void onServiceExecutedResponse(String response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pd.dismiss();
                        try {
                            jAr = null;
                            JSONObject job = new JSONObject(response);
                            jAr = job.getJSONArray("data");
                            list.getAdapter().notifyDataSetChanged();
                            // Toast.makeText(getActivity(), ""+jAr.length(), Toast.LENGTH_SHORT).show();
                            getSupportActionBar().setSubtitle("" + jAr.length() + (jAr.length() > 1 ? " reactions" : " reaction"));
                            if (jAr.length() == 0)
                                findViewById(R.id.no_item).setVisibility(View.VISIBLE);
                        } catch (JSONException e) {
                            //   Toast.makeText(getActivity(), "" + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    }

                });


            }

            @Override
            public void onServiceExecutedFailed(String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pd.dismiss();
                        findViewById(R.id.no_connection).setVisibility(View.VISIBLE);
                        //     Toast.makeText(getActivity(), "updated failed" + message, Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });
    }
}

