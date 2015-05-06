package com.trianguloy.llscript.repository;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.trianguloy.llscript.repository.auth.AuthenticatorActivity;
import com.trianguloy.llscript.repository.internal.Dialogs;
import com.trianguloy.llscript.repository.internal.DownloadTask;
import com.trianguloy.llscript.repository.internal.StringFunctions;
import com.trianguloy.llscript.repository.internal.WebClient;

import org.acra.ACRA;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import dw.xmlrpc.DokuJClient;
import dw.xmlrpc.Page;
import dw.xmlrpc.exception.DokuException;
import dw.xmlrpc.exception.DokuUnauthorizedException;

/**
 * Created by Lukas on 20.04.2015.
 * Provides an UI to edit/create a script page
 */
public class EditorActivity extends Activity {

    private static final String ALREADY_EXISTS = "AlreadyExists";
    private static final int STATE_NONE = -1;
    private static final int STATE_CHOOSE_ACTION = 0;
    private static final int STATE_CREATE = 1;
    private static final int STATE_EDIT = 2;
    private static final int STATE_PREVIEW = 3;
    private SharedPreferences sharedPref;
    private String pageId;
    private EditText editor;
    private DokuJClient client;
    private Repository repository;
    private RepositoryCategory addTo;
    private String pageName;
    private String pageText;
    private Random random;
    private int state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        state = STATE_NONE;

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        random = new Random();
        findAccount();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==android.R.id.home){
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if(state == STATE_PREVIEW){
            setContentView(R.layout.activity_edit);
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB){
                ActionBar bar = getActionBar();
                bar.setDisplayHomeAsUpEnabled(false);
                bar.hide();
            }
            editor = (EditText)findViewById(R.id.editor);
            editor.setText(pageText);
        }
        else finish();
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        switch (layoutResID){
            case R.layout.activity_select_action:
                state = STATE_CHOOSE_ACTION;
                break;
            case R.layout.activity_create:
                state = STATE_CREATE;
                break;
            case R.layout.activity_edit:
                state = STATE_EDIT;
                break;
            case R.layout.activity_preview:
                state = STATE_PREVIEW;
                break;
            default:
                if(BuildConfig.DEBUG) Log.wtf(EditorActivity.class.getSimpleName(),"Unknown state!");
        }
    }

    private void findAccount(){
        if (AuthenticatorActivity.getUser() == null || AuthenticatorActivity.getPassword() == null) {
            AccountManager accountManager = AccountManager.get(this);
            Account[] accounts = accountManager.getAccountsByType(getString(R.string.account_type));
            AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {
                public void run(AccountManagerFuture<Bundle> future) {
                    try {
                        future.getResult();
                        findAccount();

                    } catch (OperationCanceledException ignored) {
                    } catch (IOException | AuthenticatorException e) {
                        ACRA.getErrorReporter().handleException(e);
                    }
                }
            };
            if (accounts.length == 0) {
                accountManager.addAccount(getString(R.string.account_type), "", null, null, this, callback, null);
            } else if (accountManager.getPassword(accounts[0]) == null) {
                accountManager.updateCredentials(accounts[0], "", null, this, callback, null);
            } else login(accounts[0].name, accountManager.getPassword(accounts[0]));
        } else {
            login(AuthenticatorActivity.getUser(),AuthenticatorActivity.getPassword());
            AuthenticatorActivity.resetCredentials();
        }
    }

    private void login(final String user, final String password) {
        new AsyncTask<Void,Void,Integer>(){
            @Override
            protected Integer doInBackground(Void... params) {
                int result;
                try {
                    client = new DokuJClient(getString(R.string.link_xmlrpc));
                    try {
                        //test if logged in
                        Object[] parameters = new Object[]{user, password};
                        boolean login = (boolean)client.genericQuery("dokuwiki.login", parameters);
                        if(login) result = Constants.RESULT_OK;
                        else result = Constants.RESULT_BAD_LOGIN;
                    }
                    catch (DokuUnauthorizedException e){
                        e.printStackTrace();
                        result = Constants.RESULT_BAD_LOGIN;
                    }
                } catch (MalformedURLException | DokuException e) {
                    e.printStackTrace();
                    result = Constants.RESULT_NETWORK_ERROR;
                }
                return result;
            }

            @Override
            protected void onPostExecute(Integer integer) {
                Runnable finish = new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                };
                switch (integer){
                    case Constants.RESULT_BAD_LOGIN:
                        Dialogs.badLogin(EditorActivity.this,finish);
                        break;
                    case Constants.RESULT_NETWORK_ERROR:
                        Dialogs.connectionFailed(EditorActivity.this,finish);
                        break;
                    case Constants.RESULT_OK:
                        setContentView(R.layout.activity_select_action);
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            }
        }.execute();
    }

    public void button(View view){
        switch (view.getId()){
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
            default:
                throw new IllegalArgumentException();
        }
    }

    private void createPage(){
        new AsyncTask<Void,Void,Boolean>(){
            @Override
            protected Boolean doInBackground(Void... params) {
                boolean result = false;
                try {
                    String repoText = client.getPage(getString(R.string.id_scriptRepository));
                    String[] lines = repoText.split("\n");
                    final String circumflex = "^";
                    repository = new Repository(lines);
                    repository.categories.add(new RepositoryCategory(getString(R.string.text_none), -1, -1));
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
                            repository.categories.add(new RepositoryCategory(StringFunctions.findBetween(line, circumflex, "^^^", 0, false).value, i, 0));
                        else if (line.startsWith("|//**"))
                            repository.categories.add(new RepositoryCategory(StringFunctions.findBetween(line, "|//**", "**//||\\\\ |", 0, false).value, i, 1));
                    }
                    result = true;
                }catch (DokuException e) {
                    e.printStackTrace();
                }
                return result;
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                if(aBoolean) {
                    setContentView(R.layout.activity_create);
                    Spinner spinner = (Spinner) findViewById(R.id.spinner);
                    spinner.setAdapter(new CategoryAdapter(EditorActivity.this, repository.categories));
                }
                else Dialogs.connectionFailed(EditorActivity.this);
            }
        }.execute();
    }

    private void editPage(){
        new AsyncTask<Void,Void,Page[]>(){
            @Override
            protected Page[] doInBackground(Void... params) {
                Page[] result = null;
                try {
                    List<Page> list = client.getAllPages();
                    final HashSet<Page> pages = new HashSet<>();
                    for (Page p: list){
                        if(p.id().startsWith(getString(R.string.prefix_script)))pages.add(p);
                    }
                    result = pages.toArray(new Page[pages.size()]);
                } catch (DokuException e) {
                    e.printStackTrace();
                }
                return result;
            }

            @Override
            protected void onPostExecute(Page[] pages) {
                if(pages == null) Dialogs.connectionFailed(EditorActivity.this);
                else showSelectPageToEdit(pages);
            }
        }.execute();
    }

    private void cancelEdit(){
        setContentView(R.layout.activity_select_action);
    }

    private void savePage() {
        final String text = editor.getText().toString();
        if(text == null || text.equals("")) Dialogs.cantSaveEmpty(this);
        else {
            //TODO progressDialog to notify user that saving is going on
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... params) {
                    boolean result = false;
                    try {
                        client.putPage(pageId, text);
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
                            client.putPage(getString(R.string.id_scriptRepository), TextUtils.join("\n", repository.lines));
                        }
                        result = true;
                    } catch (DokuException e) {
                        e.printStackTrace();
                    }
                    return result;
                }

                @Override
                protected void onPostExecute(Boolean aBoolean) {
                    if (aBoolean) Dialogs.saved(EditorActivity.this, pageId);
                    else Dialogs.connectionFailed(EditorActivity.this);
                }
            }.execute();
        }
    }

    private void commitCreate(){
        pageId = getString(R.string.prefix_script)+((EditText)findViewById(R.id.editId)).getText();
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        final RepositoryCategory selected = ((RepositoryCategory)spinner.getSelectedItem());
        new AsyncTask<Void,Void,String>(){
            @Override
            protected String doInBackground(Void... params) {
                String result = null;
                try {
                    boolean exists;
                    try {
                        String page = client.getPage(pageId);
                        exists = page!=null && !page.equals("");
                    }
                    catch (DokuException e){
                        e.printStackTrace();
                        exists = false;
                    }
                    if(exists){
                        result = ALREADY_EXISTS;
                    }
                    else {
                        if(selected.level>=0) {
                            addTo = selected;
                            pageName = ((EditText)EditorActivity.this.findViewById(R.id.editName)).getText().toString();
                        }
                        final String text;
                        if (((CheckBox) findViewById(R.id.checkTemplate)).isChecked()) {
                            text = client.getPage(getString(R.string.id_scriptTemplate));
                        } else text = "";
                        result = text;
                    }
                } catch (DokuException e) {
                    e.printStackTrace();
                }
                return result;
            }

            @Override
            protected void onPostExecute(String s) {
                if(s == null) Dialogs.connectionFailed(EditorActivity.this);
                else if(s.equals(ALREADY_EXISTS)) Dialogs.pageAlreadyExists(EditorActivity.this);
                else showPageEditor(s);
            }
        }.execute();
    }

    public void action(View view) {
        if(state!=STATE_EDIT)throw new IllegalStateException("Can't execute actions when not in editor");
        switch (view.getId()){
            case R.id.action_bold:
                surroundOrAdd("**","**",getString(R.string.text_bold));
                break;
            case R.id.action_italic:
                surroundOrAdd("//","//",getString(R.string.text_italic));
                break;
            case R.id.action_underline:
                surroundOrAdd("__","__",getString(R.string.text_underline));
                break;
            case R.id.action_code:
                surroundOrAdd("<sxh javascript;>","</sxh>",getString(R.string.text_code));
                break;
            case R.id.action_unorderedList:
                surroundOrAdd("  * ","",getString(R.string.text_unorderedList));
                break;
            case R.id.action_orderedList:
                surroundOrAdd("  - ","",getString(R.string.text_orderedList));
                break;
            default:
                if(BuildConfig.DEBUG) Log.i(EditorActivity.class.getSimpleName(),"Ignored action "+view.getId());
                break;
        }
    }

    private void preview(){
        new AsyncTask<Void,Void,String>(){

            @Override
            protected String doInBackground(Void... params) {
                String result = null;
                try {
                    String tempId = getString(R.string.prefix_temp)+ random.nextInt();
                    pageText = editor.getText().toString();
                    client.putPage(getString(R.string.prefix_script)+tempId,pageText);
                    result = tempId;
                } catch (DokuException e) {
                    e.printStackTrace();
                }
                return result;
            }

            @Override
            protected void onPostExecute(String s) {
                if(s==null) Dialogs.connectionFailed(EditorActivity.this);
                else showPreview(s);
            }
        }.execute();
    }

    private void showPreview(final String tempId){
        setContentView(R.layout.activity_preview);
        final WebView webView = (WebView)findViewById(R.id.webPreview);
        webView.getSettings().setJavaScriptEnabled(true);
        //noinspection deprecation
        webView.setWebViewClient(new WebClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            client.putPage("script_" + tempId, "");
                        } catch (DokuException e1) {
                            e1.printStackTrace();
                        }
                        return null;
                    }
                }.execute();
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
            }

            @Override
            public void onError() {
                if (BuildConfig.DEBUG) Log.i("Preview", "Ignored Error");
            }
        }).execute(getString(R.string.link_scriptPagePrefix) + tempId);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB){
            ActionBar bar = getActionBar();
            bar.setDisplayHomeAsUpEnabled(true);
            bar.show();
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
        new AlertDialog.Builder(this)
                .setAdapter(adapter,new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        loadPageToEdit(adapter.getItem(which));
                    }
                })
                .setNegativeButton(R.string.button_cancel, null)
                .setTitle(getString(R.string.title_selectPage))
                .show();
    }

    private void loadPageToEdit(String id){
        pageId = id;
        new AsyncTask<Void,Void,String>(){
            @Override
            protected String doInBackground(Void... params) {
                String result = null;
                try{
                    result = client.getPage(pageId);
                } catch (DokuException e) {
                    e.printStackTrace();
                }
                return result;
            }

            @Override
            protected void onPostExecute(String s) {
                if(s == null)Dialogs.connectionFailed(EditorActivity.this);
                else showPageEditor(s);
            }
        }.execute();
    }

    private void showPageEditor(String text){
        setContentView(R.layout.activity_edit);
        editor = (EditText)findViewById(R.id.editor);
        editor.setText(text);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB){
            getActionBar().hide();
        }
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

    private static class Repository{
        int tableStartLine;
        int tableEndLine;
        final List<RepositoryCategory> categories;
        final List<String> lines;

        public Repository(String[] lines){
            this.lines = new ArrayList<>(Arrays.asList(lines));
            categories = new ArrayList<>();
            tableStartLine = -1;
            tableEndLine = -1;
        }

    }

    private static class RepositoryCategory {
        final String name;
        final int line;
        private final int level;

        public RepositoryCategory(String name, int line, int level){
            this.name = name;
            this.line = line;
            this.level = level;
        }

    }

    private static class CategoryAdapter extends ArrayAdapter<RepositoryCategory>{

        private final Context context;

        public CategoryAdapter(Context context, List<RepositoryCategory> objects) {
            super(context, android.R.layout.simple_list_item_1, objects);
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView ==null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            }
            bindView(position,convertView);
            return convertView;
        }

        private void bindView(int position, View row) {
            ((TextView)row.findViewById(android.R.id.text1)).setText(getItem(position).name);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getView(position,convertView,parent);
        }
    }
}
