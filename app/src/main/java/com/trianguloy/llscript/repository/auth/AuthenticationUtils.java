package com.trianguloy.llscript.repository.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.internal.Dialogs;
import com.trianguloy.llscript.repository.web.RPCManager;

import org.acra.ACRA;

import java.io.IOException;

/**
 * Created by Lukas on 01.09.2015.
 * Method collection for the account
 */
public final class AuthenticationUtils {
    private AuthenticationUtils() {
    }

    public static void login(Activity context, Listener listener){
        login(context,listener,false);
    }

    private static void login(final Activity context, final Listener listener, boolean passwordInvalid){
        findAccount(context, new InternalListener() {
            @Override
            public void onComplete(String user, String password) {
                RPCManager.login(user, password, new RPCManager.Listener<Void>() {

                    @Override
                    public void onResult(RPCManager.Result<Void> result) {
                        switch (result.getStatus()) {
                            case RPCManager.RESULT_BAD_LOGIN:
                                Dialogs.badLogin(context, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        login(context,listener,true);
                                    }
                                });
                                break;
                            case RPCManager.RESULT_NETWORK_ERROR:
                                Dialogs.connectionFailed(context, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        listener.onError();
                                    }
                                });
                                break;
                            case RPCManager.RESULT_OK:
                                listener.onComplete();
                                break;
                            default:
                                throw new IllegalArgumentException();
                        }
                    }
                });
            }

            @Override
            public void onError() {

            }
        }, passwordInvalid);
    }

    private static void findAccount(@NonNull final Activity context, @NonNull final InternalListener listener, boolean passwordInvalid) {
        final AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType(context.getString(R.string.account_type));
        AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    future.getResult();
                    Account[] accounts = accountManager.getAccountsByType(context.getString(R.string.account_type));
                    listener.onComplete(accounts[0].name, accountManager.getPassword(accounts[0]));
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

    public static void resetPassword(@NonNull Context context) {
        final AccountManager accountManager = AccountManager.get(context);
        final Account[] accounts = accountManager.getAccountsByType(context.getString(R.string.account_type));
        accountManager.clearPassword(accounts[0]);
    }

    public static boolean hasPassword(@NonNull Context context) {
        final AccountManager accountManager = AccountManager.get(context);
        final Account[] accounts = accountManager.getAccountsByType(context.getString(R.string.account_type));
        return accounts.length > 0 && accountManager.getPassword(accounts[0]) != null;
    }

    public static void set(@NonNull Context context, @Nullable Account account, @NonNull String accountType, @NonNull String user, @Nullable String password) {
        AccountManager accountManager = AccountManager.get(context);
        if (account != null) {
            accountManager.setPassword(account, password);
            if (!account.name.equals(user)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    accountManager.renameAccount(account, user, null, null);
                else {
                    //noinspection deprecation
                    accountManager.removeAccount(account, null, null);
                    account = null;
                }
            }
        }
        if (account == null) {
            account = new Account(user, accountType);
            accountManager.addAccountExplicitly(account, password, null);
        }
    }

    public interface Listener {
        void onComplete();

        void onError();
    }

    private interface InternalListener {
        void onComplete(String user, String password);
        void onError();
    }
}
