package com.trianguloy.llscript.repository.web;

import android.annotation.TargetApi;
import android.net.http.HttpResponseCache;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Lukas on 30.04.2015.
 * Base class to be further extended
 * Enforces caching on all Resources
 */
public class WebClient extends WebViewClient {

    @Nullable
    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public WebResourceResponse shouldInterceptRequest(final WebView view, @NonNull final String url) {
        //from http://stackoverflow.com/questions/12063937/can-i-use-the-android-4-httpresponsecache-with-a-webview-based-application/13596877#13596877
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH || !(url.startsWith("http://") || url.startsWith("https://")) || HttpResponseCache.getInstalled() == null)
            return null;
        try {
            final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.connect();
            final String contentType = connection.getContentType();
            final String separator = "; charset=";
            final int pos = contentType.indexOf(separator);
            final String mimeType = pos >= 0 ? contentType.substring(0, pos) : contentType;
            final String encoding = pos >= 0 ? contentType.substring(pos + separator.length()) : "UTF-8";
            return new WebResourceResponse(mimeType, encoding, connection.getInputStream());
        } catch (@NonNull final MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (@NonNull final IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Nullable
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, @NonNull WebResourceRequest request) {
        return shouldInterceptRequest(view,request.getUrl().toString());
    }
}
