package com.trianguloy.llscript.repository.editor;

import android.view.View;
import android.widget.ProgressBar;

import com.trianguloy.llscript.repository.R;

/**
 * Created by Lukas on 07.09.2015.
 */
class Lock {
    private EditorActivity activity;
    boolean state;

    public Lock(EditorActivity activity) {
        this.activity = activity;
        state = true;
    }

    public void lock() {
        state = true;
        final ProgressBar bar = (ProgressBar) activity.findViewById(R.id.progressBar);
        assert bar != null;
        bar.setVisibility(View.VISIBLE);
    }

    public void unlock() {
        state = false;
        final ProgressBar bar = (ProgressBar) activity.findViewById(R.id.progressBar);
        assert bar != null;
        bar.setVisibility(View.GONE);
    }

    public boolean isLocked() {
        return state;
    }

}
