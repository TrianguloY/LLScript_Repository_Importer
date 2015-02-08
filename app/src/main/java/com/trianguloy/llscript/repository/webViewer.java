package com.trianguloy.llscript.repository;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.http.HttpResponseCache;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.app.lukas.llscript.ScriptImporter;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Main activity: displays the webView used to load scripts
 */

public class webViewer extends Activity {

    //Elements
    private WebView webView; //webView element
    private Button button; //button element
    private ProgressBar progressBar;

    //User vars
    private SharedPreferences sharedPref;//user saved data (used to save the id of the script manager)
    private int id = -1;

    //Callbacks
    private Boolean close = false; //if pressing back will close or not
    private Boolean finish = false; //Used in onNewIntent as a callback

    //Web view data
    private String repoHtml = "";//source code of the repository, used to get the name of the scripts
    private String currentHtml = "";//source code of the current page
    private String currentUrl = "";//The URL of the current page
    private Stack<String> backStack;//contains the history of the views pages
    private DownloadTask.Listener downloadTaskListener=null; //default downloadTaskListener



    //Application functions
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //initialize variables
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        id = sharedPref.getInt(Constants.keyId, Constants.notId);
        backStack = new Stack<>();
        currentUrl = Constants.pageMain;

        //parse the Intent
        onNewIntent(getIntent());

        if(finish){
            finish();//Callback to finish the activity
            return;
        }

