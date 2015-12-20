package com.trianguloy.llscript.repository.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by Lukas on 27.04.2015.
 * Manages an account. UpdateCredentials should be implemented.
 */
class AccountAuthenticator extends AbstractAccountAuthenticator{

    @NonNull
    private final Context context;

    public AccountAuthenticator(@NonNull Context context) {
        super(context);
        this.context = context;
    }

    @Nullable
    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @NonNull
    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) {
        final Intent intent = new Intent(context, AuthenticatorActivity.class);
        intent.putExtra(AuthenticatorActivity.ACCOUNT_TYPE, accountType);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        return bundleIntent(intent);
    }

    @Nullable
    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) {
        return null;
    }

    @Nullable
    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) {
        return null;
    }

    @Nullable
    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return null;
    }

    @NonNull
    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, @NonNull Account account, String authTokenType, Bundle options) {
        final Intent intent = new Intent(context, AuthenticatorActivity.class);
        intent.putExtra(AuthenticatorActivity.ACCOUNT_TYPE, account.type);
        intent.putExtra(AuthenticatorActivity.ACCOUNT,account);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        return bundleIntent(intent);
    }

    @Nullable
    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) {
        return null;
    }

    @NonNull
    private static Bundle bundleIntent(Intent intent){
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }
}
