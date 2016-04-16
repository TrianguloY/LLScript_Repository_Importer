package com.trianguloy.llscript.repository.web;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.settings.Preferences;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
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
public class RPCManager {

    //internal return values in AsyncTasks
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({RESULT_OK, RESULT_NETWORK_ERROR, RESULT_BAD_LOGIN, RESULT_NEED_RW})
    public @interface RPCResult {
    }

    public static final int RESULT_OK = 1;
    public static final int RESULT_NETWORK_ERROR = -1;
    public static final int RESULT_BAD_LOGIN = -2;
    public static final int RESULT_NEED_RW = -3;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({NOT_LOGGED_IN, LOGIN_RO, LOGIN_USER})
    public @interface RPCLoginState {
    }

    public static final int NOT_LOGGED_IN = 0;
    public static final int LOGIN_RO = 1;
    public static final int LOGIN_USER = 2;
    private static final int ACL_WRITE = 4;

    private static RPCManager instance;

    private Context context;
    private DokuJClient client;
    @RPCLoginState
    private int login = NOT_LOGGED_IN;
    private String username;

    private RPCManager(Context context) {
        this.context = context;
    }

    public static RPCManager getInstance(Context context) {
        if (instance == null) instance = new RPCManager(context);
        else instance.context = context;
        return instance;
    }

    private boolean init() {
        if (client == null) {
            try {
                client = new DokuJClient(context.getString(R.string.link_xmlrpc));
            } catch (MalformedURLException e) {
                //should never happen
                throw new RuntimeException("Unable to create client: Malformed Url", e);
            }
        }
        if (login < LOGIN_RO) {
            try {
                login = client.login("remote_ro", "remote_ro") ? LOGIN_RO : NOT_LOGGED_IN;
            } catch (DokuException e) {
                return false;
            }
        }
        return true;
    }

    @RPCLoginState
    public int isLoggedIn() {
        return login;
    }

    public void login(final String user, final String password, @Nullable Listener<Void> listener) {
        if(user == null || password == null) {
            if (listener != null) {
                listener.onResult(new Result<Void>(RESULT_BAD_LOGIN));
            }
            return;
        }
        new ListenedTask<Void>(listener) {
            @NonNull
            @Override
            protected Result<Void> doInBackground(Void... params) {
                int result = RESULT_NETWORK_ERROR;
                try {
                    if (init()) {
                        login = client.login(user, password) ? LOGIN_USER : NOT_LOGGED_IN;
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

    public void logout() {
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

    public void getPage(final String id, Listener<String> listener) {
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

    public void getAllPages(Listener<List<Page>> listener) {
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
    public String getUser() {
        return login == LOGIN_USER ? username : context.getString(R.string.text_defaultUser);
    }

    public void putPage(final String id, final String text, Listener<Void> listener) {
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

    public void getChangedSubscriptions(@NonNull final Context context, Listener<List<String>> listener) {
        Preferences sharedPref = Preferences.getDefault(context);
        final int timestamp = sharedPref.getInt(R.string.pref_timestamp, 0);
        final Set<String> subscriptions = sharedPref.getStringSet(R.string.pref_subscriptions, Collections.<String>emptySet());
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
                            if (page.startsWith(context.getString(R.string.prefix_script))) {
                                page = page.substring(context.getString(R.string.prefix_script).length());
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

    public void setTimestampToCurrent(@NonNull final Preferences sharedPref, Listener<Integer> listener) {
        new ListenedTask<Integer>(listener) {

            @NonNull
            @Override
            protected Result<Integer> doInBackground(Void... params) {
                try {
                    if (init()) {
                        int timestamp = client.getTime();
                        sharedPref.edit().putInt(R.string.pref_timestamp, timestamp).apply();
                        return new Result<>(RESULT_OK, timestamp);
                    }
                } catch (DokuException e) {
                    e.printStackTrace();
                }
                return new Result<>(RESULT_NETWORK_ERROR);
            }
        }.execute();
    }

    public AsyncTask getPageTimestamp(final String id, Listener<Integer> listener) {
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
        @RPCResult
        private final int status;
        @Nullable
        private final T result;

        public Result(@RPCResult int status) {
            this(status, null);
        }

        public Result(@RPCResult int status, @Nullable T result) {
            this.status = status;
            this.result = result;
        }

        @RPCResult
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
