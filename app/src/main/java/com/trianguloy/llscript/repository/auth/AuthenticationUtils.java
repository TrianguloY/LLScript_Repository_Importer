package com.trianguloy.llscript.repository.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.trianguloy.llscript.repository.R;

import org.acra.ACRA;

import java.io.IOException;

/**
 * Created by Lukas on 01.09.2015.
 * Method collection for the account
 */
public final class AuthenticationUtils {
    private AuthenticationUtils(){}

    public static void findAccount(final Activity context, final Listener listener, boolean passwordInvalid){
        final AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType(context.getString(R.string.account_type));
        AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    future.getResult();
                    Account[] accounts = accountManager.getAccountsByType(context.getString(R.string.account_type));
                    listener.onComplete(accounts[0].name,  accountManager.getPassword(accounts[0]));
                } catch (OperationCanceledException e) {
                    listener.onError();
                } catch (IOException | AuthenticatorException e) {
                    ACRA.getErrorReporter().handleException(e);
                    listener.onError();
                }
            }
        };
        if (accounts.length == 0) {
            accountManager.addAccount(context.getString(R.string.account_type), "", null, null, context, callback, null);
        } else if (accountManager.getPassword(accounts[0]) == null || passwordInvalid) {
            accountManager.updateCredentials(accounts[0], "", null, context, callback, null);
        } else listener.onComplete(accounts[0].name, accountManager.getPassword(accounts[0]));
    }

    public static void resetPassword(Context context){
        final AccountManager accountManager = AccountManager.get(context);
        final Account[] accounts = accountManager.getAccountsByType(context.getString(R.string.account_type));
        accountManager.clearPassword(accounts[0]);
    }

    public static boolean hasPassword(Context context){
        final AccountManager accountManager = AccountManager.get(context);
        final Account[] accounts = accountManager.getAccountsByType(context.getString(R.string.account_type));
        return accounts.length > 0 && accountManager.getPassword(accounts[0]) != null;
    }

    public interface Listener{
        void onComplete(String user, String password);
        void onError();
    }
}
