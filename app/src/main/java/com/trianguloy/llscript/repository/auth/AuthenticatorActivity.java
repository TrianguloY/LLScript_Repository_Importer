package com.trianguloy.llscript.repository.auth;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.internal.AppChooser;

import java.net.MalformedURLException;

import dw.xmlrpc.DokuJClient;
import dw.xmlrpc.exception.DokuException;
import dw.xmlrpc.exception.DokuUnauthorizedException;

/**
 * Created by Lukas on 27.04.2015.
 * Shows UI to manage account
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity {

    public static final String ACCOUNT_TYPE = "accType";
    //public static final String AUTH_TYPE = "authType";


    private String accountType;


    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_login);
        accountType = getIntent().getStringExtra(ACCOUNT_TYPE);
    }

    @SuppressWarnings("UnusedParameters")
    public void login(View v){
        final String user = ((EditText) findViewById(R.id.username)).getText().toString();
        final String password = ((EditText) findViewById(R.id.password)).getText().toString();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DokuJClient client = new DokuJClient(getString(R.string.link_xmlrpc));
                    try {
                        //test if logged in
                        Object[] params = new Object[]{user, password};
                        Object login = client.genericQuery("dokuwiki.login", params);
                        Log.d("Auth",login.toString());
                        if(login!=null) {
                            Account account = new Account(user,accountType);
                            AccountManager.get(AuthenticatorActivity.this).addAccountExplicitly(account,password,null);
                            Intent intent = new Intent();
                            intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, user);
                            intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, ACCOUNT_TYPE);
                            setAccountAuthenticatorResult(intent.getExtras());
                            setResult(RESULT_OK, intent);
                            AuthenticatorActivity.this.finish();
                            Log.d("Act", "done");
                        }
                        else showBadLogin();
                    }
                    catch (DokuUnauthorizedException e){
                        e.printStackTrace();
                        showBadLogin();
                    }
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


    private void showBadLogin() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(AuthenticatorActivity.this)
                        .setTitle(getString(R.string.title_error))
                        .setMessage(getString(R.string.text_badLogin))
                        .setNeutralButton(R.string.button_ok, null)
                        .show();
            }
        });
    }

    private void showConnectionFailed() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(AuthenticatorActivity.this)
                        .setTitle(getString(R.string.title_error))
                        .setMessage(getString(R.string.text_cantConnect))
                        .setNeutralButton(R.string.button_ok, null)
                        .show();
            }
        });
    }
}
