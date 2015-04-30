package com.trianguloy.llscript.repository;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.HttpResponseCache;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.trianguloy.llscript.repository.internal.AppChooser;
import com.trianguloy.llscript.repository.internal.DownloadTask;
import com.trianguloy.llscript.repository.internal.ServiceManager;
import com.trianguloy.llscript.repository.internal.StringFunctions;
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

        if (checkForLauncher()) {
            return;
        }

        //initialize variables
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
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

    @Override
    protected void onNewIntent(Intent intent) {

        if (
                intent.hasExtra(Constants.extraOpenUrl)
                        &&//if has both extras
                        intent.hasExtra(Constants.extraOpenUrlTime)
                        &&//and if the time passed is less than five seconds (to avoid be launched after closed, because the intent is kept)
                        intent.getLongExtra(Constants.extraOpenUrlTime, 0) + 5000 > System.currentTimeMillis()
                ) {
            changePage(intent.getStringExtra(Constants.extraOpenUrl));
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
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        if (!currentUrl.equals(getString(R.string.link_repository))) {
            //not on the home page
            if (backStack.empty()) {
                changePage(getString(R.string.link_repository));
            } else {
                backClass previous = backStack.pop();
                currentUrl = previous.url;
                changePage(currentUrl, previous.posY);
            }

        } else if (!close) {
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
        if (keyCode == KeyEvent.KEYCODE_BACK) {
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


    //Initialization
    private boolean checkForLauncher() {

        //checks the installed package, extreme or not
        PackageManager pm = getPackageManager();
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
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(getString(R.string.title_launcherNotFound))
                    .setMessage(getString(R.string.message_launcherNotFound))
                    .setNeutralButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(getString(R.string.link_playStorePrefix) + Constants.packages[1]));
                            startActivity(i);
                            finish();
                        }
                    })
                    .setIcon(R.drawable.ic_launcher)
                    .show();
            return true;
        }


        //Checks the version of the launcher

        if ((pi.versionCode % 1000) < Constants.minimumNecessaryVersion) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(getString(R.string.title_outdatedLauncher))
                    .setMessage(getString(R.string.message_outdatedLauncher))
                    .setNeutralButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(getString(R.string.link_playStorePrefix) + Constants.installedPackage));
                            startActivity(i);
                            finish();
                        }
                    })
                    .setIcon(R.drawable.ic_launcher)
                    .show();
            return true;
        }


        return false;
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
                    StringFunctions.valueAndIndex val = StringFunctions.findBetween(result, "<div class=\"tools group\">", "<hr class=\"a11y\" />", 0, false);
                    currentHtml = result.substring(0, val.from) + result.substring(val.to, result.length());
                }
                //open spoilers, as the cannot be opened without javascript enabled (which would be a security issue)
                //currentHtml = currentHtml.replace("display: none","display");
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
                showNoPageLoaded(currentUrl);
            }
        };

        webView.setWebViewClient(new WebClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                //prevent login and register, broken because cookies are missing
                if(url.contains("&do=login")||url.contains("&do=register"))return true;
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

        changePage(currentUrl);
    }

    private void showNewScripts() {
        //legacy code
        // old method: if the page was changed with the previous method hash of page
        if (sharedPref.contains(getString(R.string.pref_repoHash))) {
            int newHash = StringFunctions.pageToHash(repoHtml);
            if (newHash != -1 && sharedPref.getInt(getString(R.string.pref_repoHash), -1) != newHash && !sharedPref.contains(getString(R.string.pref_Scripts))) {
                //show the toast only if the page has changed based on the previous method and the new method is not found
                Toast.makeText(getApplicationContext(), R.string.toast_repoChanged, Toast.LENGTH_SHORT).show();
            }
            //will remove the old method
            sharedPref.edit().remove(getString(R.string.pref_repoHash)).apply();
        }

        //new method: based on the scripts found
        Map<String, String> map = StringFunctions.getAllScriptPagesAndNames(repoHtml);
        HashMap<String, Object> temp = new HashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            temp.put(entry.getKey(), entry.getValue());
        }
        StringFunctions.saveMapToPref(sharedPref, getString(R.string.pref_pageNames), temp);
        Set<String> currentScripts = map.keySet();
        if (sharedPref.contains(getString(R.string.pref_Scripts))) {
            Set<String> oldScripts = StringFunctions.getSetFromPref(sharedPref, getString(R.string.pref_Scripts));
            HashSet<String> newScripts = new HashSet<>(currentScripts);
            newScripts.removeAll(oldScripts);
            if (!newScripts.isEmpty()) {
                //found new Scripts
                StringFunctions.saveSetToPref(sharedPref, getString(R.string.pref_Scripts), currentScripts);
                ArrayList<String> newScriptNames = new ArrayList<>();
                for (String s : newScripts) newScriptNames.add(map.get(s));
                StringBuilder names = new StringBuilder();
                if (newScriptNames.size() == 1){
                    names.append(getString(R.string.toast_oneNewScript)).append("\n").append(newScriptNames.get(0));
                }
                else {
                    names.append(getString(R.string.toast_severalNewScripts));
                    for (int i = 0; i < newScriptNames.size(); i++)
                        names.append("\n").append(newScriptNames.get(i));
                }
                Toast.makeText(this,  names.toString(), Toast.LENGTH_LONG).show();
            }
        } else {
            //No info about previous scripts. Only save the current scripts
            StringFunctions.saveSetToPref(sharedPref, getString(R.string.pref_Scripts), currentScripts);
        }
    }

    //webView functions
    private void changePage(String url) {
        changePage(url, 0);
    }

    private void changePage(String url, int positionY) {
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
                currentHtml = repoHtml;
                display();
            }
        } else if (url.startsWith(getString(R.string.link_scriptPagePrefix))) {
            // script page
            if (!currentUrl.equals(url)) {
                backStack.push(new backClass(currentUrl, webView.getScrollY()));
            }
            currentUrl = url;
            webViewPositionY = positionY;
            progressBar.setVisibility(View.VISIBLE);
            new DownloadTask(downloadTaskListener).execute(url);
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) getActionBar().setDisplayHomeAsUpEnabled(false);
        } else {
            button.setVisibility(View.VISIBLE);
            setTitle(StringFunctions.getNameForPageFromPref(sharedPref, this, StringFunctions.getNameFromUrl(currentUrl)));
            boolean sub = StringFunctions.getMapFromPref(sharedPref, getString(R.string.pref_subs)).containsKey(currentUrl);
            setSubscriptionState(sub ? SUBSCRIBED : NOT_SUBSCRIBED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void showExternalPageLinkClicked(final String url) {
        //When the clicked page is not useful for this app
        new AppChooser(this, Uri.parse(url), getString(R.string.title_appChooserExternalClicked), getString(R.string.message_noBrowser), null).show();
    }

    private void showNoPageLoaded(final String url) {
        //When the page couldn't be loaded
        progressBar.setVisibility(View.GONE);
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_noPageFound)
                .setMessage(R.string.message_noPageFound)
                .setPositiveButton(R.string.button_retry, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        progressBar.setVisibility(View.VISIBLE);
                        new DownloadTask(downloadTaskListener).execute(url);
                    }
                })
                .setNegativeButton(R.string.button_exit, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setIcon(R.drawable.ic_launcher)
                .setCancelable(false)
                .show();
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
            StringFunctions.valueAndIndex found = new StringFunctions.valueAndIndex(null, -1, 0);
            do {
                //searches for a match
                found = StringFunctions.findBetween(currentHtml, aStart, Constants.ending, found.to, false);
                if (found.value != null) {
                    //if it is found, it adds it to the list
                    rawCodes.add(found.value.trim());
                    //Assumes the script name is just before the code, and searches for it
                    StringFunctions.valueAndIndex name = new StringFunctions.valueAndIndex(null, found.from, -1);
                    do {
                        name = StringFunctions.findBetween(currentHtml, ">", "<", name.from, true);
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
        aboutScript = StringFunctions.findBetween(currentHtml, "id=\"about_the_script\">", "</ul>", -1, false).value;
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
            showMoreThanOneScriptFound(names.toArray(new String[names.size()]), rawCodes.toArray(new String[rawCodes.size()]), aboutScript);
        } else if (rawCodes.size() > 0) {
            oneScriptFound(names.get(0), rawCodes.get(0), aboutScript);
        } else {
            showNoScriptFound();
        }
    }

    private void oneScriptFound(String name, String rawCode, String about) {
        //only one script, load directly

        //get the name from the repository
        String url = currentUrl;
        Log.d("tag",url);
        url = url.substring(url.indexOf("/", "http://www".length()));
        int index = repoHtml.indexOf(url);
        String scriptName;
        if (index != -1) {
            scriptName = repoHtml.substring(repoHtml.indexOf(">", index) + 1, repoHtml.indexOf("<", index)).trim();
        } else
            //fallback if not found in repo
            scriptName = name;

        showImportScript(scriptName, rawCode, about);

    }

    private void showMoreThanOneScriptFound(final String[] names, final String[] rawCodes, final String about) {
        //More than one script found select one of them to import
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_severalScriptsFound)
                .setIcon(R.drawable.ic_launcher)
                .setSingleChoiceItems(names, android.R.layout.simple_list_item_single_choice, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();//Necessary, this is launched when clicking an item, not when clicking a button
                        showImportScript(names[which], rawCodes[which], about);
                    }
                })
                .setNegativeButton(R.string.button_cancel, null)
                .show();
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

        //the alert dialog
        View layout = getLayoutInflater().inflate(R.layout.confirm_alert, (ViewGroup) findViewById(R.id.webView).getRootView(), false);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) layout.setBackgroundColor(Color.WHITE);
        final EditText contentText = ((EditText) layout.findViewById(R.id.editText2));
        contentText.setText(code);
        final EditText nameText = ((EditText) layout.findViewById(R.id.editText));
        nameText.setText(scriptName);
        final CheckBox[] flagsBoxes = {
                (CheckBox) layout.findViewById(R.id.checkBox1),
                (CheckBox) layout.findViewById(R.id.checkBox2),
                (CheckBox) layout.findViewById(R.id.checkBox3)};

        new AlertDialog.Builder(this)
                .setTitle(R.string.title_importer)
                .setIcon(R.drawable.ic_launcher)
                .setView(layout)
                .setPositiveButton(R.string.button_import, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        sendScriptToLauncher(contentText, nameText, flagsBoxes);
                        if (!StringFunctions.getMapFromPref(sharedPref, getString(R.string.pref_subs)).keySet().contains(currentUrl))
                            showSubscribe();
                    }
                })
                .setNeutralButton(R.string.button_share, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        shareAsText(contentText, nameText, flagsBoxes);
                    }
                })
                .setNegativeButton(R.string.button_exit, null)
                .show();
    }

    private void showSubscribe() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_subscribe))
                .setMessage(getString(R.string.message_subscribeAsk))
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        subscribeToCurrent();
                    }
                })
                .show();
    }

    private void showNoScriptFound() {
        //alert to show that no script is found
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_importer)
                .setNegativeButton(R.string.button_exit, null)/* new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {}
                })*/
                .setPositiveButton(R.string.text_googlePlus, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent j = new Intent(Intent.ACTION_VIEW);
                        j.setData(Uri.parse(getString(R.string.link_playStoreImporter)));
                        startActivity(j);
                    }
                })
                .setIcon(R.drawable.ic_launcher)
                .setMessage(R.string.message_noScriptFound)
                .show();
    }


    //Send & share functions
    private void sendScriptToLauncher(EditText contentText, EditText nameText, CheckBox[] flagsBoxes) {
        // let's import the script
        final String code = contentText.getText().toString();
        final String scriptName = nameText.getText().toString();
        final int flags = (flagsBoxes[0].isChecked() ? Constants.FLAG_APP_MENU : 0) +
                (flagsBoxes[1].isChecked() ? Constants.FLAG_ITEM_MENU : 0) +
                (flagsBoxes[2].isChecked() ? Constants.FLAG_CUSTOM_MENU : 0);
        Intent intent = new Intent(this, ScriptImporter.class);
        intent.putExtra(Constants.extraCode, code);
        intent.putExtra(Constants.extraName, scriptName);
        intent.putExtra(Constants.extraFlags, flags);
        startService(intent);
    }

    private void shareAsText(EditText contentText, EditText nameText, CheckBox[] flagsBoxes) {
        //share the code as plain text

        StringBuilder text = new StringBuilder("");

        //flags
        text.append("//Flags: ");
        if (flagsBoxes[0].isChecked()) text.append("app ");
        if (flagsBoxes[1].isChecked()) text.append("item ");
        if (flagsBoxes[2].isChecked()) text.append("custom ");
        text.append("\n");

        //name
        text.append("//Name: ")
                .append(nameText.getText())
                .append("\n")
                .append(contentText.getText());

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
                        showChangedSubscriptions(updated);
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

    private void showChangedSubscriptions(List<String> updatedPages) {
        Map<String, String> map = StringFunctions.getAllScriptPagesAndNames(repoHtml);
        StringBuffer pages = new StringBuffer();
        for (String s : updatedPages) {
            pages.append(map.get(StringFunctions.getNameFromUrl(s))).append("\n");
        }
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_updatedSubs))
                .setMessage(pages)
                .setNeutralButton(R.string.button_ok, null)
                .show();
    }

    private void subscribeToCurrent() {
        Map<String, Object> subs = StringFunctions.getMapFromPref(sharedPref, getString(R.string.pref_subs));
        subs.put(currentUrl, StringFunctions.pageToHash(currentHtml));
        StringFunctions.saveMapToPref(sharedPref, getString(R.string.pref_subs), subs);
        Toast.makeText(this, getString(R.string.toast_subscribeSuccessful), Toast.LENGTH_SHORT).show();
        setSubscriptionState(SUBSCRIBED);
    }

    private void unsubscribeCurrent() {
        Map<String, Object> subs = StringFunctions.getMapFromPref(sharedPref, getString(R.string.pref_subs));
        subs.remove(currentUrl);
        StringFunctions.saveMapToPref(sharedPref, getString(R.string.pref_subs), subs);
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
                throw new IllegalArgumentException("Invalid Argument: "+state);
        }
        menu.findItem(R.id.action_subscribe).setVisible(sub);
        menu.findItem(R.id.action_unsubscribe).setVisible(unsub);

    }


    private static class backClass {
        final String url;
        final int posY;

        backClass(String u, int p) {
            url = u;
            posY = p;
        }
    }



}
