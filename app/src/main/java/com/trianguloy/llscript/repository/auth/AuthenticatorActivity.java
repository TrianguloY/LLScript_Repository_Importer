package com.trianguloy.llscript.repository.auth;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.internal.AppChooser;
import com.trianguloy.llscript.repository.internal.Dialogs;

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
    public static final String ACCOUNT = "acc";


    public static String user;
    public static String password;

    private String accountType;
    private Account account;


    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_login);
        Intent intent = getIntent();
        accountType = intent.getStringExtra(ACCOUNT_TYPE);
        account = intent.getParcelableExtra(ACCOUNT);
        if(account!=null)((EditText) findViewById(R.id.username)).setText(account.name);

    }

    @SuppressWarnings("UnusedParameters")
    public void login(View v){
        final String user = ((EditText) findViewById(R.id.username)).getText().toString();
        final String password = ((EditText) findViewById(R.id.password)).getText().toString();
        final boolean savePw = ((CheckBox)findViewById(R.id.checkRemember)).isChecked();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DokuJClient client = new DokuJClient(getString(R.string.link_xmlrpc));
                    try {
                        //test if logged in
                        Object[] params = new Object[]{user, password};
                        boolean login = (boolean)client.genericQuery("dokuwiki.login", params);
                        if(login) {
                            AccountManager accountManager = AccountManager.get(AuthenticatorActivity.this);
                            if(account != null){
                                accountManager.setPassword(account,savePw?password:null);
                                if(!account.name.equals(user)) {
                                    if( Build.VERSION.SDK_INT >= 21) accountManager.renameAccount(account,user,null,null);
                                    else {
                                        accountManager.removeAccount(account,null,null);
                                        account = null;
                                    }
                                }
                            }
                            if(account ==  null) {
                                Account account = new Account(user, accountType);
                                accountManager.addAccountExplicitly(account, savePw ? password : null, null);
                            }
                            Intent intent = new Intent();
                            intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, user);
                            intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, ACCOUNT_TYPE);
                            setAccountAuthenticatorResult(intent.getExtras());
                            setResult(RESULT_OK, intent);
                            AuthenticatorActivity.user = user;
                            AuthenticatorActivity.password = password;
                            AuthenticatorActivity.this.finish();
                        }
                        else Dialogs.badLogin(AuthenticatorActivity.this);
                    }
                    catch (DokuUnauthorizedException e){
                        e.printStackTrace();
                        Dialogs.badLogin(AuthenticatorActivity.this);
                    }
                } catch (MalformedURLException | DokuException e) {
                    e.printStackTrace();
                    Dialogs.connectionFailed(AuthenticatorActivity.this);
                }
            }
        }).start();

    }


    @SuppressWarnings("UnusedParameters")
    public void register(View v) {
        new AppChooser(this, Uri.parse(getString(R.string.link_register)), getString(R.string.title_appChooserRegister), getString(R.string.message_noBrowser), null).show();
    }
}
