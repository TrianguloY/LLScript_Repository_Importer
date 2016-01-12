package com.trianguloy.llscript.repository.web;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.google.common.base.Charsets;
import com.trianguloy.llscript.repository.R;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Lukas on 26.01.2015.
 * Requests a Html page and return it to the listener passed in the constructor
 */
public class DownloadTask extends AsyncTask<String, Void, DownloadTask.Result> {
    private final Listener listener;
    private final String server;

    public DownloadTask(Context context, Listener listener) {
        super();
        this.listener = listener;
        server = context.getString(R.string.link_server);
    }


    @Nullable
    @Override
    protected Result doInBackground(String... urls) {
        HttpURLConnection connection;

        try {
            connection = (HttpURLConnection) new URL(urls[0]).openConnection();
            connection.setUseCaches(true);
            InputStream inputStream = null;
            try {
                inputStream = connection.getInputStream();
                Document document = Jsoup.parse(inputStream, Charsets.UTF_8.name(), server);
                return new Result(urls[0], document);
            } finally {
                connection.disconnect();
                if (inputStream != null) inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(@Nullable Result result) {
        if (result == null) {
            listener.onError();
        } else {
            listener.onFinish(result);
        }

    }

    public interface Listener {
        void onFinish(Result result);

        void onError();
    }

    public static class Result {
        public final String url;
        public final Document document;

        public Result(String url,  Document document) {
            this.url = url;
            this.document = document;
        }
    }
}
