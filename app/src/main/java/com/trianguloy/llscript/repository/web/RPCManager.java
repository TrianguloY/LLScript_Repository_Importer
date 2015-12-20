package com.trianguloy.llscript.repository.web;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.internal.Utils;
import com.trianguloy.llscript.repository.settings.Preferences;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import dw.xmlrpc.DokuJClient;
import dw.xmlrpc.Page;
import dw.xmlrpc.PageChange;
import dw.xmlrpc.exception.DokuException;

/**
 * Created by Lukas on 02.08.2015.
 * Performs all DokuJClient jobs
 */
public final class RPCManager {

    //internal return values in AsyncTasks
    public static final int RESULT_OK = 1;
    public static final int RESULT_NETWORK_ERROR = -1;
    public static final int RESULT_BAD_LOGIN = -2;
    public static final int RESULT_NEED_RW = -3;

    public static final int NOT_LOGGED_IN = 0;
    public static final int LOGIN_RO = 1;
    public static final int LOGIN_USER = 2;

    private static final int ACL_WRITE = 4;

    private static DokuJClient client;
    private static int login = NOT_LOGGED_IN;
    private static String username;

    private RPCManager() {
    }

    private static boolean init() {
        if (client == null) {
            try {
                client = new DokuJClient(Utils.getString(R.string.link_xmlrpc));
            } catch (MalformedURLException e) {
                //should never happen
                throw new RuntimeException("Unable to create client: Malformed Url", e);
            }
        }
        if (login < LOGIN_RO) {
            try {
                Object[] parameters = new Object[]{"remote_ro", "remote_ro"};
                login = ((boolean) client.genericQuery("dokuwiki.login", parameters)) ? LOGIN_RO : NOT_LOGGED_IN;
            } catch (DokuException e) {
                return false;
            }
        }
        return true;
    }

    public static int isLoggedIn() {
        return login;
    }

    public static void login(final String user, final String password, @Nullable Listener<Void> listener) {
        new ListenedTask<Void>(listener) {
            @NonNull
            @Override
            protected Result<Void> doInBackground(Void... params) {
                int result = RESULT_NETWORK_ERROR;
                try {
                    if (init()) {
                        Object[] parameters = new Object[]{user, password};
                        login = ((boolean) client.genericQuery("dokuwiki.login", parameters)) ? LOGIN_USER : NOT_LOGGED_IN;
                        if (login == LOGIN_USER) {
                            result = RESULT_OK;
                            username = user;
                        } else result = RESULT_BAD_LOGIN;
                    }
                } catch (DokuException e) {
                    e.printStackTrace();
                }
                return new Result<>(result);
            }
        }.execute();
    }

