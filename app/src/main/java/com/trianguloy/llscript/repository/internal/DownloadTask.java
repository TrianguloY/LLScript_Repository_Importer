package com.trianguloy.llscript.repository.internal;

import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;

/**
 * Created by Lukas on 26.01.2015.
 * Requests a Html page and return it to the listener passed in the constructor
 */
public class DownloadTask extends AsyncTask<String, Void, String> {
    private final Listener listener;
    private final boolean usePost;
    private final String body;

    public DownloadTask(Listener listener) {
        this(listener,false, null);
    }

    public DownloadTask(Listener listener, boolean usePost, String body) {
        this.listener = listener;
        this.usePost = usePost;
        this.body = body;
    }

    @Override
    protected String doInBackground(String... urls) {
        HttpURLConnection connection;

        try {
            connection = (HttpURLConnection) new URL(urls[0]).openConnection();
            connection.setUseCaches(true);
            if(usePost){
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                Charset utf8 = Charset.forName("UTF-8");
                byte[] bytes = utf8.encode(URLEncoder.encode(body,"UTF-8")).array();
                connection.setFixedLengthStreamingMode(bytes.length);
                OutputStream stream = connection.getOutputStream();
                try {
                    stream.write(bytes);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                finally {
                    stream.close();
                }
            }
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
