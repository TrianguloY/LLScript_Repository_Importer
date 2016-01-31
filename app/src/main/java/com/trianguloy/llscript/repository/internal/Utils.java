package com.trianguloy.llscript.repository.internal;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.trianguloy.llscript.repository.BuildConfig;
import com.trianguloy.llscript.repository.Constants;
import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.settings.Preferences;
import com.trianguloy.llscript.repository.web.ManagedWebView;
import com.trianguloy.llscript.repository.web.RPCManager;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by TrianguloY on 26/01/2015.
 * Collection of Utilities
 */
public final class Utils {

    private Utils() {
    }

    /**
     * @param source    the base string
     * @param beginning the prefix
     * @param ending    the suffix
     * @return first string between beginning and ending
     */
    @Nullable
    public static String findBetween(@NonNull String source, @NonNull String beginning, @NonNull String ending) {
        int start = source.indexOf(beginning, 0);
        if (start == -1) return null;
        start += beginning.length();

        int end = source.indexOf(ending, start);
        if (end == -1) return null;

        return source.substring(start, end);
    }

    /**
     * @param document should be the repository
     * @return all script names in the repository mapped to their pages
     */
    @NonNull
    public static Map<String, String> getAllScriptPagesAndNames(@NonNull Context context, @NonNull Document document) {
        HashMap<String, String> scripts = new HashMap<>();
        //find all scripts in the repository
        String prefix = context.getString(R.string.prefix_script);
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

    /**
     * @param context a context
     * @param page    page id
     * @return page name if found, otherwise page id
     */
    @NonNull
    public static String getNameForPage(@NonNull Context context, @NonNull String page) {
        Preferences pref = Preferences.getDefault(context);
        if (page.startsWith(context.getString(R.string.prefix_script)))
            page = page.substring(context.getString(R.string.prefix_script).length());
        String result = pref.getStringMap(R.string.pref_pageNames, Collections.<String, String>emptyMap()).get(page);
        if (result != null) return result.trim();
        if (BuildConfig.DEBUG)
            Log.i(Utils.class.getSimpleName(), "Failed to find script name for " + page);
        return page;
    }

    /**
     * @param url url of a script page
     * @return id of the page or, if not found url of the page
     */
    @NonNull
    public static String getIdFromUrl(@NonNull String url) {
        final String idScript = "?id=script_";
        int index = url.indexOf(idScript);
        if (index == -1) return url;
        return url.substring(index + idScript.length());
    }

    /**
     * @param context a context
     * @return the launcher package, if any
     */
    @Nullable
    private static PackageInfo getInstalledLauncher(@NonNull Context context) {
        PackageManager pm = context.getPackageManager();

        for (String p : Constants.PACKAGES) {
            try {
                PackageInfo info = pm.getPackageInfo(p, PackageManager.GET_ACTIVITIES);
                pkg = info.packageName;
                return info;
            } catch (PackageManager.NameNotFoundException ignored) {
                //empty, it just doesn't break and go to next iteration
            }
        }
        return null;
    }

    /**
     * used for caching the package
     */
    private static String pkg = null;

    /**
     * @param context a context
     * @return the package name of the installed launcher, if any
     */
    @Nullable
    public static String getLauncherPackage(@NonNull Context context) {
        if (pkg == null) {
            PackageInfo info = getInstalledLauncher(context);
            if (info != null) pkg = info.packageName;
        }
        return pkg;
    }

    /**
     * @param context a context
     * @return if LL was found and is of sufficient version
     */
    public static boolean hasValidLauncher(@NonNull Context context) {
        PackageInfo pi = getInstalledLauncher(context);
        return pi != null && (pi.versionCode % Constants.VERSIONCODE_MODULO) >= Constants.MINIMUM_NECESSARY_VERSION;
    }

    /**
     * alerts the user of potential Launcher problems
     *
     * @param context a context
     */
    public static void alertLauncherProblemsIfAny(@NonNull Context context) {
        PackageInfo pi = getInstalledLauncher(context);

        if (pi == null) {
            //None of the apps were found
            Dialogs.launcherNotFound(context);
        }
        //Checks the version of the launcher
        else if ((pi.versionCode % Constants.VERSIONCODE_MODULO) < Constants.MINIMUM_NECESSARY_VERSION) {
            Dialogs.launcherOutdated(context);
        }
    }

    //matches values_notify resource
    private static final int SHOW_NONE = 0;
    private static final int SHOW_TOAST = 1;
    private static final int SHOW_DIALOG = 2;

    /**
     * loads and shows changes to subscriptions
     *
     * @param context a context able to show dialogs
     * @param webView the webView to load clicked pages in
     */
    public static void showChangedSubscriptionsIfAny(@NonNull final Context context, @NonNull final ManagedWebView webView) {
        final Preferences sharedPref = Preferences.getDefault(context);
        RPCManager.getInstance(context).getChangedSubscriptions(context, new RPCManager.Listener<List<String>>() {
            @Override
            public void onResult(@NonNull RPCManager.Result<List<String>> result) {
                if (result.getStatus() == RPCManager.RESULT_OK) {
                    List<String> updated = result.getResult();
                    assert updated != null;
                    if (updated.size() > 0) {
                        StringBuilder pages = new StringBuilder();
                        for (String s : updated) {
                            pages.append(Utils.getNameForPage(context, s)).append("\n");
                        }
                        int showAs = Integer.valueOf(sharedPref.getString(R.string.pref_changedSubs, "2"));
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
                        RPCManager.getInstance(context).setTimestampToCurrent(sharedPref, null);
                    }
                }
            }
        });
    }

    /**
     * loads and shows new scripts
     *
     * @param context      a context able to show dialogs
     * @param repoDocument the repository (data loaded from it)
     * @param webView      the webView to load clicked pages in
     */
    public static void showNewScriptsIfAny(@NonNull Context context, @NonNull Document repoDocument, @NonNull final ManagedWebView webView) {
        final Preferences sharedPref = Preferences.getDefault(context);
        //new method: based on the scripts found
        Map<String, String> map = Utils.getAllScriptPagesAndNames(context, repoDocument);
        sharedPref.edit().putStringMap(R.string.pref_pageNames, map).apply();
        Set<String> currentScripts = map.keySet();
        if (sharedPref.contains(R.string.pref_Scripts)) {
            Set<String> oldScripts = sharedPref.getStringSet(R.string.pref_Scripts, Collections.<String>emptySet());
            HashSet<String> newScripts = new HashSet<>(currentScripts);
            newScripts.removeAll(oldScripts);
            if (!newScripts.isEmpty()) {
                //found new Scripts
                sharedPref.edit().putStringSet(R.string.pref_Scripts, currentScripts).apply();
                ArrayList<String> newScriptNames = new ArrayList<>();
                for (String s : newScripts) {
                    newScriptNames.add(map.get(s));
                }
                StringBuilder names = new StringBuilder();
                for (int i = 0; i < newScriptNames.size(); i++) {
                    names.append(newScriptNames.get(i)).append("\n");
                }
                names.deleteCharAt(names.length() - 1);
                int showAs = Integer.valueOf(sharedPref.getString(R.string.pref_newScripts, "2"));
                switch (showAs) {
                    case SHOW_NONE:
                        break;
                    case SHOW_TOAST:
                        Toast.makeText(context,
                                (newScriptNames.size() == 1 ?
                                        context.getString(R.string.toast_oneNewScript) :
                                        context.getString(R.string.toast_severalNewScripts)) + names.toString(),
                                Toast.LENGTH_LONG).show();
                        break;
                    case SHOW_DIALOG:
                        Dialogs.newScripts(context, webView, Arrays.asList(newScripts.toArray(new String[newScripts.size()])));
                        break;
                }
                Toast.makeText(context, names.toString(), Toast.LENGTH_LONG).show();
            }
        } else {
            //No info about previous scripts. Only save the current scripts
            sharedPref.edit().putStringSet(R.string.pref_Scripts, currentScripts).apply();
        }
    }
}
