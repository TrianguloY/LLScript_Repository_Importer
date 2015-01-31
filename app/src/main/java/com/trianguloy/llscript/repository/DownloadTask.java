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
    //From http://stackoverflow.com/questions/16994777/android-get-html-from-web-page-as-string-with-httpclient-not-working
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
            try {
                StringBuilder builder = new StringBuilder();
                byte[] buff = new byte[2048];
                BufferedInputStream input = new BufferedInputStream(connection.getInputStream());
                int count;
                while ((count = input.read(buff)) > 0) {
                    builder.append(new String(buff, 0, count));
                }
                return builder.toString().trim();
            } finally {
                connection.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        listener.onFinish(result);

    }

    public interface Listener {
        public void onFinish(String result);
    }
}