package com.trianguloy.llscript.repository.internal;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;
import android.widget.Toast;

import com.trianguloy.llscript.repository.BuildConfig;
import com.trianguloy.llscript.repository.Constants;
import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.settings.Preferences;
import com.trianguloy.llscript.repository.web.ManagedWebView;
import com.trianguloy.llscript.repository.web.RPCManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
        @Nullable
        public final String value;
        public final int from;
        public final int to;

        public valueAndIndex(@Nullable String v, int f, int t) {
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
    @NonNull
    public static valueAndIndex findBetween(@NonNull String source, @NonNull String beginning, @NonNull String ending, int index, boolean backwards) {
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

    public static void saveMapToPref(@NonNull Preferences pref, String key, Map<String, Object> map) {
        pref.edit().putString(key, new JSONObject(map).toString()).apply();
    }

    @NonNull
    public static Map<String, Object> getMapFromPref(@NonNull Preferences pref, String key) {
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

    @NonNull
    public static Map<String, String> getAllScriptPagesAndNames(@NonNull Document document) {
        HashMap<String, String> scripts = new HashMap<>();
        //find all scripts in the repository
        String prefix = getString(R.string.prefix_script);
        Elements temp = document.select("[title^=" + prefix + "]");
        for (Element e : temp) {
            String title = e.attr("title").substring(prefix.length());
            //exclude the repository itself and the script template
            if (!title.startsWith("repository") && !title.startsWith("template")) {
                String name = e.ownText();
                scripts.put(title, name);
            }
        }
        return scripts;
    }

    @NonNull
    public static String getNameForPageFromPref(@NonNull Preferences pref, @NonNull String page) {
        if (page.startsWith(getString(R.string.prefix_script)))
            page = page.substring(getString(R.string.prefix_script).length());
        String result = (String) getMapFromPref(pref, getString(R.string.pref_pageNames)).get(page);
        if (result != null) return result.trim();
        if (BuildConfig.DEBUG)
            Log.i(Utils.class.getSimpleName(), "Failed to find script name for " + page);
        return page;
    }

    @NonNull
    public static String getNameFromUrl(@NonNull String url) {
        final String idScript = "?id=script_";
        int index = url.indexOf(idScript);
        if (index == -1) return url;
        return url.substring(index + idScript.length());
    }

    //Checks for the launcher installed and sets it in the Constants variable. Returns false if no launcher was found
    public static boolean checkForLauncher(@NonNull Context context) {

        //checks the installed package, extreme or not
        PackageManager pm = context.getPackageManager();
        PackageInfo pi = null;
        Constants.installedPackage = null;

        for (String p : Constants.PACKAGES) {
            try {
                pi = pm.getPackageInfo(p, PackageManager.GET_ACTIVITIES);
                Constants.installedPackage = p;
                break;
            } catch (PackageManager.NameNotFoundException ignored) {
                //empty, it just don't breaks and go to next iteration
            }
        }

        if (Constants.installedPackage == null || pi == null) {
            //Non of the apps were found
            Constants.installedPackage = null;
            Dialogs.launcherNotFound(context);
            return false;
        }


        //Checks the version of the launcher

        if ((pi.versionCode % Constants.VERSIONCODE_MODULO) < Constants.MINIMUM_NECESSARY_VERSION) {
            Constants.installedPackage = null;
            Dialogs.launcherOutdated(context);
            return false;
        }


        return true;
    }

    //Methods for retrieving strings in a non-context-environment
    public static void setContext(Context context) {
        Utils.context = context;
    }

    public static String getString(@StringRes int resId) {
        if (context == null) throw new NoContextException();
        return context.getString(resId);
    }

    public static Context getContext() {
        if (context == null) throw new NoContextException();
        return context;
    }

    private static final int SHOW_NONE = 0;
    private static final int SHOW_TOAST = 1;
    private static final int SHOW_DIALOG = 2;

    public static void showChangedSubscriptionsIfAny(@NonNull final Context context, @NonNull final ManagedWebView webView) {
        final Preferences sharedPref = Preferences.getDefault(context);
        RPCManager.getChangedSubscriptions(sharedPref, new RPCManager.Listener<List<String>>() {
            @Override
            public void onResult(@NonNull RPCManager.Result<List<String>> result) {
                if (result.getStatus() == RPCManager.RESULT_OK) {
                    List<String> updated = result.getResult();
                    assert updated != null;
                    if (updated.size() > 0) {
                        StringBuilder pages = new StringBuilder();
                        for (String s : updated) {
                            pages.append(Utils.getNameForPageFromPref(sharedPref, s)).append("\n");
                        }
                        int showAs = Integer.valueOf(sharedPref.getString(getString(R.string.pref_changedSubs), "2"));
                        switch (showAs) {
                            case SHOW_NONE:
                                break;
                            case SHOW_TOAST:
                                Toast.makeText(context, pages.toString(), Toast.LENGTH_LONG).show();
                                break;
                            case SHOW_DIALOG:
                                Dialogs.changedSubscriptions(context, webView, updated);
                                break;
                        }
                        RPCManager.setTimestampToCurrent(sharedPref, null);
                    }
                }
            }
        });
    }

    public static void showNewScriptsIfAny(@NonNull Context context, @NonNull Document repoDocument, @NonNull final ManagedWebView webView) {
        final Preferences sharedPref = Preferences.getDefault(context);
        //new method: based on the scripts found
        Map<String, String> map = Utils.getAllScriptPagesAndNames(repoDocument);
        HashMap<String, Object> temp = new HashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            temp.put(entry.getKey(), entry.getValue());
        }
        Utils.saveMapToPref(sharedPref, getString(R.string.pref_pageNames), temp);
        Set<String> currentScripts = map.keySet();
        if (sharedPref.contains(getString(R.string.pref_Scripts))) {
            Set<String> oldScripts = sharedPref.getStringSet(getString(R.string.pref_Scripts), Collections.<String>emptySet());
            HashSet<String> newScripts = new HashSet<>(currentScripts);
            newScripts.removeAll(oldScripts);
            if (!newScripts.isEmpty()) {
                //found new Scripts
                sharedPref.edit().putStringSet(getString(R.string.pref_Scripts), currentScripts).apply();
                ArrayList<String> newScriptNames = new ArrayList<>();
                for (String s : newScripts) {
                    newScriptNames.add(map.get(s));
                }
                StringBuilder names = new StringBuilder();
                for (int i = 0; i < newScriptNames.size(); i++) {
                    names.append(newScriptNames.get(i)).append("\n");
                }
                names.deleteCharAt(names.length() - 1);
                int showAs = Integer.valueOf(sharedPref.getString(getString(R.string.pref_newScripts), "2"));
                switch (showAs) {
                    case SHOW_NONE:
                        break;
                    case SHOW_TOAST:
                        Toast.makeText(context, (newScriptNames.size() == 1 ? getString(R.string.toast_oneNewScript) : getString(R.string.toast_severalNewScripts)) + names.toString(), Toast.LENGTH_LONG);
                        break;
                    case SHOW_DIALOG:
                        Dialogs.newScripts(context, webView, Arrays.asList(newScripts.toArray(new String[newScripts.size()])));
                        break;
                }
                Toast.makeText(context, names.toString(), Toast.LENGTH_LONG).show();
            }
        } else {
            //No info about previous scripts. Only save the current scripts
            sharedPref.edit().putStringSet(getString(R.string.pref_Scripts), currentScripts).apply();
        }
    }

    public static class NoContextException extends RuntimeException {
        public NoContextException() {
            super("Context not initialized");
        }
    }


}
