package com.trianguloy.llscript.repository.internal;

import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Lukas on 26.01.2015.
 * Requests a Html page and return it to the listener passed in the constructor
 */
public class DownloadTask extends AsyncTask<String, Void, String> {
    private final Listener listener;
    private final boolean usePost;

    public DownloadTask(Listener listener) {
        this(listener,false);
    }

    public DownloadTask(Listener listener, boolean usePost) {
        this.listener = listener;
        this.usePost = usePost;
    }

    @Override
    protected String doInBackground(String... urls) {
        HttpURLConnection connection;

        try {
            connection = (HttpURLConnection) new URL(urls[0]).openConnection();
            connection.setUseCaches(true);
            if(usePost)connection.setRequestMethod("POST");
            StringBuilder builder = new StringBuilder();
            try {
                byte[] buff = new byte[2048];
                BufferedInputStream input = new BufferedInputStream(connection.getInputStream());
                int count;
                while ((count = input.read(buff)) > 0) {
                    builder.append(new String(buff, 0, count));
                }
            } finally {
                connection.disconnect();
            }
            if (builder.length() > 0) return builder.toString().trim();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        if (result != null) {
            listener.onFinish(result);
        } else {
            listener.onError();
        }

    }

    public interface Listener {
        public void onFinish(String result);

        public void onError();
    }
}
