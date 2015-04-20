package com.trianguloy.llscript.repository;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.trianguloy.llscript.repository.internal.AppChooser;


public class IntentHandle extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        //manages the received intent, run automatically when the activity is running and is called again
        if (intent.getAction() != null && intent.getAction().equalsIgnoreCase(Intent.ACTION_VIEW)) {
            String getUrl = intent.getDataString();
            Uri uri = intent.getData();
            if (uri != null && getUrl.startsWith(getString(R.string.link_scriptPagePrefix))) {
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
        } else {
            //bad intent
            openWebViewer(null);
            finish();
        }
    }

    void openWebViewer(String url) {
        Intent intent = new Intent(this, webViewer.class);
        if (url != null) intent.putExtra(Constants.extraOpenUrl, url);
        intent.putExtra(Constants.extraOpenUrlTime, System.currentTimeMillis());
        startActivity(intent);
        finish();
    }
}
