package com.trianguloy.llscript.repository.internal;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.util.AttributeSet;
import android.widget.TextView;

import com.trianguloy.llscript.repository.R;

/**
 * Created by Lukas on 28.02.2015.
 * A TextView that shows the name instead of the identifier of a page
 */
public class SubTextView extends TextView {
    private String key;
    @Nullable
    private String text = null;
    private final Context context;

    public SubTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        if (text != null) setText(text);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        //inspection is wrong here! this statement is not constant!
        //noinspection ConstantConditions
        if (context == null) {
            this.text = (String) text;
        } else {
            key = (String) text;
            String prefix = context.getString(R.string.prefix_script);
            if(key.startsWith(prefix))key = key.substring(prefix.length());
            super.setText(Utils.getNameForPage(context, key), type);
        }
    }

    @Override
    public CharSequence getText() {
        return key;
    }

    @Nullable
    @Override
    public Editable getEditableText() {
        return null;
    }


}
