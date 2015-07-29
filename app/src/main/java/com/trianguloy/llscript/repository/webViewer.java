package com.trianguloy.llscript.repository;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.http.HttpResponseCache;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.trianguloy.llscript.repository.internal.AppChooser;
import com.trianguloy.llscript.repository.internal.Dialogs;
import com.trianguloy.llscript.repository.internal.DownloadTask;
import com.trianguloy.llscript.repository.internal.ServiceManager;
import com.trianguloy.llscript.repository.internal.Utils;
import com.trianguloy.llscript.repository.internal.WebClient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Main activity: displays the webView used to load scripts
 */

public class webViewer extends Activity {

    private static final int SHOW_NONE = 0;
    private static final int SHOW_TOAST = 1;
    private static final int SHOW_DIALOG = 2;

    //Elements
    private WebView webView; //webView element
    private Button button; //button element
    private ProgressBar progressBar;

    //User vars
    private SharedPreferences sharedPref;//user saved data

    //Callbacks
    private Boolean close = false; //if pressing back will close or not

    //Web view data
    private String repoHtml = "";//source code of the repository, used to get the name of the scripts
    private String currentHtml = "";//source code of the current page
    private String currentUrl = "";//The URL of the current page
    private DownloadTask.Listener downloadTaskListener = null; //default downloadTaskListener

    private Stack<backClass> backStack;//contains the history of the views pages
    private int webViewPositionY = 0;//Contains the positionY that will be applied when the webView finish loading a page
    private Menu menu;

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
        if (savedInstanceState != null && restore(savedInstanceState)) {
            initializeWeb();
        } else {
            backStack = new Stack<>();
            currentUrl = getString(R.string.link_repository);

            //parse the Intent
            onNewIntent(getIntent());


            if (sharedPref.contains(Constants.keyId)) {
                //To move from the previous version to the new one, to remove on next releases
                int id = sharedPref.getInt(Constants.keyId, -1);
                sharedPref.edit().remove(Constants.keyId).apply();

                if (id != -1) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setComponent(new ComponentName(Constants.installedPackage, Constants.activityRunScript));
                    intent.putExtra(Constants.RunActionExtra, Constants.RunActionKey);
                    intent.putExtra(Constants.RunDataExtra, "" + id);
                    startActivity(intent);
                    finish();
                    return;
                }

            }

            //Normal activity
            initializeWeb();
            getChangedSubscriptions();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {

        if (intent.hasExtra(Constants.extraOpenUrl)
                &&//if has both extras
                intent.hasExtra(Constants.extraOpenUrlTime)
                &&//and if the time passed is less than five seconds (to avoid be launched after closed, because the intent is kept)
                intent.getLongExtra(Constants.extraOpenUrlTime, 0) + 5000 > System.currentTimeMillis()
                ) {
            boolean invalidate = false;
            if (intent.getBooleanExtra(Constants.extraReload, false)) {
                repoHtml = "";
                invalidate = true;
            }
            changePage(intent.getStringExtra(Constants.extraOpenUrl), 0, invalidate);
        }


        super.onNewIntent(intent);
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
                changePage(getString(R.string.link_repository));
                break;
            case R.id.action_openInBrowser:
                new AppChooser(this, Uri.parse(currentUrl), getString(R.string.title_appChooserNormal), getString(R.string.message_noBrowser), null).show();
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
                startActivity(new Intent(this, EditorActivity.class));
                break;
            case R.id.action_share:
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, currentUrl);
                startActivity(Intent.createChooser(share, getString(R.string.title_share)));
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        if (!currentUrl.equals(getString(R.string.link_repository)) && (!sharedPref.getBoolean(getString(R.string.pref_directReturn),false) || !backStack.empty())) {
            //not on the home page
            if (backStack.empty()) {
                changePage(getString(R.string.link_repository));
            } else {
                backClass previous = backStack.pop();
                currentUrl = previous.url;
                changePage(currentUrl, previous.posY);
            }

        } else if (!close && !sharedPref.getBoolean(getString(R.string.pref_singleClose),false)) {
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
        if (keyCode == KeyEvent.KEYCODE_BACK && sharedPref.getBoolean(getString(R.string.pref_longPressClose),true)) {
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
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        ArrayList<String> temp = new ArrayList<>(backStack.size());
        for (backClass b : backStack) {
            temp.add(Utils.backClassToString(b));
        }
        outState.putStringArrayList(getString(R.string.key_backStack), temp);
        outState.putString(getString(R.string.key_currentUrl), currentUrl);
        outState.putString(getString(R.string.key_currentHtml), currentHtml);
        outState.putString(getString(R.string.key_repoHtml), repoHtml);
    }

    private boolean restore(@NonNull Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(getString(R.string.key_backStack))) {
            backStack = new Stack<>();
            ArrayList<String> temp = savedInstanceState.getStringArrayList(getString(R.string.key_backStack));
            for (String s : temp) {
                backStack.add(Utils.stringToBackClass(s));
            }
        }
        if (savedInstanceState.containsKey(getString(R.string.key_currentUrl)))
            currentUrl = savedInstanceState.getString(getString(R.string.key_currentUrl));
        if (savedInstanceState.containsKey(getString(R.string.key_currentHtml)))
            currentHtml = savedInstanceState.getString(getString(R.string.key_currentHtml));
        if (savedInstanceState.containsKey(getString(R.string.key_repoHtml)))
            repoHtml = savedInstanceState.getString(getString(R.string.key_repoHtml));
        return backStack != null && currentUrl != null && currentHtml != null && repoHtml != null;
    }


