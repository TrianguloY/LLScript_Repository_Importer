package com.trianguloy.llscript.repository.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.trianguloy.llscript.repository.BuildConfig;
import com.trianguloy.llscript.repository.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
public final class StringFunctions {
    private StringFunctions(){}

    //This function returns the string between beginning and ending in source starting from index, and the position o the matches (including the searched strings). If backwards is true it uses lastIndexOf
    public static valueAndIndex findBetween(String source, String beginning, String ending, int index, boolean backwards) {
        int start;
        int end;
        valueAndIndex notFound = new valueAndIndex();


        if (!backwards) {

            start = source.indexOf(beginning, index == -1 ? 0 : index);
            if (start == -1) return notFound;
            start += beginning.length();

            end = source.indexOf(ending, start);
            if (end == -1) return notFound;

        } else {

            end = source.lastIndexOf(ending, index == -1 ? source.length() : index);
            if (end == -1) return notFound;

            start = source.lastIndexOf(beginning, end - beginning.length());
            if (start == -1) return notFound;
            start += beginning.length();

        }

        return new valueAndIndex(source.substring(start, end), start - beginning.length(), end + ending.length());
    }

    public static void saveSetToPref(SharedPreferences pref, String key, Set<String> set) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) pref.edit().putStringSet(key, set).apply();
        else pref.edit().putString(key, new JSONArray(set).toString()).apply();
    }

    public static Set<String> getSetFromPref(SharedPreferences pref, String key) {
        if (pref.contains(key)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
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

    public static void saveMapToPref(SharedPreferences pref, String key, Map<String, Object> map) {
        pref.edit().putString(key, new JSONObject(map).toString()).apply();
    }

    public static Map<String, Object> getMapFromPref(SharedPreferences pref, String key) {
        if (pref.contains(key)) {
            try {
                HashMap<String, Object> map = new HashMap<>();
                JSONObject object = new JSONObject(pref.getString(key, ""));
                Iterator<String> iterator = object.keys();
                while (iterator.hasNext()) {
                    String k = iterator.next();
                    map.put(k, object.get(k));
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

    public static Map<String, String> getAllScriptPagesAndNames(String html) {
        HashMap<String, String> scripts = new HashMap<>();
        //find all scripts in the repository
        String[] temp = html.split("title=\"script_");
        for (int i = 1; i < temp.length; i++) {
            String s = temp[i];
            if (!s.startsWith("repository\"") && !s.startsWith("template\""))//exclude the repository itself and the script template
            {
                String page = s.substring(0, s.indexOf('"'));
                String name = StringFunctions.findBetween(html, page + "\">", "<", 0, false).value;
                scripts.put(page, name);
            }
        }
        return scripts;
    }

    public static String getNameForPageFromPref(SharedPreferences pref, Context ctx, String page) {
        if(page.startsWith("script_"))page = page.substring(ctx.getString(R.string.prefix_script).length());
        String result = (String) getMapFromPref(pref, ctx.getString(R.string.pref_pageNames)).get(page);
        if (result != null) return result.trim();
        if (BuildConfig.DEBUG) Log.i("StringFunctions", "Failed to find script name for " + page);
        return page;
    }

    public static String getNameFromUrl(String url) {
        final String idScript = "?id=script_";
        return url.substring(url.indexOf(idScript) + idScript.length());
    }

    public static class valueAndIndex {
        public final String value;
        public final int from;
        public final int to;

        public valueAndIndex(String v, int f, int t) {
            value = v;
            from = f;
            to = t;
        }

        public valueAndIndex() {
            value = null;
            from = -1;
            to = -1;
        }
    }

}
