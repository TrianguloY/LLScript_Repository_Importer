package com.trianguloy.llscript.repository;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


public class IntentHandle extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intent_handle);
        Intent intent=getIntent();
        Log.d("IntentHandler","New intent"+intent.toString());

        //manages the received intent, run automatically when the activity is running and is called again
        if (intent.getAction() != null && intent.getAction().equalsIgnoreCase(Intent.ACTION_VIEW)) {
            String getUrl = intent.getDataString();
            Uri uri = intent.getData();
            if (getUrl.startsWith(getString(R.string.link_scriptPagePrefix))) {
                openWebViewer(getUrl);
            } else if (uri != null) {
                //pass the bad intent to another app
                new AppChooser(this, uri, getString(R.string.title_appChooserBad), getString(R.string.toast_badString), new AppChooser.OnCloseListener() {
                    @Override
                    public void onClose() {
                        finish();
                    }
                }).show();
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.toast_badString), Toast.LENGTH_LONG).show();
                finish();
            }
        }else{
            //bad intent
            openWebViewer(null);
            finish();
        }
    }

    public void openWebViewer(String url){
        Intent intent = new Intent(this,webViewer.class);
        if(url!=null)intent.putExtra(Constants.extraOpenUrl,url);
        intent.putExtra(Constants.extraOpenUrlTime,System.currentTimeMillis());
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_intent_handle, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
