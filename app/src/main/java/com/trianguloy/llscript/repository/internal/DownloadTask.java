package com.trianguloy.llscript.repository.internal;

import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Lukas on 26.01.2015.
 * Requests a Html page and return it to the listener passed in the constructor
 */
public class DownloadTask extends AsyncTask<String, Void, String> {
    private final Listener listener;

    public DownloadTask(Listener listener) {
        super();
        this.listener = listener;
    }


    @Override
    protected String doInBackground(String... urls) {
        HttpURLConnection connection;

        try {
            connection = (HttpURLConnection) new URL(urls[0]).openConnection();
            connection.setUseCaches(true);
            StringBuilder builder = new StringBuilder();
            BufferedInputStream input = null;
            InputStream inputStream = null;
            int count;
            try {
                byte[] buff = new byte[2048];
                inputStream = connection.getInputStream();
                input = new BufferedInputStream(inputStream);
                while ((count = input.read(buff)) > 0) {
                    builder.append(new String(buff, 0, count,"UTF-8"));
                }
            } finally {
                connection.disconnect();
                if(input!=null)input.close();
                if(inputStream!=null)inputStream.close();
            }
            if (builder.length() > 0) return builder.toString().trim();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        if (result == null) {
            listener.onError();
        } else {
            listener.onFinish(result);
        }

    }

    public interface Listener {
        void onFinish(String result);

        void onError();
    }
}
