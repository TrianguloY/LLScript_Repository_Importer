package com.trianguloy.llscript.repository;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.http.HttpResponseCache;
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
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.trianguloy.llscript.repository.internal.AesCbcWithIntegrity;
import com.trianguloy.llscript.repository.internal.AppChooser;
import com.trianguloy.llscript.repository.internal.StringFunctions;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    private SharedPreferences sharedPref;
    private String pageId;
    private EditText editor;
    private DokuJClient client;
    private Repository repository;
    private RepositoryCategory addTo;
    private String pageName;
    private String pageText;
    private AesCbcWithIntegrity.SecretKeys key;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        editor = null;

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.activity_login);
        ((EditText) findViewById(R.id.username)).setText(sharedPref.getString(getString(R.string.pref_user), ""));
        Map<String,Object> map  = StringFunctions.getMapFromPref(sharedPref, getString(R.string.pref_pageNames));
        String k = (String)map.get(getString(R.string.text_none));
        key = null;
        try {
            if (k!=null) {
                try {
                    key = AesCbcWithIntegrity.keys(k);
                }catch (IllegalArgumentException e){
                    e.printStackTrace();
                }
            }
            if(key == null){
                key = AesCbcWithIntegrity.generateKey();
            }
            map.put(getString(R.string.text_none),key.toString());
            StringFunctions.saveMapToPref(sharedPref,getString(R.string.pref_pageNames),map);
            String encPw = sharedPref.getString(getString(R.string.pref_password),null);
            if(encPw!=null){
                String pw = AesCbcWithIntegrity.decryptString(new AesCbcWithIntegrity.CipherTextIvMac(encPw),key);
                ((EditText) findViewById(R.id.password)).setText(pw);
            }
        } catch (GeneralSecurityException | UnsupportedEncodingException |IllegalArgumentException e ) {
            e.printStackTrace();
        }
        ((CheckBox)findViewById(R.id.checkRemember)).setChecked(sharedPref.getBoolean(getString(R.string.pref_remindPassword),false));
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
        if(findViewById(R.id.webPreview)!=null){
            setContentView(R.layout.activity_edit);
            if(Build.VERSION.SDK_INT>=11)getActionBar().setDisplayHomeAsUpEnabled(false);
            editor = (EditText)findViewById(R.id.editor);
            editor.setText(pageText);
        }
        else super.onBackPressed();
    }

    @SuppressWarnings("UnusedParameters")
    public void login(View v) {
        final String user = ((EditText) findViewById(R.id.username)).getText().toString();
        final String password = ((EditText) findViewById(R.id.password)).getText().toString();
        boolean remember = ((CheckBox)findViewById(R.id.checkRemember)).isChecked();
        String save = null;
        if(remember){
            try {
                save = AesCbcWithIntegrity.encrypt(password,key).toString();
            } catch (UnsupportedEncodingException | GeneralSecurityException e) {
                e.printStackTrace();
                save = null;
            }
        }
        sharedPref.edit()
                .putString(getString(R.string.pref_user),user)
                .putBoolean(getString(R.string.pref_remindPassword),remember)
                .putString(getString(R.string.pref_password),save)
                .apply();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        client = new DokuJClient(getString(R.string.link_xmlrpc),user,password);
                        try {
                            //test if logged in
                            client.getPageInfo(getString(R.string.id_scriptRepository));
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setContentView(R.layout.activity_select_action);
                                }
                            });
                        }
                        catch (DokuUnauthorizedException e){
                            e.printStackTrace();
                            showBadLogin();
                        }
                    } catch (MalformedURLException | DokuException e) {
                        e.printStackTrace();
                        showConnectionFailed();
                    }
                }
            }).start();
    }

    @SuppressWarnings("UnusedParameters")
    public void register(View v) {
        new AppChooser(this, Uri.parse(getString(R.string.link_register)), getString(R.string.title_appChooserRegister), getString(R.string.message_noBrowser), null).show();
    }

    @SuppressWarnings("UnusedParameters")
    public void createPage(View v){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String repoText = client.getPage(getString(R.string.id_scriptRepository));
                    String[] lines = repoText.split("\n");
                    repository = new Repository(lines);
                    repository.categories.add(new RepositoryCategory(getString(R.string.text_none),-1,-1));
                    for (int i = 0; i< lines.length; i++){
                        String line = lines[i];
                        if(!line.startsWith("|")&&!line.startsWith("^")){
                            if(repository.tableStartLine!=-1){
                                repository.tableEndLine = i-1;
                                break;
                            }
                            continue;
                        }
                        if(repository.tableStartLine == -1)repository.tableStartLine = i;
                        else if(line.startsWith("^"))repository.categories.add(new RepositoryCategory(StringFunctions.findBetween(line,"^","^^^",0,false).value,i,0));
                        else if(line.startsWith("|//**"))repository.categories.add(new RepositoryCategory(StringFunctions.findBetween(line,"|//**","**//||\\\\ |",0,false).value,i,1));
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setContentView(R.layout.activity_create);
                            Spinner spinner = (Spinner) findViewById(R.id.spinner);
                            spinner.setAdapter(new CategoryAdapter(EditorActivity.this, repository.categories));
                        }
                    });
                } catch (DokuException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @SuppressWarnings("UnusedParameters")
    public void editPage(View v){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    List<Page> list = client.getAllPages();
                    final HashSet<Page> pages = new HashSet<>();
                    for (Page p: list){
                        if(p.id().startsWith(getString(R.string.prefix_script)))pages.add(p);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showSelectPageToEdit(pages.toArray(new Page[pages.size()]));
                        }
                    });
                } catch (DokuException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @SuppressWarnings("UnusedParameters")
    public void cancelEdit(View v){
        setContentView(R.layout.activity_select_action);
        editor = null;
    }

    @SuppressWarnings("UnusedParameters")
    public void savePage(View v) {
        //TODO progressDialog to notify user that saving is going on
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    client.putPage(pageId,editor.getText().toString());
                    if(addTo!=null){
                        int index = repository.categories.indexOf(addTo);
                        int addAt = repository.tableEndLine;
                        if(addTo.level == 0){
                            for(int i=index+1;i<repository.categories.size();i++){
                                if(repository.categories.get(i).level == addTo.level){
                                    addAt = repository.categories.get(i).line;
                                    break;
                                }
                            }
                        }else{
                            for(int i=addTo.line+1;i<repository.lines.size();i++){
                                if(repository.lines.get(i).startsWith("|[[")){
                                    addAt = i;
                                    break;
                                }
                            }
                        }
                        String add = ((addTo.level==0)?"|":"|\\\\ |") +
                                "[[" + pageId + ((pageName!=null)?(" |"+pageName):"")+ "]]" +
                                ((addTo.level==0)?"||\\\\ |":"|\\\\ |");
                        repository.lines.add(addAt,add);
                        client.putPage(getString(R.string.id_scriptRepository), TextUtils.join("\n", repository.lines));

                    }
                    showSaved();

                } catch (DokuException e) {
                    e.printStackTrace();
                    showConnectionFailed();
                }
            }
        }).start();
    }

    @SuppressWarnings("UnusedParameters")
    public void commitCreate(View v){
        pageId = getString(R.string.prefix_script)+((EditText)findViewById(R.id.editId)).getText();
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        final RepositoryCategory selected = ((RepositoryCategory)spinner.getSelectedItem());
            new Thread(new Runnable() {
                @Override
                public void run() {
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
                            showPageAlreadyExists();
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
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showPageEditor(text);
                                }
                            });
                        }
                    } catch (DokuException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
    }

    @SuppressWarnings("UnusedParameters")
    public void action(View v) {
        switch (v.getId()){
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
        }
    }

    @SuppressWarnings("UnusedParameters")
    public void preview(View v){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String tempId = getString(R.string.prefix_temp)+ new Random().nextInt();
                    pageText = editor.getText().toString();
                    client.putPage(getString(R.string.prefix_script)+tempId,pageText);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showPreview(tempId);
                        }
                    });
                } catch (DokuException e) {
                    e.printStackTrace();
                    showConnectionFailed();
                }
            }
        }).start();
    }

    private void showPreview(final String tempId){
        setContentView(R.layout.activity_preview);
        final WebView webView = (WebView)findViewById(R.id.webPreview);
        //noinspection deprecation
        webView.setWebViewClient(new WebViewClient(){

            @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            @Override
            public WebResourceResponse shouldInterceptRequest(final WebView view, final String url) {
                //from http://stackoverflow.com/questions/12063937/can-i-use-the-android-4-httpresponsecache-with-a-webview-based-application/13596877#13596877
                if (Build.VERSION.SDK_INT < 14 || !(url.startsWith("http://") || url.startsWith("https://")) || HttpResponseCache.getInstalled() == null)
                    return null;
                try {
                    final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                    connection.connect();
                    final String content_type = connection.getContentType();
                    final String separator = "; charset=";
                    final int pos = content_type.indexOf(separator);
                    final String mime_type = pos >= 0 ? content_type.substring(0, pos) : content_type;
                    final String encoding = pos >= 0 ? content_type.substring(pos + separator.length()) : "UTF-8";
                    return new WebResourceResponse(mime_type, encoding, connection.getInputStream());
                } catch (final MalformedURLException e) {
                    e.printStackTrace();
                    return null;
                } catch (final IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            client.putPage("script_"+tempId,"");
                        } catch (DokuException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        });
        webView.loadUrl(getString(R.string.link_scriptPagePrefix) + tempId);
        if(Build.VERSION.SDK_INT>=11)getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void showPageAlreadyExists() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(EditorActivity.this)
                        .setTitle(getString(R.string.title_error))
                        .setMessage(getString(R.string.text_alreadyExists))
                        .setNeutralButton(R.string.button_ok, null)
                        .show();
            }
        });
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String text = client.getPage(pageId);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showPageEditor(text);
                        }
                    });
                } catch (DokuException e) {
                    e.printStackTrace();
                    showConnectionFailed();
                }
            }
        }).start();
    }

    private void showPageEditor(String text){
        setContentView(R.layout.activity_edit);
        editor = (EditText)findViewById(R.id.editor);
        editor.setText(text);
    }

    private void showConnectionFailed() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(EditorActivity.this)
                        .setTitle(getString(R.string.title_error))
                        .setMessage(getString(R.string.text_cantConnect))
                        .setNeutralButton(R.string.button_ok, null)
                        .show();
            }
        });
    }

    private void showBadLogin() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(EditorActivity.this)
                        .setTitle(getString(R.string.title_error))
                        .setMessage(getString(R.string.text_badLogin))
                        .setNeutralButton(R.string.button_ok, null)
                        .show();
            }
        });
    }

    private void showSaved(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(EditorActivity.this)
                        .setTitle(getString(R.string.title_saved))
                        .setMessage(getString(R.string.text_doNext))
                        .setPositiveButton(getString(R.string.button_viewPage),new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(getString(R.string.link_scriptPagePrefix)+pageId.substring(7)));
                                intent.setClass(EditorActivity.this,IntentHandle.class);
                                startActivity(intent);
                                finish();
                            }
                        })
                        .setNeutralButton(getString(R.string.button_goHome),new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(getString(R.string.link_repository)));
                                intent.setClass(EditorActivity.this, IntentHandle.class);
                                startActivity(intent);
                                finish();
                            }
                        })
                        .setNegativeButton(getString(R.string.button_stay),null)
                        .show();
            }
        });
    }

    private void surroundOrAdd(String prefix, String suffix, String text){
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd();
        Editable editable = editor.getEditableText();
        if(start!=end){
            editable.insert(end,suffix);
            editable.insert(start,prefix);
            editor.setSelection(start+prefix.length(),end+prefix.length());
        }
        else {
            editable.insert(start==-1?0:start,prefix+text+suffix);
            editor.setSelection(start + prefix.length(), start + prefix.length() + text.length());
        }
    }

    class Repository{
        int tableStartLine;
        int tableEndLine;
        final ArrayList<RepositoryCategory> categories;
        final ArrayList<String> lines;

        public Repository(String[] lines){
            this.lines = new ArrayList<>(Arrays.asList(lines));
            categories = new ArrayList<>();
            tableStartLine = -1;
            tableEndLine = -1;
        }

    }

    class RepositoryCategory {
        final String name;
        final int line;
        private final int level;

        public RepositoryCategory(String name, int line, int level){
            this.name = name;
            this.line = line;
            this.level = level;
        }

    }

    class CategoryAdapter extends ArrayAdapter<RepositoryCategory>{

        private final Context context;

        public CategoryAdapter(Context context, List<RepositoryCategory> objects) {
            super(context, android.R.layout.simple_list_item_1, objects);
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView ==null) convertView = newView(parent);
            bindView(position,convertView);
            return convertView;
        }

        private View newView(ViewGroup parent) {
            return (((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(android.R.layout.simple_list_item_1, parent, false));
        }

        private void bindView(int position, View row) {
            ((TextView)row.findViewById(android.R.id.text1)).setText(getItem(position).name);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if(convertView ==null) convertView = newView(parent);
            bindView(position,convertView);
            return convertView;
        }
    }
}