        if (id == Constants.notId) {
            //manager not loaded
            startActivity(new Intent(this, noManager.class));
            finish();
        }else{
            //Normal activity
            initializeWeb();
        }
    }

    @Override
    protected void onNewIntent(Intent intent){
        //manages the received intent, run automatically when the activity is running and is called again
        if (intent.hasExtra(Constants.extraId)) {
            int getId = (int) intent.getDoubleExtra(Constants.extraId, Constants.notId); //The returned id
            if(getId!=id){
                //new manager loaded
                sharedPref.edit().putInt(Constants.keyId, getId).apply();//id of the manager script
                id = getId;
                showLoadSuccessful();
            }
        }else if (intent.getBooleanExtra(Constants.extraUpdate, false)) {
            //The manager asks for the updated script
            sendUpdate();
        }else if (intent.getAction()!=null && intent.getAction().equalsIgnoreCase(Intent.ACTION_VIEW)){
            String getUrl=intent.getDataString();
            if(getUrl.startsWith(Constants.pagePrefix)){
                changePage(getUrl);
            }else{
                Toast.makeText(getApplicationContext(),getString(R.string.message_badString),Toast.LENGTH_LONG).show();
                moveTaskToBack(true);
                finish=true;
            }
        }

        setIntent(new Intent(this,webViewer.class));

        super.onNewIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_webviewer, menu);
        menu.findItem(R.id.action_id).setTitle("id: " + (id != Constants.notId ? id : "not found")).setVisible(BuildConfig.DEBUG);
        menu.findItem(R.id.action_reset).setVisible(BuildConfig.DEBUG);

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
                changePage(Constants.pageMain);
                break;
            case R.id.action_reset:
                //removes the saved id. For Debug purpose
                sharedPref.edit().remove(Constants.keyId).apply();
                finish();
                break;
            case R.id.action_linkPlayStore:
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(Constants.linkPlaystore));
                startActivity(i);
                break;
            case R.id.action_linkGooglePlus:
                Intent j = new Intent(Intent.ACTION_VIEW);
                j.setData(Uri.parse(Constants.linkGoogleplus));
                startActivity(j);
                break;
            case R.id.action_openInBrowser:
                Intent k = new Intent(Intent.ACTION_VIEW);
                k.setData(Uri.parse(currentUrl));
                startActivity(k);
                break;
            case R.id.action_attachAbout:
                    item.setChecked(!item.isChecked());
                    sharedPref.edit().putBoolean(Constants.keyAbout,item.isChecked()).apply();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public void onBackPressed(){
        if (!currentUrl.equals(Constants.pageMain)) {
            //not on the home page
            if(!backStack.empty()){
                currentUrl = backStack.pop();
                changePage(currentUrl);
            }else{
                changePage(Constants.pageMain);
            }

        }else if (!close) {
            //Press back while the toast is still displayed to close
            Toast.makeText(getApplicationContext(),R.string.message_back_to_close, Toast.LENGTH_SHORT).show();
            close = true;
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    //when the toast disappear
                    close = false;
                }
            }, 2000);//2000ms is the default time for the TOAST_LENGTH_SHORT
        }
        else
            finish();
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            finish();
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    protected void onStop() {
        if (Build.VERSION.SDK_INT >= 14) {
            HttpResponseCache cache = HttpResponseCache.getInstalled();
            if (cache != null) {
                cache.flush();
            }
        }
        super.onStop();
    }


    //Initialization
    void initializeWeb(){
        //Main Activity. Run on onCreate when normal launch
        setContentView(R.layout.activity_webviewer);

        //initialize vars
        button = (Button) findViewById(R.id.button);
        webView = (WebView) findViewById(R.id.webView);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        downloadTaskListener = new DownloadTask.Listener() {
            @Override
            public void onFinish(String result) {
                //default listener: show the page after loading it
                currentHtml = result;
                if(currentUrl.equals(Constants.pageMain)&& repoHtml.equals("")){
                    repoHtml=result;

                    //It detects if the page has changed since the last visit (the editing date is different)
                    String newHash = StringFunctions.findBetween(result,"<div class=\"docInfo\">","</div>",-1,false).value;
                    if(newHash!=null){

                        if(sharedPref.contains(Constants.keyRepoHash) && sharedPref.getInt(Constants.keyRepoHash,0) != newHash.hashCode()){
                            Toast.makeText(getApplicationContext(),R.string.message_repo_changed,Toast.LENGTH_SHORT).show();
                        }
                        sharedPref.edit().putInt(Constants.keyRepoHash,newHash.hashCode()).apply();
                    }

                }
                progressBar.setVisibility(View.GONE);
                display();
            }

            @Override
            public void onError(){
                progressBar.setVisibility(View.GONE);
                showNoPageLoaded(currentUrl);
            }
        };

        //noinspection deprecation
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (!currentUrl.equals(url)) {
                    //link clicked
                    changePage(url);
                }
                return true;
            }

            @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            @Override
            public WebResourceResponse shouldInterceptRequest(final WebView view, final String url) {
                //from http://stackoverflow.com/questions/12063937/can-i-use-the-android-4-httpresponsecache-with-a-webview-based-application/13596877#13596877
                if (Build.VERSION.SDK_INT<14 || !(url.startsWith("http://") || url.startsWith("https://")) || HttpResponseCache.getInstalled() == null) return null;
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
        });

        //install cache
        if (Build.VERSION.SDK_INT >= 14) {
            try {
                File httpCacheDir = new File(getCacheDir(), "http");
                long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
                HttpResponseCache.install(httpCacheDir, httpCacheSize);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        changePage(currentUrl);
    }

    void showLoadSuccessful(){
        //notify user that import was successful.
        new AlertDialog.Builder(this)
                .setTitle("")
                .setMessage(R.string.message_manager_loaded)
                .setNeutralButton(R.string.button_ok, null)
                .setIcon(R.drawable.ic_launcher)
                .show();
    }

    void sendUpdate(){
        Intent intent = new Intent(this,ScriptImporter.class);
        intent.putExtra("update",true);
        startService(intent);
    }



    //webView functions
    void changePage(String url) {
        //Change the page of the webView to the passed one
        if(downloadTaskListener==null){
            //The activity is not yet loaded. The url is kept as the currentUrl, so it gets loaded when the activity do so
            currentUrl=url;
            return;
        }

        if (url.equals(Constants.pageMain)) {
            //main page
            currentUrl = url;
            backStack.clear();

            if(repoHtml.equals("")){
                new DownloadTask(downloadTaskListener).execute(url);
            }else{
                currentHtml = repoHtml;
                display();
            }
        }
        else if (url.startsWith(Constants.pagePrefix)) {
            // script page
            if(!currentUrl.equals(url))backStack.push(currentUrl);
            currentUrl = url;
            progressBar.setVisibility(View.VISIBLE);
            new DownloadTask(downloadTaskListener).execute(url);
        }
        else
            //external page
            showExternalPageLinkClicked(url);
    }

    void display() {
        //display a page
        webView.loadDataWithBaseURL(Constants.pageRoot, currentHtml, "text/html", "utf-8", null);

        //ets the visibility of the button and the title of the app
        if(currentUrl.equals(Constants.pageMain)){
            button.setVisibility(View.GONE);
            setTitle(R.string.action_main_page);
        }else{
            button.setVisibility(View.VISIBLE);
            setTitle(currentUrl.substring(Constants.pagePrefix.length()));
            //Note: the name is the one of the URL (with _ ) to let the user
        }
    }

    void showExternalPageLinkClicked(final String url){
        //When the clicked page is not useful for this app
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_external_page)
                .setMessage(R.string.message_external_page)
                .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(url));
                        startActivity(i);
                    }
                })
                .setNegativeButton(R.string.button_no, null)
                .setIcon(R.drawable.ic_launcher)
                .show();
    }

    void showNoPageLoaded(final String url){
        //When the page couldn't be loaded
        progressBar.setVisibility(View.GONE);
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_noPageFound)
                .setMessage(R.string.message_noPageFound)
                .setPositiveButton(R.string.button_retry, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        progressBar.setVisibility(View.VISIBLE);
                        new DownloadTask(downloadTaskListener).execute(url);
                    }
                })
                .setNegativeButton(R.string.button_exit, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setIcon(R.drawable.ic_launcher)
                .setCancelable(false)
                .show();
    }



    //Script importer
    @SuppressWarnings({"unused", "unusedParameter"})
    public void startImport(View v) {
        //Download button clicked

        //initialize variables
        final ArrayList<String> names = new ArrayList<>();//names of all scripts
        final ArrayList<String> rawCodes = new ArrayList<>();//Found scripts
        String aboutScript;

        //Starts searching all scripts
        for(String aStart : Constants.beginning){
            //starting found
            StringFunctions.valueAndIndex found=new StringFunctions.valueAndIndex(null,-1,0);
            do{
                //searchs for a match
                found = StringFunctions.findBetween(currentHtml, aStart, Constants.ending, found.to, false);
                if(found.value!=null){
                    //if it is found, it adds it to the list
                    rawCodes.add(found.value.trim());
                    //Assumes the script name is just before the code, and searchs for it
                    StringFunctions.valueAndIndex name=new StringFunctions.valueAndIndex(null,found.from,-1);
                    do {
                        name=StringFunctions.findBetween(currentHtml,">","<",name.from,true);
                        if(name.value==null) {names.add("Name not found");break;}//In theory this will never get executed ... in theory
                        if(name.value.matches(".*\\w.*")){
                            //when it is found (if not it will return another text not related
                            names.add(name.value);
                            break;
                        }
                    } while (true);

                }else{
                    //if not found, another starting token
                    break;
                }
            }while(true);
        }

        //TODO search the flags

        //About script: purpose, author, link
        aboutScript=StringFunctions.findBetween(currentHtml,"id=\"about_the_script\">","</ul>",-1,false).value;
        if(aboutScript!=null){
            aboutScript=
                    "/* "+
                    aboutScript
                        .replaceAll("<[^>]*>","")//remove html tags
                        .trim()
                        .replaceAll("\n+","\n *  ")+//adds an asterisk at the beginning of each line & remove duplicated line breaks (all in one!)
                    "\n */\n\n";
        }

        //switch based on the number of scripts found
        if(rawCodes.size()>1){
            //more than one script founds
            showMoreThanOneScriptFound(names.toArray(new String[names.size()]),rawCodes.toArray(new String[rawCodes.size()]),aboutScript);
        }else if(rawCodes.size()>0){
            oneScriptFound(names.get(0),rawCodes.get(0),aboutScript);
        }else{
            showNoScriptFound();
        }
    }

    void oneScriptFound(String name, String rawCode, String about){
        //only one script, load directly

        //get the name from the repository
        String url = currentUrl;
        url = url.substring(url.indexOf("/", 10));//Why 10?
        int index = repoHtml.indexOf(url);
        String scriptName;
        if (index != -1) {
            scriptName = repoHtml.substring(repoHtml.indexOf(">", index) + 1, repoHtml.indexOf("<", index)).trim();
        } else
            //fallback if not found in repo
            scriptName = name;

        showImportScript(scriptName, rawCode, about);

    }

    void showMoreThanOneScriptFound(final String[] names, final String[] rawCodes, final String about){
        //More than one script found select one of them to import
        new AlertDialog.Builder(this)
                .setTitle(R.string.message_more_than_one_script)
                .setIcon(R.drawable.ic_launcher)
                .setSingleChoiceItems(names, android.R.layout.simple_list_item_single_choice, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();//Necessary, this is launched when clicking an item, not when clicking a button
                        showImportScript(names[which],rawCodes[which],about);
                    }
                })
                .setNegativeButton(R.string.button_cancel, null)
                .show();
    }

    void showImportScript(String scriptName, String rawCode,String aboutString) {
        //show the alert to import a single script
        String[] lines = rawCode.split("\n");
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(Html.fromHtml(line).toString()).append("\n");//Because Html.fromHtml() removes the line breaks
        }

        String code =new String(builder).trim();
        if( sharedPref.getBoolean(Constants.keyAbout,true) ) code=aboutString+code;

        //the alert dialog
        View layout = getLayoutInflater().inflate(R.layout.confirm_alert, (ViewGroup) findViewById(R.id.webView).getRootView(), false);
        final EditText contentText = ((EditText) layout.findViewById(R.id.editText2));
        contentText.setText(code);
        final EditText nameText = ((EditText) layout.findViewById(R.id.editText));
        nameText.setText(scriptName);
        final CheckBox[] flagsBoxes = {
                (CheckBox) layout.findViewById(R.id.checkBox),
                (CheckBox) layout.findViewById(R.id.checkBox2),
                (CheckBox) layout.findViewById(R.id.checkBox3)};

        new AlertDialog.Builder(this)
                .setTitle(R.string.title_importer)
                .setIcon(R.drawable.ic_launcher)
                .setView(layout)
                .setPositiveButton(R.string.button_import, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        sendScriptToLauncher(contentText, nameText, flagsBoxes);
                    }
                })
                .setNeutralButton(R.string.button_share, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        shareAsText(contentText,nameText,flagsBoxes);
                    }
                })
                .setNegativeButton(R.string.button_exit, null)
                .show();
    }

    void showNoScriptFound(){
        //alert to show that no script is found
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_importer)
                .setNegativeButton(R.string.button_exit,null)/* new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {}
                })*/
                .setPositiveButton(R.string.action_linkGooglePlus, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent j = new Intent(Intent.ACTION_VIEW);
                        j.setData(Uri.parse(Constants.linkGoogleplus));
                        startActivity(j);
                    }
                })
                .setIcon(R.drawable.ic_launcher)
                .setMessage(R.string.message_no_script_found)
                .show();
    }



    //Send & share functions
    void sendScriptToLauncher(EditText contentText, EditText nameText, CheckBox[] flagsBoxes) {
        // let's import the script
        final String code = contentText.getText().toString();
        final String scriptName = nameText.getText().toString();
        final int flags = (flagsBoxes[0].isChecked() ? Constants.FLAG_APP_MENU : 0) +
                (flagsBoxes[1].isChecked() ? Constants.FLAG_ITEM_MENU : 0) +
                (flagsBoxes[2].isChecked() ? Constants.FLAG_CUSTOM_MENU : 0);
        Intent intent = new Intent(this,ScriptImporter.class);
        intent.putExtra("code",code);
        intent.putExtra("name", scriptName);
        intent.putExtra("flags",flags);
        startService(intent);
    }

    void shareAsText(EditText contentText, EditText nameText, CheckBox[] flagsBoxes) {
        //share the code as plain text

        StringBuilder text = new StringBuilder("");

        //flags
        text.append("//Flags: ");
        if(flagsBoxes[0].isChecked())text.append("app ");
        if(flagsBoxes[1].isChecked())text.append("item ");
        if(flagsBoxes[2].isChecked())text.append("custom ");
        text.append("\n");

        //name
        text.append("//Name: ")
                .append(nameText.getText())
                .append("\n")
                .append(contentText.getText());

        text.append("\n");

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT,text.toString());
        startActivity(Intent.createChooser(share,"Send to..."));
    }

}