    public static void logout() {
        new AsyncTask<Void, Void, Void>() {

            @Nullable
            @Override
            protected Void doInBackground(Void... params) {
                if (login > NOT_LOGGED_IN) {
                    try {
                        client.logoff();
                        login = NOT_LOGGED_IN;
                    } catch (DokuException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        }.execute();
    }

    public static void getPage(final String id, Listener<String> listener) {
        new ListenedTask<String>(listener) {
            @NonNull
            @Override
            protected Result<String> doInBackground(Void... voids) {
                try {
                    if (init()) {
                        return new Result<>(RESULT_OK, client.getPage(id));
                    }
                } catch (DokuException e) {
                    e.printStackTrace();
                }
                return new Result<>(RESULT_NETWORK_ERROR);
            }
        }.execute();
    }

    public static void getAllPages(Listener<List<Page>> listener) {
        new ListenedTask<List<Page>>(listener) {
            @NonNull
            @Override
            protected Result<List<Page>> doInBackground(Void... voids) {
                try {
                    if (init()) {
                        return new Result<>(RESULT_OK, client.getAllPages());
                    }
                } catch (DokuException e) {
                    e.printStackTrace();
                }
                return new Result<>(RESULT_NETWORK_ERROR);
            }
        }.execute();
    }

    @NonNull
    public static String getUser() {
        return login == LOGIN_USER ? username : Utils.getString(R.string.text_defaultUser);
    }

    public static void putPage(final String id, final String text, Listener<Void> listener) {
        new ListenedTask<Void>(listener) {
            @NonNull
            @Override
            protected Result<Void> doInBackground(Void... voids) {
                int result = RESULT_NETWORK_ERROR;
                try {
                    if (init()) {
                        if (client.aclCheck(id) >= ACL_WRITE) {
                            client.putPage(id, text);
                            result = RESULT_OK;
                        } else result = RESULT_NEED_RW;
                    }
                } catch (DokuException e) {
                    e.printStackTrace();
                }
                return new Result<>(result);
            }
        }.execute();
    }

    public static void getChangedSubscriptions(@NonNull final Preferences sharedPref, Listener<List<String>> listener) {
        final int timestamp = sharedPref.getInt(Utils.getString(R.string.pref_timestamp), 0);
        final Set<String> subscriptions = sharedPref.getStringSet(Utils.getString(R.string.pref_subscriptions), Collections.<String>emptySet());
        if (subscriptions.size() > 0) new ListenedTask<List<String>>(listener) {
            @NonNull
            @Override
            protected Result<List<String>> doInBackground(Void... voids) {
                try {
                    if (init()) {
                        List<PageChange> changes = client.getRecentChanges(timestamp);
                        List<String> changedSubs = new ArrayList<>();
                        for (PageChange change : changes) {
                            String page = change.pageId();
                            if (page.startsWith(Utils.getString(R.string.prefix_script))) {
                                page = page.substring(Utils.getString(R.string.prefix_script).length());
                            }
                            if (subscriptions.contains(page)) {
                                changedSubs.add(page);
                            }
                        }
                        return new Result<>(RESULT_OK, changedSubs);
                    }
                } catch (DokuException e) {
                    e.printStackTrace();
                }
                return new Result<>(RESULT_NETWORK_ERROR);
            }
        }.execute();
    }

    public static void setTimestampToCurrent(@NonNull final Preferences sharedPref, Listener<Integer> listener) {
        new ListenedTask<Integer>(listener) {

            @NonNull
            @Override
            protected Result<Integer> doInBackground(Void... params) {
                try {
                    if (init()) {
                        int timestamp = client.getTime();
                        sharedPref.edit().putInt(Utils.getString(R.string.pref_timestamp), timestamp).apply();
                        return new Result<>(RESULT_OK, timestamp);
                    }
                } catch (DokuException e) {
                    e.printStackTrace();
                }
                return new Result<>(RESULT_NETWORK_ERROR);
            }
        }.execute();
    }

    public static AsyncTask getPageTimestamp(final String id, Listener<Integer> listener) {
        return new ListenedTask<Integer>(listener) {

            @NonNull
            @Override
            protected Result<Integer> doInBackground(Void... params) {
                try {
                    if (init()) {
                        return new Result<>(RESULT_OK, client.getPageInfo(id).version());
                    }
                } catch (DokuException e) {
                    e.printStackTrace();
                }
                return new Result<>(RESULT_NETWORK_ERROR);
            }
        }.execute();
    }


    public interface Listener<T> {
        void onResult(Result<T> result);
    }

    public static class Result<T> {
        private final int status;
        @Nullable
        private final T result;

        public Result(int status) {
            this(status, null);
        }

        public Result(int status, @Nullable T result) {
            this.status = status;
            this.result = result;
        }

        public int getStatus() {
            return status;
        }

        @Nullable
        public T getResult() {
            return result;
        }
    }

    private abstract static class ListenedTask<T> extends AsyncTask<Void, Void, Result<T>> {
        @Nullable
        private final Listener<T> listener;

        public ListenedTask(@Nullable Listener<T> listener) {
            super();
            this.listener = listener;
        }

        @Override
        protected final void onPostExecute(Result<T> result) {
            super.onPostExecute(result);
            if (listener != null) listener.onResult(result);
        }
    }

}
