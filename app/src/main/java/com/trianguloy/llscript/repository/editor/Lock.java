package com.trianguloy.llscript.repository.editor;

import android.view.View;
import android.widget.ProgressBar;

import com.trianguloy.llscript.repository.R;

/**
 * Created by Lukas on 07.09.2015.
 * Represents editorActivities accessibility state
 */
class Lock {
    private final EditorActivity activity;
    private boolean state;

    Lock(EditorActivity activity) {
        this.activity = activity;
        state = true;
    }

    void lock() {
        state = true;
        final ProgressBar bar = (ProgressBar) activity.findViewById(R.id.progressBar);
        assert bar != null;
        bar.setVisibility(View.VISIBLE);
    }

    void unlock() {
        state = false;
        final ProgressBar bar = (ProgressBar) activity.findViewById(R.id.progressBar);
        assert bar != null;
        bar.setVisibility(View.GONE);
    }

    public boolean isLocked() {
        return state;
    }

}
