package com.trianguloy.llscript.repository.internal;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;

/**
 * Created by Lukas on 25.04.2015.
 */
public class NoEventWebView extends WebView {
    MotionEvent last;

    public NoEventWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_UP:
                event.setAction(MotionEvent.ACTION_CANCEL);
                break;
            case MotionEvent.ACTION_DOWN:
                last = MotionEvent.obtain(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                if(last!=null)super.onTouchEvent(last);
                last = null;
                break;
        }
        return super.onTouchEvent(event);
    }
}
