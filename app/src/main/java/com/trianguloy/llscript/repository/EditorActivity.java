package com.trianguloy.llscript.repository;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.trianguloy.llscript.repository.internal.AppChooser;
import com.trianguloy.llscript.repository.internal.StringFunctions;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import dw.xmlrpc.DokuJClient;
import dw.xmlrpc.Page;
import dw.xmlrpc.exception.DokuException;

/**
 * Created by Lukas on 20.04.2015.
 * Provides an UI to edit/create a script page
 * TODO edit: Preview & helpers (bold, list, usw)
 */
public class EditorActivity extends Activity {

    private SharedPreferences sharedPref;
    private String pageId;
    private EditText editor;
    private DokuJClient client;
    private Repository repository;
    private RepositoryCategory addTo;
    private String pageName;

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
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String repoText = client.getPage("script_repository");
                    String[] lines = repoText.split("\n");
                    repository = new Repository(lines);
                    repository.categories.add(new RepositoryCategory("None",-1,-1));
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
                    for (RepositoryCategory c : repository.categories)
                        Log.d("Categories",c.name);
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

    @SuppressWarnings("UnusedParameters")
    public void cancelEdit(View v){
        setContentView(R.layout.activity_select_action);
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
                        client.putPage("script_repository", TextUtils.join("\n", repository.lines));

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
        pageId = "script_"+((EditText)findViewById(R.id.editId)).getText();
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        final RepositoryCategory selected = ((RepositoryCategory)spinner.getSelectedItem());
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean exists;
                        try {
                            String page = client.getPage(pageId);
                            exists = page!=null && page != "";
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
                                text = client.getPage("script_template");
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

    private void showPageAlreadyExists() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(EditorActivity.this)
                        .setTitle("Error")
                        .setMessage("Page already exists. Please choose another ID")
                        .setNeutralButton(R.string.button_ok, null)
                        .show();
            }
        });
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

    class Repository{
        int tableStartLine;
        int tableEndLine;
        ArrayList<RepositoryCategory> categories;
        ArrayList<String> lines;

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
