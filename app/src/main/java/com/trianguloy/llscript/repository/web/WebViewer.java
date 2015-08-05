package com.trianguloy.llscript.repository.web;

import android.app.ActionBar;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.http.HttpResponseCache;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.Html;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.trianguloy.llscript.repository.Constants;
import com.trianguloy.llscript.repository.EditorActivity;
import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.RepositoryImporter;
import com.trianguloy.llscript.repository.ScriptImporter;
import com.trianguloy.llscript.repository.SettingsActivity;
import com.trianguloy.llscript.repository.internal.AppChooser;
import com.trianguloy.llscript.repository.internal.Dialogs;
import com.trianguloy.llscript.repository.internal.PageCacheManager;
import com.trianguloy.llscript.repository.internal.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Main activity: displays the webView used to load scripts
 */

public class WebViewer extends Activity {

    private static final int SHOW_NONE = 0;
    private static final int SHOW_TOAST = 1;
    private static final int SHOW_DIALOG = 2;

    //Elements
    private ManagedWebView webView; //webView element
    private Button button; //button element
    private ProgressBar progressBar; //spinner

    private String sentUrl;

    //User vars
    private SharedPreferences sharedPref;//user saved data

    //Callbacks
    private Boolean close = false; //if pressing back will close or not

    private Menu menu;

    private Bundle savedInstanceState;

