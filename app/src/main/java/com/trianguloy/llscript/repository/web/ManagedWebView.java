package com.trianguloy.llscript.repository.web;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.trianguloy.llscript.repository.BuildConfig;
import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.internal.Dialogs;
import com.trianguloy.llscript.repository.internal.IntentHelper;
import com.trianguloy.llscript.repository.internal.PageCacheManager;
import com.trianguloy.llscript.repository.internal.Utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.Stack;

/**
 * Created by Lukas on 04.08.2015.
 * A WebView with page history and cache
 */
public class ManagedWebView extends WebView {

    private Context context;
    private Stack<HistoryElement> backStack;
    private int posY;
    private Document repoDocument;
    private Document currentDocument;

    private Listener listener;
    private DownloadTask.Listener downloadTaskListener;
    private boolean showTools;

    private String loadingId;
    @Nullable
    private AsyncTask ongoingTask;
    private PageCacheManager cacheManager;

    public ManagedWebView(Context context) {
        super(context);
        init(context);
    }

    public ManagedWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ManagedWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * initialize Structure and prepare for downloading
     *
     * @param context View context
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void init(final Context context) {
        this.context = context;
        backStack = new Stack<>();
        showTools = false;
        cacheManager = new PageCacheManager(context);
        setWebViewClient(new WebClient() {
            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, @NonNull String url) {
                //prevent login and register, broken because cookies are missing
                if (url.contains("&do=login") || url.contains("&do=register")) return true;
                if (!backStack.peek().url.equals(url)) show(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                scrollTo(0, posY);
                loading(false);
                listener.pageChanged(backStack.peek().url);
            }

            @TargetApi(Build.VERSION_CODES.N)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return shouldOverrideUrlLoading(view, request.getUrl().toString());
            }
        });
        downloadTaskListener = new DownloadTask.Listener() {

            @Override
            public void onFinish(@NonNull DownloadTask.Result result) {
                //default listener: show the page after loading it
                showPage(result.url, result.document);
                if (ManagedWebView.this.context.getString(R.string.link_repository).equals(result.url)) {
                    listener.repoDocumentUpdated(result.document);
                }
                final String id = Utils.getIdFromUrl(result.url);
                final String html = result.document.outerHtml();
                new RPCManager(context).getPageTimestamp(ManagedWebView.this.context.getString(R.string.prefix_script) + id, new RPCManager.Listener<Integer>() {
                    @Override
                    public void onResult(@NonNull RPCManager.Result<Integer> result) {
                        if (result.getStatus() == RPCManager.RESULT_OK) {
                            Integer timestamp = result.getResult();
                            assert timestamp != null;
                            cacheManager.savePage(id, new PageCacheManager.Page(timestamp, html));
                        }
                    }
                });
            }

            @Override
            public void onError() {
                loading(false);
                Dialogs.noPageLoaded(ManagedWebView.this.context, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        loading(true);
                        String url = backStack.empty() ? ManagedWebView.this.context.getString(R.string.link_repository) : backStack.peek().url;
                        new DownloadTask(ManagedWebView.this.context, downloadTaskListener).execute(url);
                    }
                });
            }
        };
        getSettings().setJavaScriptEnabled(true);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public Listener getListener() {
        return listener;
    }

    /**
     * Manages the showing of a new page (loading it from cache, getting a new version, etc)
     *
     * @param url the url to load and show
     */
    public void show(@NonNull final String url) {
        if (url.startsWith(context.getString(R.string.link_scriptPagePrefix))) {
            final String id = Utils.getIdFromUrl(url);
            if (!id.equals(loadingId)) cancel();
            loadingId = id;
            if (cacheManager.hasPage(id)) {
                final PageCacheManager.Page page = cacheManager.getPage(id);
                assert page != null;
                showPage(url, Jsoup.parse(page.html, context.getString(R.string.link_server)));
                ongoingTask = new RPCManager(context).getPageTimestamp(context.getString(R.string.prefix_script) + id, new RPCManager.Listener<Integer>() {
                    @Override
                    public void onResult(@NonNull RPCManager.Result<Integer> result) {
                        if (result.getStatus() == RPCManager.RESULT_OK) {
                            Integer timestamp = result.getResult();
                            assert timestamp != null;
                            if (timestamp > page.timestamp) {
                                cancel();
                                downloadPage(url);
                            }
                        } else Dialogs.connectionFailed(context);
                    }
                });
            } else downloadPage(url);
        } else {
            IntentHelper.sendToAllButSelf(context, Uri.parse(url));
        }
    }

    /**
     * load a page into WebView
     *
     * @param url      url of the page
     * @param document Jsoup document of the page
     */
    private void showPage(@NonNull String url, @NonNull Document document) {
        if (Utils.getIdFromUrl(url).equals(loadingId)) {
            if (!showTools) {
                //remove tools
                document.select("div.tools.group").remove();
            }
            if (context.getString(R.string.link_repository).equals(url)) repoDocument = document;
            currentDocument = document;
            loading(true);
            HistoryElement current = null;
            if (!backStack.empty()) current = backStack.pop();
            posY = 0;
            if (backStack.empty() || !url.equals(backStack.peek().url)) {
                if (current != null && !current.url.equals(url)) {
                    current.posY = getScrollY();
                    backStack.push(current);
                }
                backStack.push(new HistoryElement(url));
            } else posY = backStack.peek().posY;
            loadDataWithBaseURL(context.getString(R.string.link_server), document.outerHtml(), "text/html", "utf-8", null);
            ongoingTask = null;
        }
    }

    /**
     * download a page from the server (it gets displayed afterwards)
     *
     * @param url the page to load
     */
    private void downloadPage(final String url) {
        loading(true);
        ongoingTask = new DownloadTask(context, downloadTaskListener).execute(url);
    }

    /**
     * stop everything ongoing
     */
    private void cancel() {
        if (ongoingTask != null) ongoingTask.cancel(true);
        stopLoading();
    }

    /**
     * @return if there is a page on the backStack
     */
    public boolean backPossible() {
        return backStack.size() > 1;
    }

    /**
     * navigate one page back in the backStack
     */
    public void performBack() {
        if (!backPossible()) throw new IllegalStateException("Navigating back on empty Stack");
        HistoryElement current = backStack.pop();
        String load = backStack.peek().url;
        backStack.push(current);
        show(load);
    }

    @Nullable
    @Override
    public String getUrl() {
        if (backStack.empty()) return null;
        return backStack.peek().url;
    }

    @Nullable
    public String getPageId() {
        if (backStack.empty()) return null;
        return Utils.getIdFromUrl(backStack.peek().url);
    }

    public boolean hasPage() {
        return !backStack.empty();
    }

    /**
     * put this page onto the backStack, so it can be reached by navigating back, but don't show it
     *
     * @param url the page
     */
    public void dropOnStackWithoutShowing(String url) {
        backStack.push(new HistoryElement(url));
    }

    public Document getRepoDocument() {
        return repoDocument;
    }

    public Document getCurrentDocument() {
        return currentDocument;
    }

    /**
     * set the loading status
     *
     * @param isLoading the new status
     */
    private void loading(boolean isLoading) {
        if (listener != null) {
            listener.loading(isLoading);
        }
    }

    /**
     * set if tools should be shown
     *
     * @param showTools value
     */
    public void setShowTools(boolean showTools) {
        this.showTools = showTools;
    }

    /**
     * save the state to a bundle
     *
     * @param savedInstanceState the bundle
     */
    public void saveToInstanceState(@NonNull Bundle savedInstanceState) {
        Gson gson = new Gson();
        savedInstanceState.putString(context.getString(R.string.key_backStack), gson.toJson(backStack));
        if (repoDocument != null)
            savedInstanceState.putString(context.getString(R.string.key_repoHtml), repoDocument.outerHtml());
    }

    /**
     * load an old state from a bundle
     *
     * @param savedInstanceState the bundle
     * @return if something was restored
     */
    public boolean restoreFromInstanceState(@NonNull Bundle savedInstanceState) {
        try {
            Gson gson = new Gson();
            Stack<HistoryElement> backStack = gson.fromJson(savedInstanceState.getString(context.getString(R.string.key_backStack)), new TypeToken<Stack<HistoryElement>>() {
            }.getType());
            String repoHtml = savedInstanceState.getString(context.getString(R.string.key_repoHtml));
            if (repoHtml != null && backStack != null && !backStack.empty()) {
                this.backStack = backStack;
                this.repoDocument = Jsoup.parse(repoHtml, context.getString(R.string.link_server));
                show(backStack.pop().url);
                return true;
            }
        } catch (Exception e) {
            //though exceptions caught here might be worth reporting, we don't want to disturb the user experience and just ignore them
            if (BuildConfig.DEBUG) {
                Log.d(ManagedWebView.class.getSimpleName(), "Failed to restore Instance state", e);
            }
        }
        return false;
    }

    private static class HistoryElement {
        public final String url;
        public int posY;

        public HistoryElement(String url) {
            this.url = url;
            posY = 0;
        }
    }

    public interface Listener {
        void loading(boolean isLoading);

        void pageChanged(String url);

        void repoDocumentUpdated(Document repoHtml);
    }
}
