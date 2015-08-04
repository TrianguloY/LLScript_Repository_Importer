package com.trianguloy.llscript.repository.web;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.util.AttributeSet;
import android.webkit.WebView;

import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.internal.AppChooser;
import com.trianguloy.llscript.repository.internal.Dialogs;
import com.trianguloy.llscript.repository.internal.PageCacheManager;
import com.trianguloy.llscript.repository.internal.Utils;

import java.util.Stack;

/**
 * Created by Lukas on 04.08.2015.
 * A WebView with page history and cache
 */
public class ManagedWebView extends WebView {

    private Context context;
    private Stack<HistoryElement> backStack;
    private int posY;
    private String repoHtml;
    private Listener listener;
    private DownloadTask.Listener downloadTaskListener;
    private boolean showTools;

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
                showPage(result.url, result.html);
                final String id = Utils.getNameFromUrl(result.url);
                final String html = result.html;
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

    public void show(final String url) {
        if (url.startsWith(context.getString(R.string.link_scriptPagePrefix))) {
            final String id = Utils.getNameFromUrl(url);
            if (PageCacheManager.hasPage(id)) {
                RPCManager.getPageTimestamp(context.getString(R.string.prefix_script) + id, new RPCManager.Listener<Integer>() {
                    @Override
                    public void onResult(RPCManager.Result<Integer> result) {
                        if (result.getStatus() == RPCManager.RESULT_OK) {
                            PageCacheManager.Page page = PageCacheManager.getPage(id);
                            if (result.getResult() > page.timestamp) {
                                downloadPage(url);
                            } else if (!url.equals(backStack.peek().url)) {
                                showPage(url, page.html);
                            }
                        } else Dialogs.connectionFailed(context);
                    }
                });
            } else downloadPage(url);
        } else {
            new AppChooser(context, Uri.parse(url), context.getString(R.string.title_appChooserExternalClicked), context.getString(R.string.message_noBrowser), null).show();
        }
    }

    private void showPage(String url, String html) {
        if (!showTools) {
            //remove tools
            Utils.valueAndIndex val = Utils.findBetween(html, "<div class=\"tools group\">", "<hr class=\"a11y\" />", 0, false);
            html = html.substring(0, val.from) + html.substring(val.to, html.length());
        }
        if (context.getString(R.string.link_repository).equals(url)) repoHtml = html;
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
        loadDataWithBaseURL(context.getString(R.string.link_server), html, "text/html", "utf-8", null);
    }

    private void downloadPage(final String url) {
        loading(true);
        new DownloadTask(downloadTaskListener).execute(url);
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
        if (backStack.empty()) throw new IllegalStateException("Getting url from empty Stack");
        return backStack.peek().url;
    }

    public boolean hasPage() {
        return !backStack.empty();
    }

    public void dropOnStackWithoutShowing(String url) {
        backStack.push(new HistoryElement(url));
    }

    public String getRepoHtml() {
        return repoHtml;
    }

    private void loading(boolean isLoading) {
        if (listener != null) {
            listener.loading(isLoading);
        }
    }

    public void setShowTools(boolean showTools) {
        this.showTools = showTools;
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
    }
}
