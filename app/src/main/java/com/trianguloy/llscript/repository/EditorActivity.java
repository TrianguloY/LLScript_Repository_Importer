package com.trianguloy.llscript.repository;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.trianguloy.llscript.repository.internal.CategoryAdapter;
import com.trianguloy.llscript.repository.internal.Dialogs;
import com.trianguloy.llscript.repository.internal.DownloadTask;
import com.trianguloy.llscript.repository.internal.Repository;
import com.trianguloy.llscript.repository.internal.StringFunctions;
import com.trianguloy.llscript.repository.internal.WebClient;

import org.acra.ACRA;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import dw.xmlrpc.Page;

/**
 * Created by Lukas on 20.04.2015.
 * Provides an UI to edit/create a script page
 */
public class EditorActivity extends Activity {

    private static final int STATE_NONE = -1;
    private static final int STATE_CHOOSE_ACTION = 0;
    private static final int STATE_CREATE = 1;
    private static final int STATE_EDIT = 2;
    private static final int STATE_PREVIEW = 3;
    private SharedPreferences sharedPref;
    private String pageId;
    private EditText editor;
    private RPCService rpcService;
    private Repository repository;
    private Repository.RepositoryCategory addTo;
    private String pageName;
    private String pageText;
    private Random random;
    private int state = STATE_NONE;
    private ServiceConnection connection;
    private Lock lock;
    private int textHash=-1;
    private boolean isTemplate = false;
    private Bundle savedInstanceState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.savedInstanceState = savedInstanceState;
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        RepositoryImporter.setTheme(this,sharedPref);
        lock = new Lock();
        setContentView(R.layout.activity_empty);
        lock.lock();
        startService(new Intent(this,RPCService.class));

