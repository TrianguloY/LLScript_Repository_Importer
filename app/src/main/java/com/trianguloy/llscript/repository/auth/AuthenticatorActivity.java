package com.trianguloy.llscript.repository.auth;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.internal.AppChooser;
import com.trianguloy.llscript.repository.internal.Dialogs;
import com.trianguloy.llscript.repository.web.RPCManager;

/**
 * Created by Lukas on 27.04.2015.
 * Shows UI to manage account
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity {

    public static final String ACCOUNT_TYPE = "accType";
    public static final String ACCOUNT = "acc";


    private String accountType;
    private Account account;


    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_login);
        Intent intent = getIntent();
        accountType = intent.getStringExtra(ACCOUNT_TYPE);
        account = intent.getParcelableExtra(ACCOUNT);
        if (account != null) ((EditText) findViewById(R.id.username)).setText(account.name);

    }

    public void button(@NonNull View v){
        switch (v.getId()){
            case R.id.button_login:
                login();
                break;
            case R.id.button_register:
                register();
                break;
        }
    }

    private void login() {
        final String user = ((EditText) findViewById(R.id.username)).getText().toString();
        final String password = ((EditText) findViewById(R.id.password)).getText().toString();
        final boolean savePw = ((CheckBox) findViewById(R.id.checkRemember)).isChecked();
        RPCManager.login(user, password, new RPCManager.Listener<Void>() {
            @Override
            public void onResult(@NonNull RPCManager.Result<Void> result) {
                switch (result.getStatus()) {
                    case RPCManager.RESULT_BAD_LOGIN:
                        Dialogs.badLogin(AuthenticatorActivity.this);
                        break;
                    case RPCManager.RESULT_NETWORK_ERROR:
                        Dialogs.connectionFailed(AuthenticatorActivity.this);
                        break;
                    case RPCManager.RESULT_OK:
                        setAccount(user, savePw ? password : null);
                        AuthenticatorActivity.this.finish();
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            }
        });
    }

    private void setAccount(@NonNull String user,@Nullable String password) {
        AuthenticationUtils.set(this, account, accountType, user, password);
        Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, user);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, ACCOUNT_TYPE);
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
    }

    private void register() {
        new AppChooser(this, Uri.parse(getString(R.string.link_register)), getString(R.string.title_appChooserRegister), getString(R.string.message_noBrowser), null).show();
    }
}
