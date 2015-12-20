package com.trianguloy.llscript.repository.editor;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.trianguloy.llscript.repository.BuildConfig;
import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.internal.Utils;

/**
 * Created by Lukas on 06.09.2015.
 * manages the content of the editText
 */
class EditManager {

    @Nullable
    private EditText editText;
    private int textHash = -1;
    @Nullable
    private String text;

    @Nullable
    private String pageName;
    @Nullable
    private String pageId;

    public void assign(@NonNull EditText editText) {
        this.editText = editText;
        if (text != null) editText.setText(text);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            editText.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    //no-op
                }

                @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
                @Override
                public void onViewDetachedFromWindow(View v) {
                    text = getText();
                    assert EditManager.this.editText != null;
                    EditManager.this.editText.removeOnAttachStateChangeListener(this);
                    EditManager.this.editText = null;
                }
            });
        }
    }

    public void assign(@NonNull EditText editText, @NonNull String text) {
        this.text = text;
        assign(editText);
        if (textHash == -1) textHash = text.hashCode();
    }

    @Nullable
    public String getText() {
        if (editText == null) return text;
        String txt = editText.getText().toString();
        text = txt;
        return txt;
    }

    private void surroundOrAdd(@NonNull String prefix, String suffix, @NonNull String text) {
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
        return (editText != null ? editText.getText().toString().hashCode() : 0) == hashCode();
    }

    public void saved() {
        textHash = editText != null ? editText.getText().toString().hashCode() : 0;
    }

    public void toBundle(@NonNull Bundle bundle) {
        bundle.putString(Utils.getString(R.string.key_pageText), getText());
        bundle.putString(Utils.getString(R.string.key_pageName), pageName);
        bundle.putString(Utils.getString(R.string.key_pageId), pageId);
        bundle.putInt(Utils.getString(R.string.key_textHash), textHash);

    }

    public void fromBundle(@NonNull Bundle bundle) {
        text = bundle.getString(Utils.getString(R.string.key_pageText));
        pageName = bundle.getString(Utils.getString(R.string.key_pageName));
        pageId = bundle.getString(Utils.getString(R.string.key_pageId));
        textHash = bundle.getInt(Utils.getString(R.string.key_textHash));
    }


    @Nullable
    public String getPageName() {
        return pageName;
    }

    public void setPageName(@Nullable String pageName) {
        this.pageName = pageName;
    }

    @Nullable
    public String getPageId() {
        return pageId;
    }

    public void setPageId(@Nullable String pageId) {
        this.pageId = pageId;
    }

    public boolean hasPageId() {
        return pageId != null;
    }
    
    public void action(int id){
        switch (id) {
            case R.id.action_bold:
                surroundOrAdd("**", "**", Utils.getString(R.string.text_bold));
                break;
            case R.id.action_italic:
                surroundOrAdd("//", "//", Utils.getString(R.string.text_italic));
                break;
            case R.id.action_underline:
                surroundOrAdd("__", "__", Utils.getString(R.string.text_underline));
                break;
            case R.id.action_code:
                surroundOrAdd("<sxh javascript;>", "</sxh>", Utils.getString(R.string.text_code));
                break;
            case R.id.action_unorderedList:
                surroundOrAdd("  * ", "", Utils.getString(R.string.text_unorderedList));
                break;
            case R.id.action_orderedList:
                surroundOrAdd("  - ", "", Utils.getString(R.string.text_orderedList));
                break;
            default:
                if (BuildConfig.DEBUG)
                    Log.i(EditorActivity.class.getSimpleName(), "Ignored action " + id);
                break;
        }
    }

}
