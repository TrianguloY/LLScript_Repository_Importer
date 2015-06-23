package com.trianguloy.llscript.repository.internal;

import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.widget.ImageButton;

import com.trianguloy.llscript.repository.BuildConfig;
import com.trianguloy.llscript.repository.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by Lukas on 23.06.2015.
 */
public class ThemedImageButton extends ImageButton {

    public ThemedImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
            int themeResId = 0;
            try {
                Class<?> clazz = ContextThemeWrapper.class;
                Method method = clazz.getMethod("getThemeResId");
                method.setAccessible(true);
                themeResId = (Integer) method.invoke(context);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
                if(BuildConfig.DEBUG)Log.e("ThemedImageButton", "Failed to get theme resource ID", e);
            }
        if(themeResId == R.style.Theme_Dark){
            float[] cn = {
                    -1.0f, 0, 0, 0, 255, //red
                    0, -1.0f, 0, 0, 255, //green
                    0, 0, -1.0f, 0, 255, //blue
                    0, 0, 0, 1.0f, 0 //alpha
            };

            ColorFilter colorFilter = new ColorMatrixColorFilter(cn);
            setColorFilter(colorFilter);
        }
    }


}