    private void initializeWeb() {
        //Main Activity. Run on onCreate when normal launch
        setContentView(R.layout.activity_webviewer);

        //initialize vars
        button = (Button) findViewById(R.id.button);
        webView = (WebView) findViewById(R.id.webView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        downloadTaskListener = new DownloadTask.Listener() {
            @Override
            public void onFinish(String result) {
                //default listener: show the page after loading it
                if (sharedPref.getBoolean(getString(R.string.pref_showTools), false)) {
                    currentHtml = result;
                } else {
                    //remove tools
                    Utils.valueAndIndex val = Utils.findBetween(result, "<div class=\"tools group\">", "<hr class=\"a11y\" />", 0, false);
                    currentHtml = result.substring(0, val.from) + result.substring(val.to, result.length());
                }
                if (currentUrl.equals(getString(R.string.link_repository))) {
                    repoHtml = currentHtml;

                    //Function to check if the page has changed since the last visit
                    showNewScripts();
                }
                progressBar.setVisibility(View.GONE);
                if (menu != null) onPrepareOptionsMenu(menu);
                display();
            }

            @Override
            public void onError() {
                progressBar.setVisibility(View.GONE);
                Dialogs.noPageLoaded(webViewer.this, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        progressBar.setVisibility(View.VISIBLE);
                        new DownloadTask(downloadTaskListener).execute(currentUrl);
                    }
                });
            }
        };

