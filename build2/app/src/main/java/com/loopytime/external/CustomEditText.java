package com.loopytime.external;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;

/**
 * Created by hitasoft on 12/2/16.
 */
public class CustomEditText extends AppCompatEditText {

    private Context context;
    private AttributeSet attrs;
    private int defStyle;

    public CustomEditText(Context context) {
        super(context);
        this.context = context;
       // init();
    }



    public CustomEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.attrs = attrs;
       // init();
    }

    public CustomEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        this.attrs = attrs;
        this.defStyle = defStyle;
       // init();
    }

   /* private void init() {
        Typeface font = Typeface.createFromAsset(getContext().getAssets(), "Proxima Nova Regular.otf");
        this.setTypeface(font);
    }*/

    @Override
    public void setTypeface(Typeface tf, int style) {
       // tf = Typeface.createFromAsset(getContext().getAssets(), "Proxima Nova Regular.otf");
        super.setTypeface(tf, style);
    }

    @Override
    public void setTypeface(Typeface tf) {
        //tf = Typeface.createFromAsset(getContext().getAssets(), "Proxima Nova Regular.otf");
        super.setTypeface(tf);
    }

    private KeyImeChange keyImeChangeListener;

    public void setKeyImeChangeListener(KeyImeChange listener)
    {
        keyImeChangeListener = listener;
    }

    public interface KeyImeChange
    {
        public void onKeyIme(int keyCode, KeyEvent event);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyImeChangeListener != null)
        {
            keyImeChangeListener.onKeyIme(keyCode, event);
        }
        return false;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
        final InputConnection ic = super.onCreateInputConnection(editorInfo);
        EditorInfoCompat.setContentMimeTypes(editorInfo, new String[]{"image/*", "image/png", "image/jpeg"});

        return InputConnectionCompat.createWrapper(ic, editorInfo,
                (inputContentInfo, flags, opts) -> {
                    Toast.makeText(getContext(),"Hiddy Doesn't support image insertion here",Toast.LENGTH_SHORT).show();
                    return true;
                });
    }
}