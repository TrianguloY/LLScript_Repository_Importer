package com.trianguloy.llscript.repository;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.trianguloy.llscript.repository.internal.AppChooser;
import com.trianguloy.llscript.repository.internal.Dialogs;
import com.trianguloy.llscript.repository.web.WebViewer;


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
                openWebViewer(getUrl, intent.getBooleanExtra(Constants.extraReload, false));
            } else if (uri != null) {
                //pass the bad intent to another app
                new AppChooser(this, uri, getString(R.string.title_appChooserBad), getString(R.string.toast_badString), new AppChooser.OnCloseListener() {
                    @Override
                    public void onClose() {
                        finish();
                    }
                }).show();
            } else if (intent.hasExtra(Constants.extraStatus)) {
                int status = (int) intent.getDoubleExtra(Constants.extraStatus, 0);
                switch (status) {
                    case Constants.STATUS_OK:
                        //loaded a script, return to page
                        openWebViewer();
                        break;
                    case Constants.STATUS_UPDATE_CONFIRMATION_REQUIRED:
                        Dialogs.confirmUpdate(this, intent.getStringExtra(Constants.ScriptName), intent.getStringExtra(Constants.ScriptCode), (int) intent.getDoubleExtra(Constants.ScriptFlags, 0));
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid status code returned from script: " + status);
                }
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.toast_badString), Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            //bad intent
            openWebViewer();
        }
    }

    public void openWebViewer() {
        openWebViewer(null, false);
    }

    private void openWebViewer(String url, boolean reload) {
        Intent intent = new Intent(this, WebViewer.class);
        if (url != null) intent.putExtra(Constants.extraOpenUrl, url);
        intent.putExtra(Constants.extraOpenUrlTime, System.currentTimeMillis());
        intent.putExtra(Constants.extraReload, reload);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        finish();
    }
}
