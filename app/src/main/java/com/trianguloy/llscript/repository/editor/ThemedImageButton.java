package com.trianguloy.llscript.repository.editor;

import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.widget.ImageButton;

import com.trianguloy.llscript.repository.BuildConfig;
import com.trianguloy.llscript.repository.Constants;
import com.trianguloy.llscript.repository.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by Lukas on 23.06.2015.
 * Image is inverted based on theme
 */
public class ThemedImageButton extends ImageButton {

    public ThemedImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        //http://stackoverflow.com/questions/7267852/android-how-to-obtain-the-resource-id-of-the-current-theme
        int themeResId = 0;
        try {
            Class<?> clazz = ContextThemeWrapper.class;
            Method method = clazz.getMethod("getThemeResId");
            method.setAccessible(true);
            themeResId = (Integer) method.invoke(context);
        } catch (@NonNull NoSuchMethodException | IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
            if(BuildConfig.DEBUG)Log.e("ThemedImageButton", "Failed to get theme resource ID", e);
        }
        if(themeResId == R.style.Theme_Dark){
            float[] cn = {
                    -1.0f, 0, 0, 0, Constants.RGB_MAX, //red
                    0, -1.0f, 0, 0, Constants.RGB_MAX, //green
                    0, 0, -1.0f, 0, Constants.RGB_MAX, //blue
                    0, 0, 0, 1.0f, 0 //alpha
            };
            ColorFilter colorFilter = new ColorMatrixColorFilter(cn);
            setColorFilter(colorFilter);
        }
    }
}
