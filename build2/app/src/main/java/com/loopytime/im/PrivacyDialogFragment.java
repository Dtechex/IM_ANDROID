package com.loopytime.im;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.RadioButton;
import android.widget.TextView;

import com.loopytime.helper.OkCallback;

public class PrivacyDialogFragment extends DialogFragment implements View.OnClickListener {

    private final String TAG = this.getClass().getSimpleName();
    private RadioButton btnEveryOne, btnMyContacts, btnNoBody;
    private TextView txtTitle;
    private OkCallback callBack;
    private CharSequence selected;
    private String title;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_privacy, container, false);
        btnEveryOne = v.findViewById(R.id.btnEveryOne);
        btnMyContacts = v.findViewById(R.id.btnMyContacts);
        btnNoBody = v.findViewById(R.id.btnNoBody);
        txtTitle = v.findViewById(R.id.txtTitle);

        txtTitle.setText(title);
        if (selected != null) {
            if (selected.equals(getString(R.string.everyone))) {
                btnEveryOne.setChecked(true);
            } else if (selected.equals(getString(R.string.my_contacts))) {
                btnMyContacts.setChecked(true);
            } else if (selected.equals(getString(R.string.nobody))) {
                btnNoBody.setChecked(true);
            }
        }
        btnEveryOne.setOnClickListener(this);
        btnMyContacts.setOnClickListener(this);
        btnNoBody.setOnClickListener(this);

        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        return v;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnEveryOne:
                if (callBack != null) {
                    callBack.onOkClicked(getString(R.string.everyone));
                }
                break;

            case R.id.btnMyContacts:
                if (callBack != null) {
                    callBack.onOkClicked(getString(R.string.my_contacts));
                }
                break;

            case R.id.btnNoBody:
                if (callBack != null) {
                    callBack.onOkClicked(getString(R.string.nobody));
                }
                break;
        }
    }

    public void setCallBack(OkCallback callBack) {
        this.callBack = callBack;
    }

    public void setSelected(CharSequence selected) {
        this.selected = selected;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
