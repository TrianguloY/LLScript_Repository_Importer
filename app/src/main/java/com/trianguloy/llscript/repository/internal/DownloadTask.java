package com.trianguloy.llscript.repository.internal;

import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Lukas on 26.01.2015.
 * Requests a Html page and return it to the listener passed in the constructor
 */
public class DownloadTask extends AsyncTask<String, Void, String> {
    private final Listener listener;
    private final boolean usePost;
    private final Map<String,String> body;

    public DownloadTask(Listener listener) {
        this(listener,false, null);
    }

    public DownloadTask(Listener listener, boolean usePost, Map<String,String> body) {
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
                ArrayList<Byte> list = new ArrayList<>();
                Iterator<String> it = body.keySet().iterator();
                while (it.hasNext()){
                    String key = it.next();
                    byte[] add = key.getBytes("UTF-8");
                    for (byte b:add) list.add(b);
                    add = "=".getBytes("UTF-8");
                    for (byte b:add) list.add(b);
                    add = URLEncoder.encode(body.get(key),"UTF-8").getBytes("UTF-8");
                    for (byte b:add) list.add(b);
                    if(it.hasNext()) {
                        add = "&".getBytes("UTF-8");
                        for (byte b : add) list.add(b);
                    }
                }
                byte[] bytes = new byte[list.size()];
                for(int i=0;i<list.size();i++) bytes[i] = list.get(i);
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
