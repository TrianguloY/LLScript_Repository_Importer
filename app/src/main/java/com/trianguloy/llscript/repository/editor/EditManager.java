package com.trianguloy.llscript.repository.editor;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;

import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.internal.Utils;

/**
 * Created by Lukas on 06.09.2015.
 * manages the content of the editText
 */
public class EditManager {

    private EditText editText;
    private int textHash = -1;
    private String text;

    private String pageName;
    private String pageId;

    public void assign(EditText editText) {
        this.editText = editText;
        if (text != null) editText.setText(text);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            editText.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    //no-op
                }

                @SuppressLint("NewApi")
                @Override
                public void onViewDetachedFromWindow(View v) {
                    text = getText();
                    EditManager.this.editText.removeOnAttachStateChangeListener(this);
                    EditManager.this.editText = null;
                }
            });
        }
    }

    public void assign(EditText editText, String text) {
        this.text = text;
        assign(editText);
        if (textHash == -1) textHash = text.hashCode();
    }

    public String getText() {
        if (editText == null) return text;
        String txt = editText.getText().toString();
        text = txt;
        return txt;
    }

    public void surroundOrAdd(String prefix, String suffix, String text) {
        if (editText != null) {
            int start = editText.getSelectionStart();
            int end = editText.getSelectionEnd();
            Editable editable = editText.getEditableText();
            if (start == end) {
                editable.insert(start == -1 ? 0 : start, prefix + text + suffix);
                editText.setSelection(start + prefix.length(), start + prefix.length() + text.length());
            } else {
                editable.insert(end, suffix);
                editable.insert(start, prefix);
                editText.setSelection(start + prefix.length(), end + prefix.length());
            }
        }
    }

    public boolean isChanged() {
        return editText.getText().toString().hashCode() == hashCode();
    }

    public void saved() {
        textHash = editText.getText().toString().hashCode();
    }

    public void toBundle(Bundle bundle) {
        bundle.putString(Utils.getString(R.string.key_pageText), getText());
        bundle.putString(Utils.getString(R.string.key_pageName), pageName);
        bundle.putString(Utils.getString(R.string.key_pageId), pageId);
        bundle.putInt(Utils.getString(R.string.key_textHash), textHash);

    }

    public void fromBundle(Bundle bundle) {
        text = bundle.getString(Utils.getString(R.string.key_pageText));
        pageName = bundle.getString(Utils.getString(R.string.key_pageName));
        pageId = bundle.getString(Utils.getString(R.string.key_pageId));
        textHash = bundle.getInt(Utils.getString(R.string.key_textHash));
    }


    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public String getPageId() {
        return pageId;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public boolean hasPageId() {
        return pageId != null;
    }

}
