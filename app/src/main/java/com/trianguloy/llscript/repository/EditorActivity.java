package com.trianguloy.llscript.repository;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.trianguloy.llscript.repository.internal.AppChooser;
import com.trianguloy.llscript.repository.internal.DownloadTask;
import com.trianguloy.llscript.repository.internal.StringFunctions;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by Lukas on 20.04.2015.
 * Provides an UI to edit/create a script page
 */
public class EditorActivity extends Activity {

    private SharedPreferences sharedPref;
    private String sessionToken;

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
                    Log.d("Login Result", String.valueOf(result.contains("Logged in as")));
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

    void showConnectionFailed() {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("Can't connect to server")
                .setNeutralButton(R.string.button_ok, null)
                .show();
    }
}
