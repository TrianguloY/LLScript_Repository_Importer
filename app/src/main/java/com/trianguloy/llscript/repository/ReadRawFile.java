package com.trianguloy.llscript.repository;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by USUARIO on 26/01/2015.
 * Reads a given Resource to a string
 */
public class ReadRawFile {
    //From http://stackoverflow.com/questions/4087674/android-read-text-raw-resource-file
    public static String getString(Context ctx, int resId) {
        InputStream inputStream = ctx.getResources().openRawResource(resId);
        try{
        byte[] buff = new byte[1024];
        int read;
        StringBuilder text = new StringBuilder();
        while ((read = inputStream.read(buff)) > 0) {
            text.append(new String(buff, 0, read));
        }
        return text.toString();
        } catch (IOException e) {
            return null;
        }
        finally {
            try {
                inputStream.close();
            } catch (IOException ignored) {
            }
        }
    }
}
