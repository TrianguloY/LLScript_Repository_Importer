package com.trianguloy.llscript.repository.web;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.WebView;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.trianguloy.llscript.repository.BuildConfig;
import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.internal.AppChooser;
import com.trianguloy.llscript.repository.internal.Dialogs;
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
    private AsyncTask ongoingTask;

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

    private void init(Context context) {
        this.context = context;
        backStack = new Stack<>();
        showTools = false;
        setWebViewClient(new WebClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
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
        });
        downloadTaskListener = new DownloadTask.Listener() {

            @Override
            public void onFinish(DownloadTask.Result result) {
                //default listener: show the page after loading it
                showPage(result.url, result.document);
                if (ManagedWebView.this.context.getString(R.string.link_repository).equals(result.url)) {
                    listener.repoDocumentUpdated(result.document);
                }
                final String id = Utils.getNameFromUrl(result.url);
                final String html = result.document.outerHtml();
                RPCManager.getPageTimestamp(ManagedWebView.this.context.getString(R.string.prefix_script) + id, new RPCManager.Listener<Integer>() {
                    @Override
                    public void onResult(RPCManager.Result<Integer> result) {
                        if (result.getStatus() == RPCManager.RESULT_OK) {
                            PageCacheManager.savePage(id, new PageCacheManager.Page(result.getResult(), html));
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
                        new DownloadTask(downloadTaskListener).execute(backStack.peek().url);
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
     * @param url the url to load and show
     */
    public void show(final String url) {
        if (url.startsWith(context.getString(R.string.link_scriptPagePrefix))) {
            final String id = Utils.getNameFromUrl(url);
            if (!id.equals(loadingId)) cancel();
            loadingId = id;
            if (PageCacheManager.hasPage(id)) {
                final PageCacheManager.Page page = PageCacheManager.getPage(id);
                assert page != null;
                showPage(url, Jsoup.parse(page.html, context.getString(R.string.link_server)));
                ongoingTask = RPCManager.getPageTimestamp(context.getString(R.string.prefix_script) + id, new RPCManager.Listener<Integer>() {
                    @Override
                    public void onResult(RPCManager.Result<Integer> result) {
                        if (result.getStatus() == RPCManager.RESULT_OK) {
                            if (result.getResult() > page.timestamp) {
                                cancel();
                                downloadPage(url);
                            }
                        } else Dialogs.connectionFailed(context);
                    }
                });
            } else downloadPage(url);
        } else {
            new AppChooser(context, Uri.parse(url), context.getString(R.string.title_appChooserExternalClicked), context.getString(R.string.message_noBrowser), null).show();
        }
    }

    private void showPage(String url, Document document) {
        if (Utils.getNameFromUrl(url).equals(loadingId)) {
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

    private void downloadPage(final String url) {
        loading(true);
        ongoingTask = new DownloadTask(downloadTaskListener).execute(url);
    }

    private void cancel() {
        if (ongoingTask != null) ongoingTask.cancel(true);
        stopLoading();
    }

    public boolean backPossible() {
        return backStack.size() > 1;
    }

    public void performBack() {
        if (!backPossible()) throw new IllegalStateException("Navigating back on empty Stack");
        HistoryElement current = backStack.pop();
        String load = backStack.peek().url;
        backStack.push(current);
        show(load);
    }

    @Override
    public String getUrl() {
        if (backStack.empty()) return null;
        return backStack.peek().url;
    }

    public String getPageId() {
        if (backStack.empty()) return null;
        return Utils.getNameFromUrl(backStack.peek().url);
    }

    public boolean hasPage() {
        return !backStack.empty();
    }

    public void dropOnStackWithoutShowing(String url) {
        backStack.push(new HistoryElement(url));
    }

    public Document getRepoDocument() {
        return repoDocument;
    }

    public Document getCurrentDocument() {
        return currentDocument;
    }

    private void loading(boolean isLoading) {
        if (listener != null) {
            listener.loading(isLoading);
        }
    }

    public void setShowTools(boolean showTools) {
        this.showTools = showTools;
    }

    public void saveToInstanceState(Bundle savedInstanceState) {
        Gson gson = new Gson();
        savedInstanceState.putString(context.getString(R.string.key_backStack), gson.toJson(backStack));
        if (repoDocument != null)
            savedInstanceState.putString(context.getString(R.string.key_repoHtml), repoDocument.outerHtml());
    }

    public boolean restoreFromInstanceState(Bundle savedInstanceState) {
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
