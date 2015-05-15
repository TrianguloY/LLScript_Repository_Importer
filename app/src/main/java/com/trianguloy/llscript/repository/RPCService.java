package com.trianguloy.llscript.repository;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.net.MalformedURLException;
import java.util.List;

import dw.xmlrpc.DokuJClient;
import dw.xmlrpc.Page;
import dw.xmlrpc.exception.DokuException;
import dw.xmlrpc.exception.DokuUnauthorizedException;

/**
 * Created by Lukas on 07.05.2015.
 * Manages connection to the server
 */
public class RPCService extends Service {

    private boolean login;
    private DokuJClient client;
    private String username;

    @Override
    public void onCreate() {
        super.onCreate();
        login = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("service","Started");
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("service","bound");
        return new LocalBinder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("Service","stop");
    }

    public boolean isLoggedIn(){
        return login;
    }

    public void login(final String user, final String password, @Nullable final Listener<Integer> listener ){
        username = user;
        Log.d("user", user);
        Log.d("password",password);
        new AsyncTask<Void,Void,Integer>(){
            @Override
            protected Integer doInBackground(Void... params) {
                int result;
                try {
                    client = new DokuJClient(getString(R.string.link_xmlrpc));
                    try {
                        //test if logged in
                        Object[] parameters = new Object[]{user, password};
                        login = (boolean) client.genericQuery("dokuwiki.login", parameters);
                        if (login) result = Constants.RESULT_OK;
                        else result = Constants.RESULT_BAD_LOGIN;
                    } catch (DokuUnauthorizedException e) {
                        e.printStackTrace();
                        result = Constants.RESULT_BAD_LOGIN;
                    }
                } catch (MalformedURLException | DokuException e) {
                    e.printStackTrace();
                    result = Constants.RESULT_NETWORK_ERROR;
                }
                return result;
            }
                @Override
                protected void onPostExecute(Integer integer) {
                    if(listener!=null)listener.onResult(integer);
                    }
            }.execute();
    }

    public String getUser(){
        return username;
    }

    public void getPage(final String id, final Listener<String> listener){
        if(!login) throw new UnauthorizedException();
        new AsyncTask<Void,Void,String>(){
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    return client.getPage(id);
                } catch (DokuException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                listener.onResult(s);
            }
        }.execute();
    }

    public void getAllPages(final Listener<List<Page>> listener){
        if(!login) throw new UnauthorizedException();
        new AsyncTask<Void,Void,List<Page>>(){
            @Override
            protected List<Page> doInBackground(Void... voids) {
                try {
                    return client.getAllPages();
                } catch (DokuException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<Page> pages) {
                super.onPostExecute(pages);
                listener.onResult(pages);
            }
        }.execute();
    }

    public void putPage(final String id, final String text,@Nullable final Listener<Boolean> listener){
        if(!login) throw new UnauthorizedException();
        new AsyncTask<Void,Void,Boolean>(){
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    client.putPage(id,text);
                    return true;
                } catch (DokuException e) {
                    e.printStackTrace();
                    return false;
                }
            }
            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if(listener!=null)listener.onResult(result);
            }
        }.execute();
    }



    public class LocalBinder extends Binder {
        public RPCService getService() {
            return RPCService.this;
        }
    }

    public interface Listener<T> {
        void onResult(@ Nullable T result);
    }

    private static class UnauthorizedException extends RuntimeException{}


}
