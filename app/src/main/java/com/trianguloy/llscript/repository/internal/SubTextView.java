package com.trianguloy.llscript.repository.internal;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by Lukas on 28.02.2015.
 * A TextView that shows the name instead of the identifier of a page
 */
public class SubTextView extends TextView {
    private String key;
    private String text;
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
        if (context != null) {
            key = (String) text;
            super.setText(StringFunctions.getNameForPageFromPref(PreferenceManager.getDefaultSharedPreferences(context), context, key), type);
        } else this.text = (String) text;
    }

    @Override
    public CharSequence getText() {
        return key;
    }

    @Override
    public Editable getEditableText() {
        return null;
    }


}
