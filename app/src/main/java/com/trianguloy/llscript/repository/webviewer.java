package com.trianguloy.llscript.repository;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
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

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;


public class webViewer extends Activity {

    private WebView webView; //webView element
    private Button button; //button element
    private SharedPreferences sharedPref;

    private Boolean close=false; //if pressing back will close
    private int id; //script manager id
    private String previousUrl="";//to avoid duplicated checks

    //Script data
    private String code = "";
    private String name = "Script Name";
    private int flags = 0;


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

            //properties assignation
            //webView.getSettings().setJavaScriptEnabled(true);
            WebViewClient webViewClient = new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    return false;
                }

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    if (!previousUrl.equals(url)) {
                        previousUrl = url;
                        //Check the page
                        onPageChange(url);
                    }

                }
            };
            webView.setWebViewClient(webViewClient);
            webView.loadUrl(Constants.pageMain);

        }


    }





    @SuppressWarnings({"unused","unusedParameter"}) // ?????
    public void buttonOnClick(View v) {
        //Download button clicked
        DownloadTask task = new DownloadTask();
        task.execute(webView.getUrl());
    }

    @SuppressWarnings({"unused","unusedParameter"}) // ?????
    public void buttonInjectFromTemplate(View v){
        //start the script injection process from template
        Intent intent = new Intent(this,ApplyTemplate.class);
        startActivity(intent);
        finish();
    }

    public void buttonInjectFromLauncher(View v){
        //start the script injection process from launcher
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setComponent(ComponentName.unflattenFromString(Constants.packageMain));
        intent.putExtra("a",7);
        startActivity(intent);
        finish();
    }






    void onPageChange(final String url){
        if(url.equals(Constants.pageMain)){
            //main page
            button.setVisibility(View.GONE);
        }else if( url.startsWith(Constants.pagePrefix)){
            // script page
            button.setVisibility(View.VISIBLE);
        } else {
            //external page
            webView.stopLoading();
            button.setVisibility(View.GONE);

            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle(getString(R.string.title_external_page));
            alertDialog.setMessage(getString(R.string.message_external_page));
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,getString(R.string.button_ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // here you can add functions
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




    void showAndConfirm(String html){
        //called from download task

        //initialize variables
        int beg =-1;
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(getString(R.string.title_importer));
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE,getString(R.string.button_exit), new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which) {/* */}});
        alertDialog.setIcon(R.drawable.ic_launcher);


        //search the code
        for (String aBeginning : Constants.beginning) {
            beg = html.indexOf(aBeginning);
            if (beg != -1) {
                beg += aBeginning.length();
                break;
            }
        }
        int end = html.indexOf(Constants.ending,beg);

        //TODO search the name

        //TODO search the flags


        if(beg!=-1){
            //found something

            //apply the finds
            String[] lines=html.substring(beg,end).split("\n");
            code ="";
            for (String line : lines) {
                code += Html.fromHtml(line).toString() + "\n";
            }
            name = webView.getUrl().substring(Constants.pagePrefix.length());
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
                        Toast.makeText(getApplicationContext(),"There was an error trying to pass the data to the manager",Toast.LENGTH_LONG).show();
                        return;
                    }
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setComponent(ComponentName.unflattenFromString(Constants.packageMain));
                    i.putExtra("a",35);
                    i.putExtra("d",id+"/"+data.toString());
                    startActivity(i);

                }
            });
        }else{
            //found nothing
            alertDialog.setMessage(getString(R.string.no_script_found));
        }

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
    public boolean onKeyDown(int keyCode,KeyEvent event) {
        //edited from http://stackoverflow.com/questions/6077141/how-to-go-back-to-previous-page-if-back-button-is-pressed-in-webview
        if(event.getAction() == KeyEvent.ACTION_DOWN){
            switch(keyCode)
            {
                case KeyEvent.KEYCODE_BACK:
                    if(id==Constants.notId) {
                        finish();
                        return true;
                    }
                    if(webView.canGoBack() && !webView.getUrl().equals(Constants.pageMain)){
                        webView.goBack();
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


    private class DownloadTask extends AsyncTask<String, Void, String> {
    //From http://stackoverflow.com/questions/16994777/android-get-html-from-web-page-as-string-with-httpclient-not-working
    @Override
    protected String doInBackground(String... urls) {
        HttpResponse response;
        HttpGet httpGet;
        HttpClient mHttpClient;
        String s = "";

        try {
            mHttpClient = new DefaultHttpClient();


            httpGet = new HttpGet(urls[0]);


            response = mHttpClient.execute(httpGet);
            s = EntityUtils.toString(response.getEntity(), "UTF-8");


        } catch (IOException e) {
            e.printStackTrace();
        }
        return s;
    }

    @Override
    protected void onPostExecute(String result){
        showAndConfirm(result);

    }
}

}
