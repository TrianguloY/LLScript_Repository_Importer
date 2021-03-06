package com.trianguloy.llscript.repository.web;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
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
import com.trianguloy.llscript.repository.internal.Dialogs;
import com.trianguloy.llscript.repository.internal.ImportUtils;
import com.trianguloy.llscript.repository.internal.IntentHelper;
import com.trianguloy.llscript.repository.internal.Utils;
import com.trianguloy.llscript.repository.settings.Preferences;
import com.trianguloy.llscript.repository.settings.SettingsActivity;
import com.trianguloy.llscript.repository.settings.SubscriptionManager;

import org.acra.ACRA;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
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
    private Preferences sharedPref;//user saved data

    @NonNull
    private Boolean close = false; //if pressing back will close or not

    @NonNull
    private final SubscriptionManager subscriptionManager;

    private Bundle savedInstanceState;

    public WebViewer() {
        subscriptionManager = new SubscriptionManager();
    }

    //Application functions
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //initialize variables
        sharedPref = Preferences.getDefault(this);
        RepositoryImporter.setTheme(this, sharedPref);
        subscriptionManager.setContext(this);
        this.savedInstanceState = savedInstanceState;


        super.onCreate(savedInstanceState);

        //check for launcher to find the installed one, continue even if not found
        Utils.alertLauncherProblemsIfAny(this);

        //init views
        initializeWeb();

        if ((sharedPref.contains(R.string.pref_version) && sharedPref.getInt(R.string.pref_version, -1) == BuildConfig.VERSION_CODE) || upgradeFromOldVersion())
            init();
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
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
        if (!sentUrl.equals(getString(R.string.link_repository)) && !sharedPref.getBoolean(R.string.pref_directReturn, false) && !webView.hasPage()) {
            webView.dropOnStackWithoutShowing(getString(R.string.link_repository));
        }
        webView.show(sentUrl);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        subscriptionManager.setMenu(menu);

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_webviewer, menu);

        //Debug crash button
        if (!BuildConfig.DEBUG) {
            menu.findItem(R.id.debug).setVisible(false);
        }
        return true;
    }

    /**
     * an item in the action bar is pressed
     * <p/>
     * Handle action bar item clicks here. The action bar will
     * automatically handle clicks on the Home/Up button, so long
     * as you specify a parent activity in AndroidManifest.xml.
     *
     * @param item the pressed item
     * @return true to consume the event, false otherwise
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (webView == null) {
            //no webview, bad bad
            Toast.makeText(getApplicationContext(), R.string.toast_internalError, Toast.LENGTH_SHORT).show();
            //collect as much data as possible
            ACRA.getErrorReporter().putCustomData("MenuItem", item.getTitle().toString());
            ACRA.getErrorReporter().putCustomData("HasSharedPref", String.valueOf(sharedPref != null));
            ACRA.getErrorReporter().putCustomData("HasSavedState", String.valueOf(savedInstanceState != null));
            ACRA.getErrorReporter().putCustomData("SentUrl", sentUrl);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                ACRA.getErrorReporter().putCustomData("isDestroyed", String.valueOf(isDestroyed()));
            }
            ACRA.getErrorReporter().putCustomData("isFinishing", String.valueOf(isFinishing()));
            ACRA.getErrorReporter().handleSilentException(new Throwable("webView is null"));
            return true;
        }


        //no page-dependent buttons
        switch (item.getItemId()) {
            case R.id.action_mainPage:
                //load the main page
                webView.show(getString(R.string.link_repository));
                return true;
            case R.id.action_settings:
                //open the settings
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case android.R.id.home:
                //the button that appears at the left of the bar with an arrow
                onBackPressed();
                return true;
            case R.id.debug:
                //intended crash
                throw new RuntimeException("This crash was intended");
        }


        //check for page
        if (!webView.hasPage()) {
            //no page, error
            Toast.makeText(getApplicationContext(), R.string.toast_noPageError, Toast.LENGTH_SHORT).show();
            return true;
        }

        //page-dependent buttons
        switch (item.getItemId()) {
            case R.id.action_openInBrowser:
                //open the current page in the browser
                IntentHelper.sendToAllButSelf(this, Uri.parse(webView.getUrl()));
                return true;
            case R.id.action_subscribe: {
                //subscribe to the current page
                String page = webView.getPageId();
                if (page != null) subscriptionManager.subscribe(page);
                return true;
            }
            case R.id.action_unsubscribe: {
                //unsubscribe from the current page
                String page = webView.getPageId();
                if (page != null)
                    subscriptionManager.unsubscribe(page);
                return true;
            }
            case R.id.editor:
                //open the page in the editor
                Intent i = new Intent(this, EditorActivity.class);
                i.setAction(webView.getUrl());
                startActivity(i);
                return true;
            case R.id.action_share:
                //share the current page
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, webView.getUrl());
                startActivity(Intent.createChooser(share, getString(R.string.title_share)));
                return true;
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (webView.backPossible()) webView.performBack();
        else if (!close && !sharedPref.getBoolean(R.string.pref_singleClose, false)) {
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
        if (keyCode == KeyEvent.KEYCODE_BACK && sharedPref.getBoolean(R.string.pref_longPressClose, true)) {
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
        if (savedInstanceState == null || !restore(savedInstanceState)) {
            loadSentUrl();
        }
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
            public void pageChanged(@NonNull String url) {
                WebViewer.this.pageChanged(url);
            }

            @Override
            public void repoDocumentUpdated(@NonNull Document repoDoc) {
                Utils.showNewScriptsIfAny(WebViewer.this, repoDoc, webView);
            }
        });
        webView.setShowTools(sharedPref.getBoolean(R.string.pref_showTools, false));

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
    }

    private void pageChanged(@NonNull String url) {
        if (url.equals(getString(R.string.link_repository))) {
            button.setVisibility(View.GONE);
            setTitle(R.string.menu_mainPage);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                ActionBar bar = getActionBar();
                assert bar != null;
                bar.setDisplayHomeAsUpEnabled(false);
            }
        } else {
            button.setVisibility(View.VISIBLE);
            setTitle(Utils.getNameForPage(this, Utils.getIdFromUrl(url)));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                ActionBar bar = getActionBar();
                assert bar != null;
                bar.setDisplayHomeAsUpEnabled(true);
            }
        }
        subscriptionManager.updateState(Utils.getIdFromUrl(url));
    }

    //Script importer
    @SuppressWarnings({"unused", "unusedParameter"})
    public void startImport(View ignored) {
        ImportUtils.startImport(this, webView, new ImportUtils.Listener() {
            @Override
            public void importFinished() {
                String page = webView.getPageId();
                if (page != null && !subscriptionManager.isSubscribed(page)) {
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
        Preferences.Editor editor = sharedPref.edit();
        boolean result = true;
        editor.putInt(R.string.pref_version, BuildConfig.VERSION_CODE);
        if (sharedPref.contains(R.string.pref_reportMode)) {
            int reportMode = Integer.valueOf(sharedPref.getString(R.string.pref_reportMode, "0"));
            boolean enable = reportMode != 1;
            boolean silent = reportMode == 2;
            editor.putBoolean(R.string.pref_enableAcra, enable).putBoolean(R.string.pref_alwaysSendReports, silent);
            editor.remove(R.string.pref_reportMode);
        }

        if (sharedPref.contains(R.string.pref_subs)) {
            Map<String, String> map = sharedPref.getStringMap(R.string.pref_subs, Collections.<String, String>emptyMap());
            HashSet<String> set = new HashSet<>();
            for (String page : map.keySet()) {
                set.add(Utils.getIdFromUrl(page));
            }
            editor.putStringSet(R.string.pref_subscriptions, set).remove(R.string.pref_subs);
        }
        if (sharedPref.contains(R.string.pref_repoHash)) {
            editor.remove(R.string.pref_repoHash);
        }
        if (!sharedPref.contains(R.string.pref_timestamp)) {
            new RPCManager(this).setTimestampToCurrent(sharedPref, new RPCManager.Listener<Integer>() {
                @Override
                public void onResult(@NonNull RPCManager.Result<Integer> result) {
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
            result = false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Dialogs.explainSystemWindowPermission(this, new DialogInterface.OnClickListener() {
                @TargetApi(Build.VERSION_CODES.M)
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
                    init();
                }
            });
            result = false;
        }
        editor.apply();
        return result;
    }

}