    //Application functions
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Utils.checkForLauncher(this)) {
            return;
        }

        //initialize variables
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        RepositoryImporter.setTheme(this, sharedPref);
        this.savedInstanceState = savedInstanceState;
        if (upgradeFromOldVersion()) init();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        getChangedSubscriptions();

        if (intent.hasExtra(Constants.extraOpenUrl)
                &&//if has both extras
                intent.hasExtra(Constants.extraOpenUrlTime)
                &&//and if the time passed is less than five seconds (to avoid be launched after closed, because the intent is kept)
                intent.getLongExtra(Constants.extraOpenUrlTime, 0) + 5000 > System.currentTimeMillis()
                ) {
            sentUrl = intent.getStringExtra(Constants.extraOpenUrl);
            if (webView != null) loadSentUrl();
        }
        super.onNewIntent(intent);
    }

    private void loadSentUrl() {
        if (sentUrl == null) sentUrl = getString(R.string.link_repository);
        if (!sentUrl.equals(getString(R.string.link_repository)) && !sharedPref.getBoolean(getString(R.string.pref_directReturn), false) && !webView.hasPage()) {
            webView.dropOnStackWithoutShowing(getString(R.string.link_repository));
        }
        webView.show(sentUrl);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_webviewer, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.action_mainPage:
                //load the main page
                webView.show(getString(R.string.link_repository));
                break;
            case R.id.action_openInBrowser:
                new AppChooser(this, Uri.parse(webView.getUrl()), getString(R.string.title_appChooserNormal), getString(R.string.message_noBrowser), null).show();
                break;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.action_subscribe:
                subscribeToCurrent();
                break;
            case R.id.action_unsubscribe:
                unsubscribeCurrent();
                break;
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.editor:
                Intent i = new Intent(this, EditorActivity.class);
                i.setAction(webView.getUrl());
                startActivity(i);
                break;
            case R.id.action_share:
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, webView.getUrl());
                startActivity(Intent.createChooser(share, getString(R.string.title_share)));
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        if (webView.backPossible()) webView.performBack();
        else if (!close && !sharedPref.getBoolean(getString(R.string.pref_singleClose), false)) {
            //Press back while the toast is still displayed to close
            Toast.makeText(getApplicationContext(), R.string.toast_backToClose, Toast.LENGTH_SHORT).show();
            close = true;
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    //when the toast disappear
                    close = false;
                }
            }, 2000);//2000ms is the default time for the TOAST_LENGTH_SHORT
        } else
            finish();
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && sharedPref.getBoolean(getString(R.string.pref_longPressClose), true)) {
            finish();
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    protected void onStop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            HttpResponseCache cache = HttpResponseCache.getInstalled();
            if (cache != null) {
                cache.flush();
            }
        }
        PageCacheManager.persist();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) {
            webView.saveToInstanceState(outState);
        }
    }

    private boolean restore(@NonNull Bundle savedInstanceState) {
        return webView.restoreFromInstanceState(savedInstanceState);
    }

    private void init() {
        //parse the Intent
        onNewIntent(getIntent());
        //Normal activity
        initializeWeb();
    }

    private void initializeWeb() {
        //Main Activity. Run on onCreate when normal launch
        setContentView(R.layout.activity_webviewer);

        //initialize vars
        button = (Button) findViewById(R.id.button);
        webView = (ManagedWebView) findViewById(R.id.webView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        webView.setListener(new ManagedWebView.Listener() {
            @Override
            public void loading(boolean isLoading) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }

            @Override
            public void pageChanged(String url) {
                WebViewer.this.pageChanged(url);
            }
        });
        webView.setShowTools(sharedPref.getBoolean(getString(R.string.pref_showTools), false));

        //install cache
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            try {
                File httpCacheDir = new File(getCacheDir(), "http");
                long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
                HttpResponseCache.install(httpCacheDir, httpCacheSize);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (savedInstanceState == null || !restore(savedInstanceState)) {
            loadSentUrl();
        }
    }

    //TODO only check again if timestamp has changed
    private void showNewScripts() {
        String repoHtml = webView.getRepoHtml();
        //legacy code
        // old method: if the page was changed with the previous method hash of page
        if (sharedPref.contains(getString(R.string.pref_repoHash))) {
            int newHash = Utils.pageToHash(repoHtml);
            if (newHash != -1 && sharedPref.getInt(getString(R.string.pref_repoHash), -1) != newHash && !sharedPref.contains(getString(R.string.pref_Scripts))) {
                //show the toast only if the page has changed based on the previous method and the new method is not found
                Toast.makeText(getApplicationContext(), R.string.toast_repoChanged, Toast.LENGTH_SHORT).show();
            }
            //will remove the old method
            sharedPref.edit().remove(getString(R.string.pref_repoHash)).apply();
        }

        //new method: based on the scripts found
        Map<String, String> map = Utils.getAllScriptPagesAndNames(repoHtml);
        HashMap<String, Object> temp = new HashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            temp.put(entry.getKey(), entry.getValue());
        }
        Utils.saveMapToPref(sharedPref, getString(R.string.pref_pageNames), temp);
        Set<String> currentScripts = map.keySet();
        if (sharedPref.contains(getString(R.string.pref_Scripts))) {
            Set<String> oldScripts = Utils.getSetFromPref(sharedPref, getString(R.string.pref_Scripts));
            HashSet<String> newScripts = new HashSet<>(currentScripts);
            newScripts.removeAll(oldScripts);
            if (!newScripts.isEmpty()) {
                //found new Scripts
                Utils.saveSetToPref(sharedPref, getString(R.string.pref_Scripts), currentScripts);
                ArrayList<String> newScriptNames = new ArrayList<>();
                for (String s : newScripts) newScriptNames.add(map.get(s));
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
                        Toast.makeText(WebViewer.this, (newScriptNames.size() == 1 ? getString(R.string.toast_oneNewScript) : getString(R.string.toast_severalNewScripts)) + names.toString(), Toast.LENGTH_LONG);
                        break;
                    case SHOW_DIALOG:
                        Dialogs.newScripts(this, names.toString(), newScriptNames.size() == 1);
                        break;
                }
                Toast.makeText(this, names.toString(), Toast.LENGTH_LONG).show();
            }
        } else {
            //No info about previous scripts. Only save the current scripts
            Utils.saveSetToPref(sharedPref, getString(R.string.pref_Scripts), currentScripts);
        }
    }

    private void pageChanged(String url) {
        if (url.equals(getString(R.string.link_repository))) {
            button.setVisibility(View.GONE);
            setTitle(R.string.action_mainPage);
            setSubscriptionState(CANT_SUBSCRIBE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                ActionBar bar = getActionBar();
                assert bar != null;
                bar.setDisplayHomeAsUpEnabled(false);
            }
            showNewScripts();
        } else {
            button.setVisibility(View.VISIBLE);
            setTitle(Utils.getNameForPageFromPref(sharedPref, Utils.getNameFromUrl(url)));
            boolean sub = Utils.getSetFromPref(sharedPref, getString(R.string.pref_subscriptions)).contains(url);
            setSubscriptionState(sub ? SUBSCRIBED : NOT_SUBSCRIBED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                ActionBar bar = getActionBar();
                assert bar != null;
                bar.setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    //Script importer
    @SuppressWarnings({"unused", "unusedParameter"})
    public void startImport(View ignored) {
        String currentHtml = PageCacheManager.getPage(Utils.getNameFromUrl(webView.getUrl())).html;
        //Download button clicked

        //initialize variables
        final ArrayList<String> names = new ArrayList<>();//names of all scripts
        final ArrayList<String> rawCodes = new ArrayList<>();//Found scripts
        String aboutScript;

        //Starts searching all scripts
        for (String aStart : Constants.beginning) {
            //starting found
            Utils.valueAndIndex found = new Utils.valueAndIndex(null, -1, 0);
            while (true) {
                //searches for a match
                found = Utils.findBetween(currentHtml, aStart, Constants.ending, found.to, false);
                if (found.value != null) {
                    //if it is found, it adds it to the list
                    rawCodes.add(found.value.trim());
                    //Assumes the script name is just before the code, and searches for it
                    Utils.valueAndIndex name = new Utils.valueAndIndex(null, found.from, -1);
                    while (true) {
                        name = Utils.findBetween(currentHtml, ">", "<", name.from, true);
                        if (name.value == null) {
                            names.add("Name not found");
                            break;
                        }//In theory this will never get executed ... in theory
                        if (name.value.matches(".*\\w.*")) {
                            //when it is found (if not it will return another text not related
                            names.add(name.value);
                            break;
                        }
                    }
                } else {
                    //if not found, another starting token
                    break;
                }
            }
        }

        //TODO search the flags

        //About script: purpose, author, link
        aboutScript = Utils.findBetween(currentHtml, "id=\"about_the_script\">", "</ul>", -1, false).value;
        if (aboutScript != null) {

            //remove html tags
            aboutScript = aboutScript.replaceAll("<[^>]*>", "");

            String[] prov = aboutScript.split("\n+");//separate the text removing duplicated line breaks

            //join the text adding an asterisk at the beginning of each line and converting the html string into normal code
            StringBuilder buffer = new StringBuilder();
            for (int i = 0; i < prov.length; ++i) {
                buffer.append((i == 0) ? "" : "\n *  ").append(Html.fromHtml(prov[i]).toString());
            }
            aboutScript = buffer.toString();

            //adds the beginning and end comment block, and remove extra whitespaces at the beginning and end
            aboutScript = "/* " + aboutScript.trim() + "\n */\n\n";
        }

        //switch based on the number of scripts found
        if (rawCodes.size() > 1) {
            //more than one script founds
            final String about = aboutScript;
            Dialogs.moreThanOneScriptFound(this, names.toArray(new String[names.size()]), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();//Necessary, this is launched when clicking an item, not when clicking a button
                    showImportScript(names.get(which), rawCodes.get(which), about);
                }
            });
        } else if (rawCodes.size() > 0) {
            oneScriptFound(names.get(0), rawCodes.get(0), aboutScript);
        } else {
            Dialogs.noScriptFound(this);
        }
    }

    private void oneScriptFound(String name, String rawCode, String about) {
        //only one script, load directly

        String repoHtml = webView.getRepoHtml();
        //get the name from the repository
        String url = webView.getUrl();
        url = url.substring(url.indexOf('/', "http://www".length()));
        int index = repoHtml.indexOf(url);
        String scriptName;
        if (index != -1) {
            scriptName = repoHtml.substring(repoHtml.indexOf('>', index) + 1, repoHtml.indexOf('<', index)).trim();
        } else
            //fallback if not found in repo
            scriptName = name;

        showImportScript(scriptName, rawCode, about);

    }

    private void showImportScript(String scriptName, String rawCode, String aboutString) {
        //show the alert to import a single script
        String[] lines = rawCode.split("\n");
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(Html.fromHtml(line).toString()).append("\n");//Because Html.fromHtml() removes the line breaks
        }

        String code = new String(builder).trim();
        if (sharedPref.getBoolean(getString(R.string.pref_aboutScript), true))
            code = aboutString + code;

        Dialogs.importScript(this, code, scriptName, new Dialogs.OnImportListener() {
            @Override
            public void onClick(String code, String name, int flags) {
                sendScriptToLauncher(code, name, flags);
                if (!isSubscribed())
                    Dialogs.subscribe(WebViewer.this, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            subscribeToCurrent();
                        }
                    });
            }
        }, new Dialogs.OnImportListener() {
            @Override
            public void onClick(String code, String name, int flags) {
                shareAsText(code, name, flags);
            }
        });
    }

    //Send & share functions
    private void sendScriptToLauncher(String code, String scriptName, int flags) {
        // let's import the script
        Intent intent = new Intent(this, ScriptImporter.class);
        intent.putExtra(Constants.extraCode, code);
        intent.putExtra(Constants.extraName, scriptName);
        intent.putExtra(Constants.extraFlags, flags);
        startService(intent);
    }

    private void shareAsText(String code, String scriptName, int flags) {
        //share the code as plain text

        StringBuilder text = new StringBuilder("");

        //flags
        text.append("//Flags: ");
        if (flags >= Constants.FLAG_CUSTOM_MENU) {
            text.append("app ");
            flags -= Constants.FLAG_CUSTOM_MENU;
        }
        if (flags >= Constants.FLAG_ITEM_MENU) {
            text.append("item ");
            flags -= Constants.FLAG_ITEM_MENU;
        }
        if (flags >= Constants.FLAG_APP_MENU) {
            text.append("custom ");
        }
        text.append("\n");

        //name
        text.append("//Name: ")
                .append(scriptName)
                .append("\n")
                .append(code);

        text.append("\n");

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, text.toString());
        startActivity(Intent.createChooser(share, "Send to..."));
    }

    //Subscriptions functions
    private void getChangedSubscriptions() {
        RPCManager.getChangedSubscriptions(sharedPref, new RPCManager.Listener<List<String>>() {
            @Override
            public void onResult(RPCManager.Result<List<String>> result) {
                if (result.getStatus() == RPCManager.RESULT_OK) {
                    List<String> updated = result.getResult();
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
                                Toast.makeText(WebViewer.this, pages.toString(), Toast.LENGTH_LONG).show();
                                break;
                            case SHOW_DIALOG:
                                Dialogs.changedSubscriptions(WebViewer.this, pages.toString());
                                break;
                        }
                        RPCManager.setTimestampToCurrent(sharedPref, null);
                    }
                }
            }
        });
    }

    private void subscribeToCurrent() {
        HashSet<String> subs = (HashSet<String>) Utils.getSetFromPref(sharedPref, getString(R.string.pref_subscriptions));
        subs.add(Utils.getNameFromUrl(webView.getUrl()));
        Utils.saveSetToPref(sharedPref, getString(R.string.pref_subscriptions), subs);
        Toast.makeText(this, getString(R.string.toast_subscribeSuccessful), Toast.LENGTH_SHORT).show();
        setSubscriptionState(SUBSCRIBED);
    }

    private void unsubscribeCurrent() {
        Set<String> subs = Utils.getSetFromPref(sharedPref, getString(R.string.pref_subscriptions));
        subs.remove(Utils.getNameFromUrl(webView.getUrl()));
        Utils.saveSetToPref(sharedPref, getString(R.string.pref_subscriptions), subs);
        Toast.makeText(this, getString(R.string.toast_unsubscribeSuccessful), Toast.LENGTH_SHORT).show();
        setSubscriptionState(NOT_SUBSCRIBED);

    }

    private boolean isSubscribed() {
        return Utils.getSetFromPref(sharedPref, getString(R.string.pref_subscriptions))
                .contains(Utils.getNameFromUrl(webView.getUrl()));
    }

    private static final int CANT_SUBSCRIBE = -1;
    private static final int NOT_SUBSCRIBED = 0;
    private static final int SUBSCRIBED = 1;

    private void setSubscriptionState(int state) {
        if (menu == null) return;
        boolean sub;
        boolean unsub;
        switch (state) {
            case CANT_SUBSCRIBE:
                sub = false;
                unsub = false;
                break;
            case NOT_SUBSCRIBED:
                sub = true;
                unsub = false;
                break;
            case SUBSCRIBED:
                sub = false;
                unsub = true;
                break;
            default:
                throw new IllegalArgumentException("Invalid Argument: " + state);
        }
        menu.findItem(R.id.action_subscribe).setVisible(sub);
        menu.findItem(R.id.action_unsubscribe).setVisible(unsub);

    }

    //return false to block loading
    private boolean upgradeFromOldVersion() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        if (sharedPref.contains(getString(R.string.pref_subs))) {
            Map<String, Object> map = Utils.getMapFromPref(sharedPref, getString(R.string.pref_subs));
            HashSet<String> set = new HashSet<>();
            for (String page : map.keySet()) {
                set.add(Utils.getNameFromUrl(page));
            }
            Utils.saveSetToPref(sharedPref, getString(R.string.pref_subscriptions), set);
            sharedPref.edit().remove(getString(R.string.pref_subs)).apply();
        }

        if (!sharedPref.contains(getString(R.string.pref_timestamp))) {
            RPCManager.setTimestampToCurrent(sharedPref, new RPCManager.Listener<Integer>() {
                @Override
                public void onResult(RPCManager.Result<Integer> result) {
                    if (result.getStatus() == RPCManager.RESULT_OK) {
                        init();
                    } else {
                        Dialogs.connectionFailed(WebViewer.this, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        });
                    }
                }
            });
            return false;
        }
        return true;
    }

}

