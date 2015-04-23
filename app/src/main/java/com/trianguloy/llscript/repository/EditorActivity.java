package com.trianguloy.llscript.repository;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;

import com.trianguloy.llscript.repository.internal.AppChooser;
import com.trianguloy.llscript.repository.internal.StringFunctions;

import java.net.MalformedURLException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import dw.xmlrpc.DokuJClient;
import dw.xmlrpc.Page;
import dw.xmlrpc.exception.DokuException;

/**
 * Created by Lukas on 20.04.2015.
 * Provides an UI to edit/create a script page
 * TODO create: optionally automatically add to repository
 * TODO edit: Preview & helpers (bold, list, usw)
 */
public class EditorActivity extends Activity {

    private SharedPreferences sharedPref;
    private String pageId;
    private EditText editor;
    private DokuJClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.activity_login);
        ((EditText) findViewById(R.id.username)).setText(sharedPref.getString(getString(R.string.pref_user), ""));
        ((EditText) findViewById(R.id.password)).setText(sharedPref.getString(getString(R.string.pref_password), ""));
        ((CheckBox)findViewById(R.id.checkRemember)).setChecked(sharedPref.getBoolean(getString(R.string.pref_remindPassword),false));
    }

    @SuppressWarnings("UnusedParameters")
    public void login(View v) {
        final String user = ((EditText) findViewById(R.id.username)).getText().toString();
        final String password = ((EditText) findViewById(R.id.password)).getText().toString();
        boolean remember = ((CheckBox)findViewById(R.id.checkRemember)).isChecked();
        sharedPref.edit()
                .putString(getString(R.string.pref_user),user)
                .putBoolean(getString(R.string.pref_remindPassword),remember)
                .putString(getString(R.string.pref_password),remember?password:null)
                .apply();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                    client = new DokuJClient("http://www.pierrox.net/android/applications/lightning_launcher/wiki/lib/exe/xmlrpc.php",user,password);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setContentView(R.layout.activity_select_action);
                            }
                        });
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
        setContentView(R.layout.activity_create);
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
                        if(p.id().startsWith("script_"))pages.add(p);
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

    void showSelectPageToEdit(final Page[] pages){
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,R.layout.sub_list_item);
        for (Page p:pages)adapter.add(p.id());
        adapter.sort(new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                return StringFunctions.getNameForPageFromPref(sharedPref,EditorActivity.this,lhs).compareTo(StringFunctions.getNameForPageFromPref(sharedPref, EditorActivity.this, rhs));
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
                .setTitle("Select page to edit")
                .show();
    }

    void loadPageToEdit(String id){
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

    void showPageEditor(String text){
        setContentView(R.layout.activity_edit);
        editor = (EditText)findViewById(R.id.editor);
        editor.setText(text);
    }

    @SuppressWarnings("UnusedParameters")
    public void cancelEdit(View v){
        setContentView(R.layout.activity_select_action);
    }

    @SuppressWarnings("UnusedParameters")
    public void savePage(View v) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    client.putPage(pageId,editor.getText().toString());
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
        //TODO: Check if page already exists
        pageId = "script_"+((EditText)findViewById(R.id.editId)).getText();
        if(((CheckBox)findViewById(R.id.checkTemplate)).isChecked())
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final String text = client.getPage("script_template");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showPageEditor(text);
                            }
                        });
                    } catch (DokuException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        else showPageEditor("");
    }

    void showConnectionFailed() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(EditorActivity.this)
                        .setTitle("Error")
                        .setMessage("Can't connect to server")
                        .setNeutralButton(R.string.button_ok, null)
                        .show();
            }
        });
    }

    void showSaved(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(EditorActivity.this)
                        .setTitle("Saved")
                        .setMessage("What do you want to do now?")
                        .setPositiveButton("View Page",new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(getString(R.string.link_scriptPagePrefix)+pageId.substring(7)));
                                intent.setClass(EditorActivity.this,IntentHandle.class);
                                startActivity(intent);
                                finish();
                            }
                        })
                        .setNeutralButton("Go Home",new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(getString(R.string.link_repository)));
                                intent.setClass(EditorActivity.this, IntentHandle.class);
                                startActivity(intent);
                                finish();
                            }
                        })
                        .setNegativeButton("Stay",null)
                        .show();
            }
        });
    }
}
