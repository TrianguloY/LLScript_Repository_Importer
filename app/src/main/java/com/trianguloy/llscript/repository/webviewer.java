package com.trianguloy.llscript.repository;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.http.HttpResponseCache;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.app.lukas.template.ApplyTemplate;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;


public class webViewer extends Activity {

    private WebView webView; //webView element
    private Button button; //button element
    private SharedPreferences sharedPref;

    private Boolean close=false; //if pressing back will close
    private int id; //script manager id

    //Script data
    private String code = "";
    private String name = "Script Name";
    private int flags = 0;

    private String repoHtml = "";
    private String currentHtml = "";

    //page management values
    private DownloadTask.Listener downloadTaskListener;
    private Stack<String> BackStack;
    private String currentUrl = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //initialize variables
        sharedPref= getPreferences(Context.MODE_PRIVATE);
        id= sharedPref.getInt("id", Constants.notId);


        //Get the intent and data
        Intent intent=getIntent();
        int getId = (int) intent.getDoubleExtra("id", Constants.notId); //-1=error other=ScriptId  TODO better returned code


        if(getId!=Constants.notId && getId!=id){
            //new manager loaded
            sharedPref.edit().putInt("id",getId).apply();//id of the manager script
            id=getId;
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("");
            alertDialog.setMessage(getString(R.string.manager_loaded));
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,getString(R.string.button_ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                 }
            });
            alertDialog.setIcon(R.drawable.ic_launcher);
            alertDialog.show();


        }

        //Application opened from icon
        if(id==Constants.notId){
            //manager not loaded
            setContentView(R.layout.managernotfound);
            //TODO put this as a new activity
        }else {
            //normal activity
            setContentView(R.layout.activity_webviewer);

            //initialize vars
            button = (Button) findViewById(R.id.button);
            webView = (WebView) findViewById(R.id.webView);

            if(Build.VERSION.SDK_INT>=14) {
                try {
                    File httpCacheDir = new File(getCacheDir(), "http");
                    long httpCacheSize = 5 * 1024 * 1024; // 5 MiB
                    HttpResponseCache.install(httpCacheDir, httpCacheSize);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            BackStack = new Stack<>();
            currentUrl = Constants.pageMain;

            //properties assignation
            //webView.getSettings().setJavaScriptEnabled(true);
            WebViewClient webViewClient = new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    //link clicked
                    if (!currentUrl.equals(url)) {
                        BackStack.push(currentUrl);
                        currentUrl = url;
                        changePage(url);
                    }
                    return true;
                }
            };
            webView.setWebViewClient(webViewClient);
            //webView.loadUrl(Constants.pageMain);

            //TODO: Merge loadUrl(pageMain) and RepoDownloadTask to reduce network usage

            //load and show the repository
            new DownloadTask(new DownloadTask.Listener() {
                @Override
                public void onFinish(String result) {
                    repoHtml = result;
                    currentHtml = repoHtml;
                    display();
                }
            }).execute(Constants.pageMain);

            //default listener: show the page after loading it
            downloadTaskListener = new DownloadTask.Listener() {
                @Override
                public void onFinish(String result) {
                    currentHtml = result;
                    display();
                }
            };

        }


    }

    //display a page
    void display(){
        webView.loadDataWithBaseURL(Constants.pageRoot, currentHtml, "text/html", "utf-8", null);
    }

    protected void onStop() {
        if(Build.VERSION.SDK_INT>=14) {
            HttpResponseCache cache = HttpResponseCache.getInstalled();
            if (cache != null) {
                cache.flush();
            }
        }
    }



    @SuppressWarnings({"unused","unusedParameter"})
    public void buttonOnClick(View v) {
        //Download button clicked
        DownloadTask task = new DownloadTask(new DownloadTask.Listener(){
            @Override
            public void onFinish(String result) {
                showAndConfirm(result);
            }
        });
        task.execute(webView.getUrl());
    }

    @SuppressWarnings({"unused","unusedParameter"})
    public void buttonInjectFromTemplate(View v){
        //start the script injection process from template
        Intent intent = new Intent(this,ApplyTemplate.class);
        startActivity(intent);
        finish();
    }

    @SuppressWarnings({"unused","unusedParameter"})
    public void buttonInjectFromLauncher(View v){
        //start the script injection process from launcher
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setComponent(ComponentName.unflattenFromString(Constants.packageMain));
        intent.putExtra("a",7);
        startActivity(intent);
        finish();
    }






    void changePage(final String url){
        if(url.equals(Constants.pageMain)){
            //main page
            button.setVisibility(View.GONE);
            currentHtml = repoHtml;
            display();
        }else if( url.startsWith(Constants.pagePrefix)){
            // script page
            button.setVisibility(View.VISIBLE);
            new DownloadTask(downloadTaskListener).execute(url);
        } else {
            //external page
            webView.stopLoading();
            button.setVisibility(View.GONE);

            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle(getString(R.string.title_external_page));
            alertDialog.setMessage(getString(R.string.message_external_page));
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,getString(R.string.button_ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                }
            });
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE,getString(R.string.button_no), new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which) {/* */}});
            alertDialog.setIcon(R.drawable.ic_launcher);
            alertDialog.show();

        }
    }




    void showAndConfirm(final String html){
        //called from download task

        //initialize variables
        int beg;
        final ArrayList<Integer> starts = new ArrayList<>();//start indexes of all scripts
        final ArrayList<Integer> ends = new ArrayList<>();//end indexes of all scripts
        final ArrayList<String> names = new ArrayList<>();//names of all scripts

        //alertDialog to import a script
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(getString(R.string.title_importer));
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE,getString(R.string.button_exit), new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which) {/* */}});
        alertDialog.setIcon(R.drawable.ic_launcher);


        //search the code block start(s)
        for (String aBeginning : Constants.beginning) {
            String temp = html;
            beg = temp.indexOf(aBeginning);
            int offset = 0;
            while(beg != -1) {
                beg += aBeginning.length();
                starts.add(beg + offset);
                temp = temp.substring(beg + 1);
                offset += beg + 1;
                beg = temp.indexOf(aBeginning);
            }
        }

        //TODO search the flags


        if(starts.size()>0){
            //found something
            for(int i=0;i<starts.size();i++){
                //search for the code block end(s)
                ends.add(html.substring(starts.get(i)).indexOf(Constants.ending)+starts.get(i));
                //get name(s) from headers
                int endIndex = starts.get(i);
                int startIndex;
                String scriptName;
                do {
                    endIndex = html.substring(0, endIndex).lastIndexOf("<");
                    startIndex = html.substring(0,endIndex).lastIndexOf(">")+1;
                    scriptName = html.substring(startIndex,endIndex);
                }while (!scriptName.matches(".*\\w.*"));
                names.add(scriptName);
            }
            if(starts.size()>1){
                //select one of the scripts to import
                new AlertDialog.Builder(this)
                        .setSingleChoiceItems(names.toArray(new String[names.size()]),android.R.layout.simple_list_item_single_choice,new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.dismiss();
                                downloadScript(html.substring(starts.get(which),ends.get(which)),names.get(which),alertDialog);
                            }
                        })
                        .setNegativeButton(getString(R.string.cancel),new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .setCancelable(true)
                        .setTitle(getString(R.string.more_than_one_script))
                        .setIcon(R.drawable.ic_launcher)
                        .show();
            }
            //only one script, load directly
            else {
                //get the name from the repository
                String url = webView.getUrl();
                url = url.substring(url.indexOf("/")+2);
                url = url.substring(url.indexOf("/"));
                int index = repoHtml.indexOf(url);
                String scriptName;
                if(index!=-1) {
                    String temp = repoHtml.substring(index);
                    scriptName = temp.substring(temp.indexOf(">") + 1, temp.indexOf("<")).trim();
                }
                else scriptName = names.get(0);
                downloadScript(html.substring(starts.get(0),ends.get(0)),scriptName,alertDialog);
            }
        }else{
            //found nothing
            alertDialog.setMessage(getString(R.string.no_script_found));
            alertDialog.show();
        }

    }

    void downloadScript(String rawCode,String scriptName,AlertDialog alertDialog){
        //apply the finds
        String[] lines=rawCode.split("\n");
        code ="";
        for (String line : lines) {
            code += Html.fromHtml(line).toString() + "\n";
        }
        name = scriptName;
        flags = 0;

        //the alert
        View layout = getLayoutInflater().inflate(R.layout.confirm_alert, (ViewGroup)findViewById(R.id.webView).getRootView(),false);
        final EditText contentText = ((EditText) layout.findViewById(R.id.editText2));
        contentText.setText(code);
        final EditText nameText = ((EditText) layout.findViewById(R.id.editText));
        nameText.setText(name);
        alertDialog.setView(layout);
        final CheckBox[] flagsBoxes = {
                (CheckBox)layout.findViewById(R.id.checkBox),
                (CheckBox)layout.findViewById(R.id.checkBox2),
                (CheckBox)layout.findViewById(R.id.checkBox3)};

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,getString(R.string.button_import), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // let's import the script
                code =contentText.getText().toString();
                name=nameText.getText().toString();
                flags=(flagsBoxes[0].isChecked()?Constants.FLAG_APP_MENU:0)+
                        (flagsBoxes[1].isChecked()?Constants.FLAG_ITEM_MENU:0)+
                        (flagsBoxes[2].isChecked()?Constants.FLAG_CUSTOM_MENU:0);
                JSONObject data = new JSONObject();
                try {
                    data.put("version",Constants.managerVersion);
                    data.put("code",code);
                    data.put("name",name);
                    data.put("flags",flags);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),getString(R.string.manager_error_message),Toast.LENGTH_LONG).show();
                    return;
                }
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setComponent(ComponentName.unflattenFromString(Constants.packageMain));
                i.putExtra("a",35);
                i.putExtra("d",id+"/"+data.toString());
                startActivity(i);
            }
        });
        alertDialog.show();
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if(id==Constants.notId) return true;
        getMenuInflater().inflate(R.menu.menu_webviewer, menu);
        menu.findItem(R.id.action_id).setTitle("Id: "+ (id!=Constants.notId?id:"not found"));
        menu.findItem(R.id.action_reset).setEnabled(BuildConfig.DEBUG);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()){
            case R.id.action_mainPage:
                webView.loadUrl(Constants.pageMain);
                break;
            case R.id.action_reset:
                sharedPref.edit().remove("id").apply();
                finish();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        //edited from http://stackoverflow.com/questions/6077141/how-to-go-back-to-previous-page-if-back-button-is-pressed-in-webview
        if(event.getAction() == KeyEvent.ACTION_DOWN){
            switch(keyCode)
            {
                case KeyEvent.KEYCODE_BACK:
                    if(id==Constants.notId) {
                        finish();
                        return true;
                    }
                    if(!BackStack.empty())//not on the home page
                    {
                        currentUrl = BackStack.pop();
                        Log.d("Back", currentUrl);
                        changePage(currentUrl);
                    }else{
                        if(close){
                            finish();
                        }else{
                            Toast.makeText(getApplicationContext(), getString(R.string.back_to_close), Toast.LENGTH_SHORT).show();
                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    // this code will be executed after 2 seconds
                                    close=false;
                                }
                            }, 2000);
                            close=true;
                        }

                    }
                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }


}
