package com.trianguloy.llscript.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by TrianguloY on 26/01/2015.
 * Reads a given Resource to a string
 */
public class StringFunctions {
    //From http://stackoverflow.com/questions/4087674/android-read-text-raw-resource-file
    //resId is always R.raw.script : Why do we have an argument here?
    public static String getRawFile(Context ctx, int resId) {
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




    public static class valueAndIndex {
        final String value;
        final int from;
        final int to;
        valueAndIndex(String v,int f,int t){
            value=v;
            from=f;
            to=t;
        }
        valueAndIndex(){
            value=null;
            from=-1;
            to=-1;
        }
    }

    //This function returns the string between beggining and ending in source starting from index, and the position o the matches (including the searched strings). If backwards is true it uses lastIndexOf
    public static valueAndIndex findBetween(String source, String beggining, String ending, int index, boolean backwards){
        int start;
        int end;
        valueAndIndex notFound = new valueAndIndex();


        if(!backwards){

            start = source.indexOf(beggining,index==-1?0:index);
            if(start==-1) return notFound;
            start+=beggining.length();

            end = source.indexOf(ending,start);
            if(end==-1) return notFound;

        }else{

            end = source.lastIndexOf(ending,index==-1?source.length():index);
            if(end==-1) return notFound;

            start = source.lastIndexOf(beggining,end-beggining.length());
            if(start==-1) return notFound;
            start+=beggining.length();

        }

        return new valueAndIndex(source.substring(start,end),start-beggining.length(),end+ending.length());
    }

    public static void saveSetToPref(SharedPreferences pref, String key, Set<String> set) {
        if (Build.VERSION.SDK_INT >= 11) pref.edit().putStringSet(key, set).apply();
        else pref.edit().putString(key, new JSONArray(set).toString()).apply();
    }

    public static Set<String> getSetFromPref(SharedPreferences pref, String key) {
        if (pref.contains(key)) {
            if (Build.VERSION.SDK_INT >= 11)
                return pref.getStringSet(key, Collections.<String>emptySet());
            HashSet<String> set = new HashSet<>();
            JSONArray array;
            try {
                array = new JSONArray(pref.getString(key, ""));
                for (int i = 0; i < array.length(); i++) {
                    set.add(array.getString(i));
                }
                return set;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return Collections.emptySet();
    }

    public static void saveMapToPref(SharedPreferences pref, String key, Map<String, Integer> map) {
        pref.edit().putString(key, new JSONObject(map).toString()).apply();
    }

    public static Map<String, Integer> getMapFromPref(SharedPreferences pref, String key) {
        if (pref.contains(key)) {
            try {
                HashMap<String, Integer> map = new HashMap<>();
                JSONObject object = new JSONObject(pref.getString(key, ""));
                Iterator<String> iterator = object.keys();
                while (iterator.hasNext()) {
                    String k = iterator.next();
                    map.put(k, (int) object.get(k));
                }
                return map;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return new HashMap<>();
    }

    public static int pageToHash(String html) {
        String newHash = StringFunctions.findBetween(html, "<div class=\"docInfo\">", "</div>", -1, false).value;
        if (newHash == null) return -1;
        else return newHash.hashCode();
    }

}
