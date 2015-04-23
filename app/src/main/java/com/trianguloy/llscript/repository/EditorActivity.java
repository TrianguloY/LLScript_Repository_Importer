package com.trianguloy.llscript.repository;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.trianguloy.llscript.repository.internal.AppChooser;
import com.trianguloy.llscript.repository.internal.DownloadTask;
import com.trianguloy.llscript.repository.internal.StringFunctions;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;

/**
 * Created by Lukas on 20.04.2015.
 * Provides an UI to edit/create a script page
 */
public class EditorActivity extends Activity {

    private SharedPreferences sharedPref;
    private String sessionToken;
    private String pageId;
    private EditText editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (!(sharedPref.contains(getString(R.string.pref_user)) && sharedPref.contains(getString(R.string.pref_password)))) {
            setContentView(R.layout.activity_login);
        }
    }

    @SuppressWarnings("UnusedParameters")
    public void login(View v) {
        final String user = ((EditText) findViewById(R.id.username)).getText().toString();
        final String password = ((EditText) findViewById(R.id.password)).getText().toString();
        new DownloadTask(new DownloadTask.Listener() {
            @Override
            public void onFinish(String result) {
                sessionToken = StringFunctions.findBetween(result, "sectok=", "\"  class=\"action login\"", 0, false).value;
                sendLogin(user, password);
            }

            @Override
            public void onError() {
                showConnectionFailed();
            }
        }).execute(getString(R.string.link_repository));
    }

    @SuppressWarnings("UnusedParameters")
    public void register(View v) {
        new AppChooser(this, Uri.parse(getString(R.string.link_register)), getString(R.string.title_appChooserRegister), getString(R.string.message_noBrowser), null).show();
    }

    void sendLogin(String user, String password) {
        try {
            new DownloadTask(new DownloadTask.Listener() {
                @Override
                public void onFinish(String result) {
                    boolean login = result.contains("Logged in as");
                    Log.d("Login Result", String.valueOf(login));
                    if(login)
                        setContentView(R.layout.activity_select_action);
                    else showConnectionFailed();
                }

                @Override
                public void onError() {
                    showConnectionFailed();
                }
            }).execute(getString(R.string.link_repository) + "&do=login&sectok=" + sessionToken + "&u=" + URLEncoder.encode(user, "UTF-8") + "&p=" + URLEncoder.encode(password, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            showConnectionFailed();
        }
    }

    public void createPage(View v){

    }

    public void editPage(View v){
        new DownloadTask(new DownloadTask.Listener() {
            @Override
            public void onFinish(String result) {
                int index = result.indexOf("\">script_");
                HashSet<String> set = new HashSet<>();
                while (index!= -1){
                    StringFunctions.valueAndIndex val = StringFunctions.findBetween(result,"\">script_","</a>",index,false);
                    if(val.value!=null)set.add(val.value);
                    index = val.to;
                }
                showSelectPageToEdit(set.toArray(new String[set.size()]));
            }

            @Override
            public void onError() {
                showConnectionFailed();
            }
        }).execute(getString(R.string.link_repository)+"&do=index");
    }

    void showSelectPageToEdit(final String pages[]){
        String[] pageNames = new String[pages.length];
        for (int i=0;i<pages.length;i++)pageNames[i] = StringFunctions.getNameForPageFromPref(sharedPref,this,pages[i]).trim();
        new AlertDialog.Builder(this)
                .setSingleChoiceItems(pageNames,-1,new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        loadPageToEdit(pages[which]);
                    }
                })
                .setNegativeButton(R.string.button_cancel,null)
                .setTitle("Select page to edit")
                .show();
    }

    void loadPageToEdit(String id){
        pageId = id;
        new DownloadTask(new DownloadTask.Listener() {
            @Override
            public void onFinish(String result) {
                int index = result.indexOf("<textarea name=\"wikitext\"");
                String html = StringFunctions.findBetween(result,">","</textarea>",index,false).value;
                StringBuilder builder = new StringBuilder();
                String[] lines = html.split("\n");
                for(String s:lines){
                    builder.append(Html.fromHtml(s)).append("\n");
                }
                showPageEditor(builder.toString().trim());
            }

            @Override
            public void onError() {
                showConnectionFailed();
            }
        }).execute(getString(R.string.link_scriptPagePrefix)+id+"&do=edit");
    }

    void showPageEditor(String text){
        setContentView(R.layout.activity_edit);
        editor = (EditText)findViewById(R.id.editor);
        editor.setText(text);
    }

    public void cancelEdit(View v){
        setContentView(R.layout.activity_select_action);
    }

    public void savePage(View v){
        new DownloadTask(new DownloadTask.Listener() {
            @Override
            public void onFinish(String result) {
                Log.d("Tag","Finished");
            }

            @Override
            public void onError() {
                showConnectionFailed();
            }
        },true,"wikitext="+editor.getText()).execute(getString(R.string.link_scriptPagePrefix)+pageId+"&do=edit&rev=0&prefix=.&suffix=&sectok="+sessionToken+"&changecheck="+((int)(Math.random()*1000000))+"&target=section");
    }



    void showConnectionFailed() {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("Can't connect to server")
                .setNeutralButton(R.string.button_ok, null)
                .show();
    }
}
