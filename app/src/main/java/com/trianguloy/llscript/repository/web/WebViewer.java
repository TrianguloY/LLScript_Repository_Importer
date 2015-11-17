package com.trianguloy.llscript.repository.web;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.http.HttpResponseCache;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.trianguloy.llscript.repository.BuildConfig;
import com.trianguloy.llscript.repository.Constants;
import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.RepositoryImporter;
import com.trianguloy.llscript.repository.editor.EditorActivity;
import com.trianguloy.llscript.repository.internal.AppChooser;
import com.trianguloy.llscript.repository.internal.Dialogs;
import com.trianguloy.llscript.repository.internal.ImportUtils;
import com.trianguloy.llscript.repository.internal.Utils;
import com.trianguloy.llscript.repository.settings.SettingsActivity;
import com.trianguloy.llscript.repository.settings.SubscriptionManager;

import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Main activity: displays the webView used to load scripts
 */

public class WebViewer extends Activity {

    //Elements
    private ManagedWebView webView; //webView element
    private Button button; //button element

    private String sentUrl;

    //User vars
    private SharedPreferences sharedPref;//user saved data

    private Boolean close = false; //if pressing back will close or not

    private final SubscriptionManager subscriptionManager;

    private Bundle savedInstanceState;

    public WebViewer() {
        subscriptionManager = new SubscriptionManager();
    }

    //Application functions
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //initialize variables
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        RepositoryImporter.setTheme(this, sharedPref);
        subscriptionManager.setContext(this);
        this.savedInstanceState = savedInstanceState;


        super.onCreate(savedInstanceState);

        //check for launcher to find the installed one, continue even if not found
        Utils.checkForLauncher(this);

        if ((sharedPref.contains(getString(R.string.key_version)) && sharedPref.getInt(getString(R.string.key_version),-1) == BuildConfig.VERSION_CODE) || upgradeFromOldVersion()) init();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Utils.showChangedSubscriptionsIfAny(this, webView);

        if (intent.hasExtra(Constants.EXTRA_OPEN_URL)
                &&//if has both extras
                intent.hasExtra(Constants.EXTRA_OPEN_URL_TIME)
                &&//and if the time passed is less than five seconds (to avoid be launched after closed, because the intent is kept)
                intent.getLongExtra(Constants.EXTRA_OPEN_URL_TIME, 0) + Constants.FIVE_SECONDS > System.currentTimeMillis()
                ) {
            sentUrl = intent.getStringExtra(Constants.EXTRA_OPEN_URL);
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
        subscriptionManager.setMenu(menu);

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_webviewer, menu);

        //Debug crash button
        if (!BuildConfig.DEBUG) {
            menu.findItem(R.id.debug).setVisible(false);
        }
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
                subscriptionManager.subscribe(webView.getPageId());
                break;
            case R.id.action_unsubscribe:
                subscriptionManager.unsubscribe(webView.getPageId());
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
            case R.id.debug:
                throw new RuntimeException("This crash was intended");
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
            }, Constants.TWO_SECONDS);//2000ms is the default time for the TOAST_LENGTH_SHORT
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
        //Main Activity. Run on onCreate->init when normal launch
        setContentView(R.layout.activity_webviewer);

        //initialize vars
        button = (Button) findViewById(R.id.button);
        webView = (ManagedWebView) findViewById(R.id.webView);
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        webView.setListener(new ManagedWebView.Listener() {
            @Override
            public void loading(boolean isLoading) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }

            @Override
            public void pageChanged(String url) {
                WebViewer.this.pageChanged(url);
            }

            @Override
            public void repoDocumentUpdated(Document repoDoc) {
                Utils.showNewScriptsIfAny(WebViewer.this, repoDoc, webView);
            }
        });
        webView.setShowTools(sharedPref.getBoolean(getString(R.string.pref_showTools), false));

        //install cache
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            try {
                File httpCacheDir = new File(getCacheDir(), "http");
                long httpCacheSize = Constants.TEN_MEGABYTE;
                HttpResponseCache.install(httpCacheDir, httpCacheSize);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (savedInstanceState == null || !restore(savedInstanceState)) {
            loadSentUrl();
        }
    }

    private void pageChanged(String url) {
        if (url.equals(getString(R.string.link_repository))) {
            button.setVisibility(View.GONE);
            setTitle(R.string.action_mainPage);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                ActionBar bar = getActionBar();
                assert bar != null;
                bar.setDisplayHomeAsUpEnabled(false);
            }
        } else {
            button.setVisibility(View.VISIBLE);
            setTitle(Utils.getNameForPageFromPref(sharedPref, Utils.getNameFromUrl(url)));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                ActionBar bar = getActionBar();
                assert bar != null;
                bar.setDisplayHomeAsUpEnabled(true);
            }
        }
        subscriptionManager.updateState(Utils.getNameFromUrl(url));
    }

    //Script importer
    @SuppressWarnings({"unused", "unusedParameter"})
    public void startImport(View ignored) {
        ImportUtils.startImport(this, webView, new ImportUtils.Listener() {
            @Override
            public void importFinished() {
                if (!subscriptionManager.isSubscribed(webView.getPageId())) {
                    Dialogs.subscribe(WebViewer.this, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            subscriptionManager.subscribe(webView.getPageId());
                        }
                    });
                }
            }
        });
    }

    //return false to block loading. If blocked has to call init() when finished
    private boolean upgradeFromOldVersion() {
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        sharedPref.edit().putInt(getString(R.string.key_version), BuildConfig.VERSION_CODE).apply();
        if (!BuildConfig.DEBUG)
            sharedPref.edit().putBoolean(getString(R.string.pref_enableAcra), true).apply();

        if (sharedPref.contains(getString(R.string.pref_subs))) {
            Map<String, Object> map = Utils.getMapFromPref(sharedPref, getString(R.string.pref_subs));
            HashSet<String> set = new HashSet<>();
            for (String page : map.keySet()) {
                set.add(Utils.getNameFromUrl(page));
            }
            Utils.saveSetToPref(sharedPref, getString(R.string.pref_subscriptions), set);
            sharedPref.edit().remove(getString(R.string.pref_subs)).apply();
        }
        if (sharedPref.contains(getString(R.string.pref_repoHash))) {
            sharedPref.edit().remove(getString(R.string.pref_repoHash)).apply();
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
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.SYSTEM_ALERT_WINDOW) != PackageManager.PERMISSION_GRANTED){
            Dialogs.explainSystemWindowPermission(this, new DialogInterface.OnClickListener() {
                @TargetApi(Build.VERSION_CODES.M)
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
                    init();
                }
            });
            return false;
        }
        return true;
    }

}