        webView.setWebViewClient(new WebClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                //prevent login and register, broken because cookies are missing
                if (url.contains("&do=login") || url.contains("&do=register")) return true;
                if (!currentUrl.equals(url)) {
                    //link clicked
                    changePage(url);
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                webView.scrollTo(0, webViewPositionY);
            }
        });
        webView.getSettings().setJavaScriptEnabled(true);

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

        changePage(currentUrl, 0, true);
    }

    private void showNewScripts() {
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
                int showAs = sharedPref.getInt(getString(R.string.pref_newScripts), 1);
                switch (showAs) {
                    case SHOW_NONE:
                        break;
                    case SHOW_TOAST:
                        Toast.makeText(webViewer.this, (newScriptNames.size() == 1 ? getString(R.string.toast_oneNewScript) : getString(R.string.toast_severalNewScripts)) + names.toString(), Toast.LENGTH_LONG);
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

    //webView functions
    private void changePage(String url) {
        changePage(url, 0);
    }

    private void changePage(String url, int positionY) {
        changePage(url, positionY, false);
    }

    private void changePage(String url, int positionY, boolean invalidate) {
        //Change the page of the webView to the passed one
        if (downloadTaskListener == null) {
            //The activity is not yet loaded. The url is kept as the currentUrl, so it gets loaded when the activity do so
            currentUrl = url;
            return;
        }

        if (url.equals(getString(R.string.link_repository))) {
            //main page
            currentUrl = url;
            backStack.clear();
            webViewPositionY = positionY;
            if (repoHtml.equals("")) {
                new DownloadTask(downloadTaskListener).execute(url);
            } else {
                progressBar.setVisibility(View.GONE);
                currentHtml = repoHtml;
                display();
            }
        } else if (url.startsWith(getString(R.string.link_scriptPagePrefix))) {
            // script page
            if (!currentUrl.equals(url) || invalidate) {
                if (!currentUrl.equals(url))
                    backStack.push(new backClass(currentUrl, webView.getScrollY()));
                currentUrl = url;
                webViewPositionY = positionY;
                progressBar.setVisibility(View.VISIBLE);
                new DownloadTask(downloadTaskListener).execute(url);
            } else {
                progressBar.setVisibility(View.GONE);
                if (menu != null) onPrepareOptionsMenu(menu);
                display();
            }
        } else
            //external page
            showExternalPageLinkClicked(url);
    }

    private void display() {
        //display a page
        webView.loadDataWithBaseURL(getString(R.string.link_server), currentHtml, "text/html", "utf-8", null);

        //sets the visibility of the button and the title of the app
        if (currentUrl.equals(getString(R.string.link_repository))) {
            button.setVisibility(View.GONE);
            setTitle(R.string.action_mainPage);
            setSubscriptionState(CANT_SUBSCRIBE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                getActionBar().setDisplayHomeAsUpEnabled(false);
        } else {
            button.setVisibility(View.VISIBLE);
            setTitle(Utils.getNameForPageFromPref(sharedPref, this, Utils.getNameFromUrl(currentUrl)));
            boolean sub = Utils.getMapFromPref(sharedPref, getString(R.string.pref_subs)).containsKey(currentUrl);
            setSubscriptionState(sub ? SUBSCRIBED : NOT_SUBSCRIBED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void showExternalPageLinkClicked(final String url) {
        //When the clicked page is not useful for this app
        new AppChooser(this, Uri.parse(url), getString(R.string.title_appChooserExternalClicked), getString(R.string.message_noBrowser), null).show();
    }

    //Script importer
    @SuppressWarnings({"unused", "unusedParameter"})
    public void startImport(View ignored) {
        //Download button clicked

        //initialize variables
        final ArrayList<String> names = new ArrayList<>();//names of all scripts
        final ArrayList<String> rawCodes = new ArrayList<>();//Found scripts
        String aboutScript;

        //Starts searching all scripts
        for (String aStart : Constants.beginning) {
            //starting found
            Utils.valueAndIndex found = new Utils.valueAndIndex(null, -1, 0);
            do {
                //searches for a match
                found = Utils.findBetween(currentHtml, aStart, Constants.ending, found.to, false);
                if (found.value != null) {
                    //if it is found, it adds it to the list
                    rawCodes.add(found.value.trim());
                    //Assumes the script name is just before the code, and searches for it
                    Utils.valueAndIndex name = new Utils.valueAndIndex(null, found.from, -1);
                    do {
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
                    } while (true);

                } else {
                    //if not found, another starting token
                    break;
                }
            } while (true);
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

        //get the name from the repository
        String url = currentUrl;
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
                if (!Utils.getMapFromPref(sharedPref, getString(R.string.pref_subs)).keySet().contains(currentUrl))
                    Dialogs.subscribe(webViewer.this, new DialogInterface.OnClickListener() {
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
        final Context context = this;
        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                WebService.LocalBinder binder = (WebService.LocalBinder) service;
                binder.getService().getChangedSubscriptions(new WebService.Listener() {
                    @Override
                    public void onFinish(List<String> updated) {
                        Map<String, String> map = Utils.getAllScriptPagesAndNames(repoHtml);
                        StringBuilder pages = new StringBuilder();
                        for (String s : updated) {
                            pages.append(map.get(Utils.getNameFromUrl(s))).append("\n");
                        }
                        int showAs = sharedPref.getInt(getString(R.string.pref_changedSubs), 2);
                        switch (showAs) {
                            case SHOW_NONE:
                                break;
                            case SHOW_TOAST:
                                Toast.makeText(webViewer.this, pages.toString(), Toast.LENGTH_LONG).show();
                                break;
                            case SHOW_DIALOG:
                                Dialogs.changedSubscriptions(webViewer.this, pages.toString());
                                break;
                        }
                    }

                    @Override
                    public void onError() {
                        if (BuildConfig.DEBUG) Log.i("Subscriptions", "Ignored Error");
                    }
                });
                ServiceManager.unbindService(context, this);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };
        ServiceManager.bindService(this, connection);
    }

    private void subscribeToCurrent() {
        Map<String, Object> subs = Utils.getMapFromPref(sharedPref, getString(R.string.pref_subs));
        subs.put(currentUrl, Utils.pageToHash(currentHtml));
        Utils.saveMapToPref(sharedPref, getString(R.string.pref_subs), subs);
        Toast.makeText(this, getString(R.string.toast_subscribeSuccessful), Toast.LENGTH_SHORT).show();
        setSubscriptionState(SUBSCRIBED);
    }

    private void unsubscribeCurrent() {
        Map<String, Object> subs = Utils.getMapFromPref(sharedPref, getString(R.string.pref_subs));
        subs.remove(currentUrl);
        Utils.saveMapToPref(sharedPref, getString(R.string.pref_subs), subs);
        Toast.makeText(this, getString(R.string.toast_unsubscribeSuccessful), Toast.LENGTH_SHORT).show();
        setSubscriptionState(NOT_SUBSCRIBED);

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


    public static class backClass {
        public final String url;
        public final int posY;

        public backClass(String u, int p) {
            url = u;
            posY = p;
        }
    }


}
