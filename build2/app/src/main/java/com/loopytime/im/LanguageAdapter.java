package com.loopytime.im;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.shadow.apache.commons.lang3.LocaleUtils;

import java.util.ArrayList;
import java.util.Locale;

public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.ViewHolder> {
    ArrayList<String> langNames = new ArrayList<>();
    ArrayList<String> langCodes = new ArrayList<>();
    OnLanguageSelection mOnLanguageSelection;

    public interface OnLanguageSelection

    {
        public void onLanguageSelected(String lng);
    }

    public void setOnLanguageSelection(OnLanguageSelection mOnLanguageSelection) {
        this.mOnLanguageSelection = mOnLanguageSelection;
    }

    public LanguageAdapter(ArrayList<String> langCodes) {
        this.langCodes.addAll(langCodes);
        // this.langNames.addAll(langNames);

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(ApplicationClass.getInstance()).inflate(R.layout.item_language, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        String ln = langCodes.get(i);
        Locale loc = null;
        if (ln.contains("-")) {
            loc = new Locale("", ln.substring(ln.indexOf("-") + 1, ln.length()));
            ln = ln.substring(0, ln.indexOf("-"));

        }//Locale loc = new Locale("","IN");
        String co = loc != null ? loc.getDisplayCountry() : "";
        co = TextUtils.isEmpty(co) ? "" : co.replaceAll(" ","");
        co = TextUtils.isEmpty(co) ? "" : " - " + co;
        viewHolder.title.setText(LocaleUtils.toLocale(ln).getDisplayName() + co);
    }

    @Override
    public int getItemCount() {
        return langCodes.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnLanguageSelection.onLanguageSelected(langCodes.get(getAdapterPosition()));
                }
            });
        }
    }
}
