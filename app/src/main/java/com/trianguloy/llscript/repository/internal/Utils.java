package com.trianguloy.llscript.repository.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.trianguloy.llscript.repository.BuildConfig;
import com.trianguloy.llscript.repository.Constants;
import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.webViewer;

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
 * Collection of Utilities
 */
public final class Utils {

    private static Context context;

    private Utils() {
    }

    //class used in findBetween
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            pref.edit().putStringSet(key, set).apply();
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
        String newHash = Utils.findBetween(html, "<div class=\"docInfo\">", "</div>", -1, false).value;
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
                String name = Utils.findBetween(html, page + "\">", "<", 0, false).value;
                scripts.put(page, name);
            }
        }
        return scripts;
    }

    public static String getNameForPageFromPref(SharedPreferences pref, String page) {
        if (page.startsWith("script_"))
            page = page.substring(getString(R.string.prefix_script).length());
        String result = (String) getMapFromPref(pref, getString(R.string.pref_pageNames)).get(page);
        if (result != null) return result.trim();
        if (BuildConfig.DEBUG)
            Log.i(Utils.class.getSimpleName(), "Failed to find script name for " + page);
        return page;
    }

    public static String getNameFromUrl(String url) {
        final String idScript = "?id=script_";
        return url.substring(url.indexOf(idScript) + idScript.length());
    }

    public static String backClassToString(webViewer.backClass b) {
        JSONArray array = new JSONArray();
        array.put(b.url);
        array.put(b.posY);
        return array.toString();
    }

    public static webViewer.backClass stringToBackClass(String s) {
        try {
            JSONArray array = new JSONArray(s);
            return new webViewer.backClass((String) array.get(0), (int) array.get(1));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }


    //Checks for the launcher installed and sets it in the Constants variable. Returns false if no launcher was found
    public static boolean checkForLauncher(Context context) {

        //checks the installed package, extreme or not
        PackageManager pm = context.getPackageManager();
        PackageInfo pi = null;
        Constants.installedPackage = "";

        for (String p : Constants.packages) {
            try {
                pi = pm.getPackageInfo(p, PackageManager.GET_ACTIVITIES);
                Constants.installedPackage = p;
                break;
            } catch (PackageManager.NameNotFoundException ignored) {
                //empty, it just don't breaks and go to next iteration
            }
        }

        if (Constants.installedPackage.equals("") || pi == null) {
            //Non of the apps were found
            Dialogs.launcherNotFound(context);
            return false;
        }


        //Checks the version of the launcher

        if ((pi.versionCode % 1000) < Constants.minimumNecessaryVersion) {
            Dialogs.launcherOutdated(context);
            return false;
        }


        return true;
    }

    //Methods for retrieving strings in a non-context-environment
    public static void setContext(Context context) {
        Utils.context = context;
    }

    public static String getString(int resId) {
        if (context == null) throw new RuntimeException("Context not initialized");
        return context.getString(resId);
    }

}
