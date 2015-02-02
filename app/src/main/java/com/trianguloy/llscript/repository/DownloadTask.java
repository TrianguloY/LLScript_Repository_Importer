package com.trianguloy.llscript.repository;

import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Lukas on 26.01.2015.
 * Requests a Html page and return it to the listener passed in the constructor
 */
class DownloadTask extends AsyncTask<String, Void, String> {
    private final Listener listener;

    public DownloadTask(Listener listener) {
        this.listener = listener;
    }

    @Override
    protected String doInBackground(String... urls) {
        HttpURLConnection connection;

        try {
            connection = (HttpURLConnection) new URL(urls[0]).openConnection();
            connection.setUseCaches(true);
            StringBuilder builder = new StringBuilder();
            try {
                byte[] buff = new byte[2048];
                BufferedInputStream input = new BufferedInputStream(connection.getInputStream());
                int count;
                while ((count = input.read(buff)) > 0) {
                    builder.append(new String(buff, 0, count));
                }
            } finally {//Is necessary a 'finally' statement here? 
                connection.disconnect();
                if(builder.length()==0)return null;
                return builder.toString().trim();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        if(result!=null){
            listener.onFinish(result);
        }else{
            listener.onError();
        }

    }

    public interface Listener {
        public void onFinish(String result);
        public void onError();//Note to Lukas from TrianguloY: I think I made this 'no page found' right, but all this code is advanced, please check it and change it if necessary
    }
}
