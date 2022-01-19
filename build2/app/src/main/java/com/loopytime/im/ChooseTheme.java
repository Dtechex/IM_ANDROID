package com.loopytime.im;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.loopytime.external.videotrimmer.utils.Screen;
import com.loopytime.external.videotrimmer.view.CircleFillView;
import com.loopytime.helper.MaterialColors;


/**
 * Created by HP on 11/5/2015.
 */
public class ChooseTheme extends AppCompatActivity {
    RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mlLayoutManager;
    RecyclerView.Adapter mAdapterRecycler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choose_theme);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.BLACK);
        }
findViewById(R.id.root).setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        finish();
    }
});
        mRecyclerView=(RecyclerView)findViewById(R.id.recycler);
        int span = Screen.getWidth() / Screen.dp(72);
        span = span > 6 ? 6 : span;
        mlLayoutManager = new GridLayoutManager(this, span);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mlLayoutManager);
        mAdapterRecycler = new ColorViewAdapter();


        mRecyclerView.setAdapter(mAdapterRecycler);
    }

    public class ColorViewAdapter extends
            RecyclerView.Adapter<ColorViewAdapter.ViewHolder> {

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder implements
                View.OnClickListener {

            // each data item is just a string in this case
            CircleFillView icon1;

            public ViewHolder(View v) {
                super(v);
                // c=(TextView) v.findViewById(R.id.title_cont);//.findViewWithTag("jjk");
                icon1 = (CircleFillView) v.findViewWithTag("jj");//.findViewById(R.id.icon_ty);
                icon1.setOnClickListener(this);

            }

            @Override
            public void onClick(View v) {
              /*  ActorStyle style = ActorSDK.sharedActor().style;
                style.setAvatarBackgroundColor(MaterialColors.CONVERSATION_PALETTE.get(getAdapterPosition()).toActionBarColor(ChooseTheme.this));
*/
                getSharedPreferences("wall", Context.MODE_PRIVATE).edit().putInt(MaterialColors.THEME,getAdapterPosition()).commit();
              setResult(Activity.RESULT_OK);
                finish();

            }

        }


        // Create new views (invoked by the layout manager)
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent,
                                             int viewType) {
            LinearLayout picLL = new LinearLayout(ChooseTheme.this);
            picLL.layout(0, 0, Screen.dp(72), Screen.dp(72));
            picLL.setLayoutParams(new LinearLayout.LayoutParams(Screen.dp(72), Screen.dp(72)));
            picLL.setOrientation(LinearLayout.VERTICAL);
            CircleFillView myImage = new CircleFillView(ChooseTheme.this);
            myImage.setMinimumHeight(Screen.dp(72));
            myImage.setMinimumWidth(Screen.dp(72));
            myImage.setTag("jj");
            int pad= Screen.dp(4);
            picLL.setPadding(pad,pad,pad,pad);
            //   TextView e = new TextView(ctx);
            //  myImage.setTag("jjk");
            //e.setText("ascvsdfbvadfsb1");
            picLL.addView(myImage);
            // create a new view
            // View v =LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_send_categories_item_view, parent, false);

            return new ViewHolder(picLL);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            holder.icon1.update(MaterialColors.CONVERSATION_PALETTE.get(position).toActionBarColor(ChooseTheme.this),true);
           // holder.icon1.setImageResource(getResources().getIdentifier("m_" + (position+1), "drawable", getActivity().getPackageName()));
        }

        @Override
        public int getItemViewType(int position) {
            // TODO Auto-generated method stub
            // boolean isHead=isHeader(position);

            return position;
            // super.getItemViewType(position);
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {

            return MaterialColors.CONVERSATION_PALETTE.size();
        }
    }

}