        random = new Random();
        connect();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_logout:
                stopService(new Intent(this, RPCService.class));
                finish();
                break;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if(state == STATE_PREVIEW){
            setContentView(R.layout.activity_edit);
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB){
                getActionBar().setDisplayHomeAsUpEnabled(false);
            }
            editor = (EditText)findViewById(R.id.editor);
            editor.setText(pageText);
        }
        else if(changedCode()){
            Dialogs.unsavedChanges(this, new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            });
        }
        else finish();
    }

    private boolean changedCode(){
        return  state == STATE_EDIT && editor.getText().toString().hashCode() != textHash;
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        lock.unlock();
        boolean showActionBar = true;
        switch (layoutResID){
            case R.layout.activity_select_action:
                state = STATE_CHOOSE_ACTION;
                if(rpcService.isLoggedIn())((TextView)findViewById(R.id.textUser)).setText(getString(R.string.text_LoggedInAs)+" "+rpcService.getUser());
                break;
            case R.layout.activity_create:
                state = STATE_CREATE;
                break;
            case R.layout.activity_edit:
                state = STATE_EDIT;
                showActionBar = false;
                break;
            case R.layout.activity_preview:
                state = STATE_PREVIEW;
                break;
            case R.layout.activity_empty:
                state = STATE_NONE;
                break;
            default:
                if(BuildConfig.DEBUG) Log.wtf(EditorActivity.class.getSimpleName(),"Unknown state!");
        }
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB) {
            if (showActionBar) getActionBar().show();
            else getActionBar().hide();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connection!=null)unbindService(connection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(getString(R.string.key_state), state);
        if(state == STATE_EDIT){
            pageText = editor.getText().toString();
        }
        if(state == STATE_EDIT || state  == STATE_PREVIEW) {
            outState.putString(getString(R.string.key_pageText), pageText);
            outState.putString(getString(R.string.key_pageName), pageName);
            outState.putString(getString(R.string.key_pageId), pageId);
            outState.putInt(getString(R.string.key_textHash), textHash);
            outState.putBoolean(getString(R.string.key_isTemplate), isTemplate);
        }
    }

    private void restore(@NonNull Bundle restoreState){
        if(restoreState.containsKey(getString(R.string.key_state))){
            switch (restoreState.getInt(getString(R.string.key_state))){
                case STATE_NONE:
                case STATE_CHOOSE_ACTION:
                    setContentView(R.layout.activity_select_action);
                    break;
                case STATE_CREATE:
                    createPage();
                    break;
                case STATE_EDIT:
                case STATE_PREVIEW:
                    pageText = restoreState.getString(getString(R.string.key_pageText));
                    pageName = restoreState.getString(getString(R.string.key_pageName));
                    pageId = restoreState.getString(getString(R.string.key_pageId));
                    textHash = restoreState.getInt(getString(R.string.key_textHash));
                    isTemplate = restoreState.getBoolean(getString(R.string.key_isTemplate));
                    showPageEditor(pageText);
                    break;
            }
        }

    }

    private void connect(){
        connection =  new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                rpcService = ((RPCService.LocalBinder) iBinder).getService();
                if (rpcService.isLoggedIn()) {
                    if(savedInstanceState!=null)restore(savedInstanceState);
                    else setContentView(R.layout.activity_select_action);
                } else {
                    findAccount(false);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {

            }
        };
        bindService(new Intent(this, RPCService.class), connection, 0);
    }

    private void findAccount(boolean passwordInvalid){
        AccountManager accountManager = AccountManager.get(this);
        Account[] accounts = accountManager.getAccountsByType(getString(R.string.account_type));
        AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    future.getResult();
                    setContentView(R.layout.activity_select_action);
                } catch (OperationCanceledException e) {
                    finish();
                } catch (IOException | AuthenticatorException e) {
                    ACRA.getErrorReporter().handleException(e);
                    finish();
                }
            }
        };
        if (accounts.length == 0) {
            accountManager.addAccount(getString(R.string.account_type), "", null, null, this, callback, null);
        } else if (accountManager.getPassword(accounts[0]) == null || passwordInvalid) {
            accountManager.updateCredentials(accounts[0], "", null, this, callback, null);
        } else login(accounts[0].name, accountManager.getPassword(accounts[0]));
    }

    private void login(final String user, final String password) {
        rpcService.login(user, password, new RPCService.Listener<Integer>() {
            @Override
            public void onResult(Integer result) {
                switch (result) {
                    case Constants.RESULT_BAD_LOGIN:
                        Dialogs.badLogin(EditorActivity.this, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                findAccount(true);
                            }
                        });
                        break;
                    case Constants.RESULT_NETWORK_ERROR:
                        Dialogs.connectionFailed(EditorActivity.this, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        });
                        break;
                    case Constants.RESULT_OK:
                        if(savedInstanceState!=null)restore(savedInstanceState);
                        else setContentView(R.layout.activity_select_action);
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            }
        });
    }

    public void button(View view){
        if(!lock.isLocked()) {
            lock.lock();
            switch (view.getId()) {
                case R.id.buttonCreatePage:
                    createPage();
                    break;
                case R.id.buttonEditPage:
                    editPage();
                    break;
                case R.id.buttonCancel:
                    cancelEdit();
                    break;
                case R.id.buttonSave:
                    savePage();
                    break;
                case R.id.buttonCreate:
                    commitCreate();
                    break;
                case R.id.buttonPreview:
                    preview();
                    break;
                case R.id.buttonTemplate:
                    editTemplate();
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }
    }

    private void editTemplate() {
        isTemplate = true;
        showPageEditor(sharedPref.getString(getString(R.string.pref_template), ""));
    }

    private void createPage(){
        rpcService.getPage(getString(R.string.id_scriptRepository), new RPCService.Listener<String>() {
            @Override
            public void onResult(String result) {
                lock.unlock();
                if (result == null) {
                    Dialogs.connectionFailed(EditorActivity.this);
                    return;
                }
                String[] lines = result.split("\n");
                final String circumflex = "^";
                repository = new Repository(lines);
                repository.categories.add(new Repository.RepositoryCategory(getString(R.string.text_none), -1, -1));
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    if (!line.startsWith("|") && !line.startsWith(circumflex)) {
                        if (repository.tableStartLine != -1) {
                            repository.tableEndLine = i - 1;
                            break;
                        }
                        continue;
                    }
                    if (repository.tableStartLine == -1) repository.tableStartLine = i;
                    else if (line.startsWith(circumflex))
                        repository.categories.add(new Repository.RepositoryCategory(StringFunctions.findBetween(line, circumflex, "^^^", 0, false).value, i, 0));
                    else if (line.startsWith("|//**"))
                        repository.categories.add(new Repository.RepositoryCategory(StringFunctions.findBetween(line, "|//**", "**//||\\\\ |", 0, false).value, i, 1));
                }
                setContentView(R.layout.activity_create);
                Spinner spinner = (Spinner) findViewById(R.id.spinner);
                spinner.setAdapter(new CategoryAdapter(EditorActivity.this, repository.categories));
            }
        });
    }

    private void editPage() {
        rpcService.getAllPages(new RPCService.Listener<List<Page>>() {
            @Override
            public void onResult(List<Page> result) {
                lock.unlock();
                if (result == null) {
                    Dialogs.connectionFailed(EditorActivity.this);
                    return;
                }
                final HashSet<Page> pages = new HashSet<>();
                for (Page p : result) {
                    if (p.id().startsWith(getString(R.string.prefix_script))) pages.add(p);
                }
                Page[] array = pages.toArray(new Page[pages.size()]);
                showSelectPageToEdit(array);
            }
        });
    }

    private void cancelEdit(){
        lock.unlock();
        if(changedCode()) {
            Dialogs.unsavedChanges(this, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    setContentView(R.layout.activity_select_action);
                }
            });
        }
        else setContentView(R.layout.activity_select_action);
    }

    private void savePage() {
        final String text = editor.getText().toString();
        if (isTemplate){
            sharedPref.edit().putString(getString(R.string.pref_template),text).apply();
            lock.unlock();
            textHash = text.hashCode();
            Dialogs.saved(this,null);
        }
        else if(text.equals("")) {
            lock.unlock();
            Dialogs.cantSaveEmpty(this);
        }
        else {
            rpcService.putPage(pageId, text, new RPCService.Listener<Boolean>() {
                @Override
                public void onResult(Boolean result) {
                    if(result){
                        textHash = text.hashCode();
                        if (addTo != null) {
                            int index = repository.categories.indexOf(addTo);
                            int addAt = repository.tableEndLine;
                            if (addTo.level == 0) {
                                for (int i = index + 1; i < repository.categories.size(); i++) {
                                    if (repository.categories.get(i).level == addTo.level) {
                                        addAt = repository.categories.get(i).line;
                                        break;
                                    }
                                }
                            } else {
                                for (int i = addTo.line + 1; i < repository.lines.size(); i++) {
                                    if (repository.lines.get(i).startsWith("|[[")) {
                                        addAt = i;
                                        break;
                                    }
                                }
                            }
                            String add = (addTo.level == 0 ? "|" : "|\\\\ |") +
                                    "[[" + pageId + ((pageName == null) ? "" : " |" + pageName) + "]]" +
                                    ((addTo.level == 0) ? "||\\\\ |" : "|\\\\ |");
                            repository.lines.add(addAt, add);
                            rpcService.putPage(getString(R.string.id_scriptRepository), TextUtils.join("\n", repository.lines), new RPCService.Listener<Boolean>() {
                                @Override
                                public void onResult(Boolean result) {
                                    lock.unlock();
                                    if(result)Dialogs.saved(EditorActivity.this,pageId);
                                    else Dialogs.connectionFailed(EditorActivity.this);
                                }
                            });
                        } else {
                            lock.unlock();
                            Dialogs.saved(EditorActivity.this,pageId);
                        }
                    }
                    else {
                        lock.unlock();
                        Dialogs.connectionFailed(EditorActivity.this);
                    }
                }
            });
        }
    }

    private void commitCreate(){
        pageId = getString(R.string.prefix_script)+((EditText)findViewById(R.id.editId)).getText();
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        final Repository.RepositoryCategory selected = ((Repository.RepositoryCategory)spinner.getSelectedItem());
        rpcService.getPage(pageId, new RPCService.Listener<String>() {
            @Override
            public void onResult(String result) {
                if (result == null || result.equals("")) {
                    if (selected.level >= 0) {
                        addTo = selected;
                        pageName = ((EditText) EditorActivity.this.findViewById(R.id.editName)).getText().toString();
                    }
                    if (((RadioButton) findViewById(R.id.radioDefault)).isChecked()) {
                        rpcService.getPage(getString(R.string.id_scriptTemplate), new RPCService.Listener<String>() {
                            @Override
                            public void onResult(String result) {
                                lock.unlock();
                                if (result == null) Dialogs.connectionFailed(EditorActivity.this);
                                else showPageEditor(result);
                            }
                        });
                    }
                    else if(((RadioButton)findViewById(R.id.radioCustom)).isChecked()){
                        showPageEditor(sharedPref.getString(getString(R.string.pref_template),""));
                    }
                    else showPageEditor("");
                } else {
                    lock.unlock();
                    Dialogs.pageAlreadyExists(EditorActivity.this);
                }
            }
        });
    }

    public void action(View view) {
        if(state!=STATE_EDIT)throw new IllegalStateException("Can't execute actions when not in editor");
        if(!lock.isLocked()) {
            switch (view.getId()) {
                case R.id.action_bold:
                    surroundOrAdd("**", "**", getString(R.string.text_bold));
                    break;
                case R.id.action_italic:
                    surroundOrAdd("//", "//", getString(R.string.text_italic));
                    break;
                case R.id.action_underline:
                    surroundOrAdd("__", "__", getString(R.string.text_underline));
                    break;
                case R.id.action_code:
                    surroundOrAdd("<sxh javascript;>", "</sxh>", getString(R.string.text_code));
                    break;
                case R.id.action_unorderedList:
                    surroundOrAdd("  * ", "", getString(R.string.text_unorderedList));
                    break;
                case R.id.action_orderedList:
                    surroundOrAdd("  - ", "", getString(R.string.text_orderedList));
                    break;
                default:
                    if (BuildConfig.DEBUG)
                        Log.i(EditorActivity.class.getSimpleName(), "Ignored action " + view.getId());
                    break;
            }
        }
    }

    private void preview(){
        final String tempId = getString(R.string.prefix_temp)+ random.nextInt();
        pageText = editor.getText().toString();
        rpcService.putPage(getString(R.string.prefix_script) + tempId, pageText, new RPCService.Listener<Boolean>() {
            @Override
            public void onResult(@Nullable Boolean result) {
                lock.unlock();
                if(!result) Dialogs.connectionFailed(EditorActivity.this);
                else showPreview(tempId);
            }
        });
    }

    private void showPreview(final String tempId){
        setContentView(R.layout.activity_preview);
        lock.lock();
        final WebView webView = (WebView)findViewById(R.id.webPreview);
        webView.getSettings().setJavaScriptEnabled(true);
        //noinspection deprecation
        webView.setWebViewClient(new WebClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                rpcService.putPage("script_" + tempId, "",null);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Toast.makeText(EditorActivity.this,getString(R.string.toast_previewLink),Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View ignored) {
                return true;
            }
        });
        new DownloadTask(new DownloadTask.Listener() {
            @Override
            public void onFinish(String result) {
                if (!sharedPref.getBoolean(getString(R.string.pref_showTools), false)) {
                    //remove tools
                    StringFunctions.valueAndIndex val = StringFunctions.findBetween(result, "<div class=\"tools group\">", "<hr class=\"a11y\" />", 0, false);
                    result = result.substring(0, val.from) + result.substring(val.to, result.length());
                }
                webView.loadDataWithBaseURL(getString(R.string.link_server),result,"text/html","UTF-8",null);
                lock.unlock();
            }

            @Override
            public void onError() {
                if (BuildConfig.DEBUG) Log.i("Preview", "Ignored Error");
                lock.unlock();
            }
        }).execute(getString(R.string.link_scriptPagePrefix) + tempId);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB){
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void showSelectPageToEdit(final Page[] pages){
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,R.layout.sub_list_item);
        for (Page p:pages)adapter.add(p.id());
        adapter.sort(new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                return StringFunctions.getNameForPageFromPref(sharedPref,EditorActivity.this,lhs).toLowerCase().compareTo(StringFunctions.getNameForPageFromPref(sharedPref, EditorActivity.this, rhs).toLowerCase());
            }
        });
        Dialogs.selectPageToEdit(this, adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                lock.lock();
                dialogInterface.dismiss();
                loadPageToEdit(adapter.getItem(i));
            }
        });
    }

    private void loadPageToEdit(String id){
        pageId = id;
        rpcService.getPage(pageId, new RPCService.Listener<String>() {
            @Override
            public void onResult(@Nullable String result) {
                lock.unlock();
                if (result == null) Dialogs.connectionFailed(EditorActivity.this);
                else showPageEditor(result);
            }
        });
    }

    private void showPageEditor(String text){
        setContentView(R.layout.activity_edit);
        editor = (EditText)findViewById(R.id.editor);
        editor.setText(text);
        pageText = text;
        if(textHash==-1) textHash = text.hashCode();
    }

    private void surroundOrAdd(String prefix, String suffix, String text){
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd();
        Editable editable = editor.getEditableText();
        if (start == end) {
            editable.insert(start==-1?0:start,prefix+text+suffix);
            editor.setSelection(start + prefix.length(), start + prefix.length() + text.length());
        } else {
            editable.insert(end,suffix);
            editable.insert(start,prefix);
            editor.setSelection(start+prefix.length(),end+prefix.length());
        }
    }

    private class Lock {
        boolean state;
        public Lock(){
            state = true;
        }
        public void lock(){
            state = true;
            final ProgressBar bar = (ProgressBar)findViewById(R.id.progressBar);
            if(bar!=null)bar.setVisibility(View.VISIBLE);
        }

        public void unlock(){
            state = false;
            final ProgressBar bar = (ProgressBar)findViewById(R.id.progressBar);
            if(bar!=null)bar.setVisibility(View.GONE);
        }
        public boolean isLocked(){
            return state;
        }

    }
}
