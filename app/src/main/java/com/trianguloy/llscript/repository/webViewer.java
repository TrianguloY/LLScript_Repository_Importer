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
import android.text.Html;
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

    private WebView webView; //webView element
    private Button button; //button element
    private ProgressBar progressBar;
    private SharedPreferences sharedPref;//user saved data (used to save the id of the script manager)

    private Boolean close = false; //if pressing back will close or not

    //Web view data
    private String repoHtml = "";//source code of the repository, used to get the name of the scripts
    private String currentHtml = "";//source code of the current page
    private String currentUrl = "";//The URL of the current page
    private Stack<String> backStack;//contains the history of the views pages
    private DownloadTask.Listener downloadTaskListener; //default downloadTaskListener



    //Application functions: onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //initialize variables
        sharedPref = getPreferences(Context.MODE_PRIVATE);
        Constants.id = sharedPref.getInt("id", Constants.notId);

        //Get the intent and data
        Intent intent = getIntent();
        int getid = (int) intent.getDoubleExtra("id", Constants.notId); //The returned id

        if (getid != Constants.notId && getid != Constants.id) {
            //new manager loaded
            sharedPref.edit().putInt("id", getid).apply();//id of the manager script
            Constants.id = getid;
            showLoadSuccessful();
            initializeWeb();

        }else if (intent.hasExtra("update")) {
            //The manager asks for the updated script
            sendUpdate();
            finish();

        }else if (Constants.id == Constants.notId) {
            //manager not loaded
            startActivity(new Intent(this,noManager.class));
            finish();

        }else
            //Normal activity
            initializeWeb();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_webviewer, menu);
        menu.findItem(R.id.action_id).setTitle("id: " + (Constants.id != Constants.notId ? Constants.id : "not found")).setEnabled(BuildConfig.DEBUG);
        menu.findItem(R.id.action_reset).setEnabled(BuildConfig.DEBUG);

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
                sharedPref.edit().remove("id").apply();
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
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public void onBackPressed(){
        if (!backStack.empty()) {
            //not on the home page
            currentUrl = backStack.pop();
            changePage(currentUrl);

        }else if (!close) {
            //Press back while the toast is still displayed to close
            Toast.makeText(getApplicationContext(), getString(R.string.message_back_to_close), Toast.LENGTH_SHORT).show();
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
        backStack = new Stack<>();
        currentUrl = Constants.pageMain;
        downloadTaskListener = new DownloadTask.Listener() {
            @Override
            public void onFinish(String result) {
                //default listener: show the page after loading it
                currentHtml = result;
                if(currentUrl.equals(Constants.pageMain)&&repoHtml=="")repoHtml=result;
                progressBar.setVisibility(View.GONE);
                display();
            }

            @Override
            public void onError(){
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
        //load and show the repository
        new DownloadTask(downloadTaskListener).execute(Constants.pageMain);
    }

    void showLoadSuccessful(){
        //notify user that import was successful. Run in onCreate when received the data
        new AlertDialog.Builder(this)
                .setTitle("")
                .setMessage(getString(R.string.message_manager_loaded))
                .setNeutralButton(getString(R.string.button_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
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
        if (url.equals(Constants.pageMain)) {
            //main page
            currentUrl = url;
            currentHtml = repoHtml;
            backStack.clear();
            display();
        }
        else if (url.startsWith(Constants.pagePrefix)) {
            // script page
            backStack.push(currentUrl);
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
            setTitle(getString(R.string.action_main_page));
        }else{
            button.setVisibility(View.VISIBLE);
            setTitle(currentUrl.substring(Constants.pagePrefix.length()));
            //Note: the name is the one of the URL (with _ ) to let the user
        }
    }

    void showExternalPageLinkClicked(final String url){
        //When the clicked page is not useful for this app
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_external_page))
                .setMessage(getString(R.string.message_external_page))
                .setPositiveButton(getString(R.string.button_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(url));
                        startActivity(i);
                    }
                })
                .setNegativeButton(getString(R.string.button_no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        /* */
                    }
                })
                .setIcon(R.drawable.ic_launcher)
                .show();
    }

    void showNoPageLoaded(final String url){
        //When the page couldn't be loaded
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_noPageFound))
                .setMessage(getString(R.string.message_noPageFound))
                .setPositiveButton(R.string.button_retry, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        new DownloadTask(downloadTaskListener).execute(url);
                    }
                })
                .setNegativeButton(getString(R.string.button_exit), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setIcon(R.drawable.ic_launcher)
                .show();
    }



    //Script importer
    @SuppressWarnings({"unused", "unusedParameter"})
    public void startImport(View v) {
        //Download button clicked

        //initialize variables
        int beg;
        final ArrayList<Integer> starts = new ArrayList<>();//start indexes of all scripts
        final ArrayList<Integer> ends = new ArrayList<>();//end indexes of all scripts
        final ArrayList<String> names = new ArrayList<>();//names of all scripts


        //search the code block start(s)
        for (String aBeginning : Constants.beginning) {
            beg = -1;
            do {
                if (beg != -1) starts.add(beg = beg + aBeginning.length());//save found
                beg = currentHtml.indexOf(aBeginning, beg);//search next
            } while (beg != -1);
        }

        //TODO search the flags


        if (starts.size() > 0) {
            //found something
            for (int begIndex : starts) {
                //search for the code block end(s)
                ends.add(currentHtml.indexOf(Constants.ending, begIndex));

                int endIndex=begIndex;
                int startIndex;
                String scriptName;

                //get name(s) from headers
                do {
                    endIndex = currentHtml.lastIndexOf("<", endIndex-1);
                    startIndex = currentHtml.lastIndexOf(">", endIndex) + 1;
                    scriptName = currentHtml.substring(startIndex, endIndex);
                } while (!scriptName.matches(".*\\w.*"));//Assumes the script name is just before the code
                names.add(scriptName);
            }
            if (starts.size() > 1)
                showMoreThanOneScriptFound(names.toArray(new String[names.size()]), starts.toArray(new Integer[starts.size()]), ends.toArray(new Integer[ends.size()]));
            else {
                oneScriptFound(names.get(0),starts.get(0),ends.get(0));
            }
        } else showNoScriptFound();
    }

    void oneScriptFound(String name,int start, int end){
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

        showImportScript(currentHtml.substring(start, end), scriptName);

    }

    void showMoreThanOneScriptFound(final String[] names,final Integer[] starts, final Integer[] ends){
        //More than one script found select one of them to import
        new AlertDialog.Builder(this)
                .setSingleChoiceItems(names, android.R.layout.simple_list_item_single_choice, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        showImportScript(currentHtml.substring(starts[which], ends[which]), names[which]);
                    }
                })
                .setNegativeButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setCancelable(true)
                .setTitle(getString(R.string.message_more_than_one_script))
                .setIcon(R.drawable.ic_launcher)
                .show();
    }

    void showImportScript(String rawCode, String scriptName) {
        //show the alert to import a single script
        String[] lines = rawCode.split("\n");
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(Html.fromHtml(line).toString()).append("\n");//Because Html.fromHtml() removes the line breaks
        }

        String code = new String(builder).trim();

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
                .setView(layout)
                .setTitle(getString(R.string.title_importer))
                .setNegativeButton(getString(R.string.button_exit), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {/* */}
                })
                .setIcon(R.drawable.ic_launcher)
                .setPositiveButton(getString(R.string.button_import), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        sendScriptToLauncher(contentText, nameText, flagsBoxes);
                    }
                })
                .setNeutralButton("send", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        shareAsText(contentText,nameText,flagsBoxes);
                    }
                })
                .show();
    }

    void showNoScriptFound(){
        //alert to show that no script is found
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_importer))
                .setNegativeButton(getString(R.string.button_exit), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {/* */}
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

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT,text.toString());
        startActivity(Intent.createChooser(share,"Send to..."));
    }

}
