package com.trianguloy.llscript.repository.editor;

import android.app.ActionBar;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.trianguloy.llscript.repository.BuildConfig;
import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.auth.AuthenticationUtils;
import com.trianguloy.llscript.repository.internal.Dialogs;
import com.trianguloy.llscript.repository.internal.Utils;
import com.trianguloy.llscript.repository.settings.Preferences;
import com.trianguloy.llscript.repository.web.DownloadTask;
import com.trianguloy.llscript.repository.web.RPCManager;
import com.trianguloy.llscript.repository.web.WebClient;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import dw.xmlrpc.Page;

/**
 * Created by Lukas on 07.09.2015.
 * Manages editorActivities view state
 */
class ViewManager extends Lock {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STATE_NONE,STATE_CHOOSE_ACTION,STATE_CREATE,STATE_EDIT,STATE_PREVIEW})
    public @interface State {
    }
    public static final int STATE_NONE = -1;
    public static final int STATE_CHOOSE_ACTION = 0;
    public static final int STATE_CREATE = 1;
    public static final int STATE_EDIT = 2;
    public static final int STATE_PREVIEW = 3;

    private final EditorActivity context;
    @State
    private int state;
    private final EditManager editManager;
    private final RPCManager rpcManager;
    private boolean isTemplate;
    @NonNull
    private final Random random;
    @NonNull
    private final Preferences sharedPref;
    private Repository repository;
    private Repository.RepositoryCategory addTo;

    public ViewManager(EditorActivity context, EditManager editManager) {
        super(context);
        this.context = context;
        this.editManager = editManager;
        this.rpcManager = new RPCManager(context);
        isTemplate = false;
        random = new Random();
        sharedPref = Preferences.getDefault(context);
        setState(STATE_NONE);
    }

    public void setState(@State int state) {
        this.state = state;
        boolean showActionBar = true;
        switch (state) {
            case STATE_CHOOSE_ACTION:
                context.setContentView(R.layout.activity_select_action);
                if (rpcManager.isLoggedIn() > RPCManager.NOT_LOGGED_IN) {
                    ((TextView) context.findViewById(R.id.textUser)).setText(context.getString(R.string.text_LoggedInAs) + " " + rpcManager.getUser());
                }
                break;
            case STATE_CREATE:
                context.setContentView(R.layout.activity_create);
                break;
            case STATE_EDIT:
                context.setContentView(R.layout.activity_edit);
                showActionBar = false;
                break;
            case STATE_PREVIEW:
                context.setContentView(R.layout.activity_preview);
                break;
            case STATE_NONE:
                context.setContentView(R.layout.activity_empty);
                break;
            default:
                if (BuildConfig.DEBUG)
                    Log.wtf(EditorActivity.class.getSimpleName(), "Unknown state!");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ActionBar bar = context.getActionBar();
            assert bar != null;
            if (showActionBar) bar.show();
            else bar.hide();
        }
        unlock();
    }

    @State
    public int getState() {
        return state;
    }

    public void toBundle(@NonNull Bundle bundle, @NonNull EditManager editManager) {
        bundle.putInt(context.getString(R.string.key_state), state);
        if (state == STATE_EDIT || state == STATE_PREVIEW) {
            editManager.toBundle(bundle);
            bundle.putBoolean(context.getString(R.string.key_isTemplate), isTemplate);
        }

    }

    public void fromBundle(@NonNull Bundle bundle) {
        if (bundle.containsKey(context.getString(R.string.key_state))) {
            switch (bundle.getInt(context.getString(R.string.key_state))) {
                case STATE_NONE:
                case STATE_CHOOSE_ACTION:
                    setState(ViewManager.STATE_CHOOSE_ACTION);
                    break;
                case STATE_CREATE:
                    createPage();
                    break;
                case STATE_EDIT:
                case STATE_PREVIEW:
                    editManager.fromBundle(bundle);
                    isTemplate = bundle.getBoolean(context.getString(R.string.key_isTemplate));
                    showPageEditor(null);
                    break;
            }
        }
    }

    private void showPageEditor(@Nullable String text) {
        setState(STATE_EDIT);
        if (text == null) {
            editManager.assign((EditText) context.findViewById(R.id.editor));
        } else {
            editManager.assign((EditText) context.findViewById(R.id.editor), text);
        }
    }

    void loadPageToEdit(final String id) {
        rpcManager.getPage(id, new RPCManager.Listener<String>() {
            @Override
            public void onResult(@NonNull RPCManager.Result<String> result) {
                unlock();
                if (result.getStatus() == RPCManager.RESULT_OK) {
                    showPageEditor(result.getResult());
                    editManager.setPageId(id);
                } else {
                    Dialogs.connectionFailed(context);
                }
            }
        });
    }

    private void showSelectPageToEdit(@NonNull final Page[] pages) {
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.sub_list_item);
        for (Page p : pages) {
            adapter.add(p.id());
        }
        adapter.sort(new Comparator<String>() {
            @Override
            public int compare(@NonNull String lhs, @NonNull String rhs) {
                return Utils.getNameForPage(context, lhs).toLowerCase().compareTo(Utils.getNameForPage(context, rhs).toLowerCase());
            }
        });
        Dialogs.selectPageToEdit(context, adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(@NonNull DialogInterface dialogInterface, int i) {
                lock();
                dialogInterface.dismiss();
                loadPageToEdit(adapter.getItem(i));
            }
        });
    }

    void preview() {
        final String tempId = context.getString(R.string.prefix_temp) + random.nextInt();
        rpcManager.putPage(context.getString(R.string.prefix_script) + tempId, editManager.getText(), new RPCManager.Listener<Void>() {
            @Override
            public void onResult(@NonNull RPCManager.Result<Void> result) {
                unlock();
                switch (result.getStatus()) {
                    case RPCManager.RESULT_OK:
                        showPreview(tempId);
                        break;
                    case RPCManager.RESULT_NEED_RW:
                        AuthenticationUtils.login(context, new AuthenticationUtils.Listener() {
                            @Override
                            public void onComplete() {
                                preview();
                            }

                            @Override
                            public void onError() {
                            }
                        });
                        break;
                    case RPCManager.RESULT_NETWORK_ERROR:
                        Dialogs.connectionFailed(context);
                        break;
                    case RPCManager.RESULT_BAD_LOGIN:
                        Dialogs.badLogin(context);
                        break;
                }
            }
        });
    }

    private void showPreview(final String tempId) {
        setState(STATE_PREVIEW);
        lock();
        final WebView webView = (WebView) context.findViewById(R.id.webPreview);
        webView.getSettings().setJavaScriptEnabled(true);
        //noinspection deprecation
        webView.setWebViewClient(new WebClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                rpcManager.putPage("script_" + tempId, "", null);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Toast.makeText(context, context.getString(R.string.toast_previewLink), Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View ignored) {
                return true;
            }
        });
        new DownloadTask(context, new DownloadTask.Listener() {
            @Override
            public void onFinish(@NonNull DownloadTask.Result res) {
                if (!sharedPref.getBoolean(R.string.pref_showTools, false)) {
                    //remove tools
                    res.document.select("div.tools.group").remove();
                }
                webView.loadDataWithBaseURL(context.getString(R.string.link_server), res.document.outerHtml(), "text/html", "UTF-8", null);
                unlock();
            }

            @Override
            public void onError() {
                if (BuildConfig.DEBUG) Log.i("Preview", "Ignored Error");
                unlock();
            }
        }).execute(context.getString(R.string.link_scriptPagePrefix) + tempId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ActionBar bar = context.getActionBar();
            assert bar != null;
            bar.setDisplayHomeAsUpEnabled(true);
        }
    }

    void commitCreate() {
        editManager.setPageId(context.getString(R.string.prefix_script) + ((EditText) context.findViewById(R.id.editId)).getText());
        Spinner spinner = (Spinner) context.findViewById(R.id.spinner);
        final Repository.RepositoryCategory selected = ((Repository.RepositoryCategory) spinner.getSelectedItem());
        rpcManager.getPage(editManager.getPageId(), new RPCManager.Listener<String>() {
            @Override
            public void onResult(@NonNull RPCManager.Result<String> result) {
                if (result.getStatus() == RPCManager.RESULT_OK) {
                    String r = result.getResult();
                    if (r == null || r.equals("")) {
                        if (selected.level >= 0) {
                            addTo = selected;
                            editManager.setPageName(((EditText) context.findViewById(R.id.editName)).getText().toString());
                        }
                        if (((RadioButton) context.findViewById(R.id.radioDefault)).isChecked()) {
                            rpcManager.getPage(context.getString(R.string.id_scriptTemplate), new RPCManager.Listener<String>() {
                                @Override
                                public void onResult(@NonNull RPCManager.Result<String> result) {
                                    unlock();
                                    if (result.getStatus() == RPCManager.RESULT_OK) {
                                        showPageEditor(result.getResult());
                                    } else {
                                        Dialogs.connectionFailed(context);
                                    }
                                }
                            });
                        } else if (((RadioButton) context.findViewById(R.id.radioCustom)).isChecked()) {
                            showPageEditor(sharedPref.getString(R.string.pref_template, ""));
                        } else showPageEditor("");
                    } else {
                        unlock();
                        Dialogs.pageAlreadyExists(context);
                    }
                } else {
                    unlock();
                    Dialogs.connectionFailed(context);
                }
            }
        });
    }

    void savePage() {
        final String text = editManager.getText();
        if (isTemplate) {
            sharedPref.edit().putString(R.string.pref_template, text).apply();
            unlock();
            editManager.saved();
            Dialogs.saved(context, null);
        } else if ("".equals(text)) {
            unlock();
            Dialogs.cantSaveEmpty(context);
        } else {
            final String pageId = editManager.getPageId();
            rpcManager.putPage(pageId, text, new RPCManager.Listener<Void>() {
                        @Override
                        public void onResult(@NonNull RPCManager.Result<Void> result) {
                            switch (result.getStatus()) {
                                case RPCManager.RESULT_OK:
                                    editManager.saved();
                                    if (addTo != null) {
                                        repository.addScript(addTo, pageId, editManager.getPageName());
                                        rpcManager.putPage(context.getString(R.string.id_scriptRepository), repository.getText(), new RPCManager.Listener<Void>() {
                                                    @Override
                                                    public void onResult(@NonNull RPCManager.Result<Void> result) {
                                                        unlock();
                                                        switch (result.getStatus()) {
                                                            case RPCManager.RESULT_OK:
                                                                Dialogs.saved(context, pageId);
                                                                break;
                                                            case RPCManager.RESULT_NEED_RW:
                                                                throw new IllegalStateException();
                                                            case RPCManager.RESULT_NETWORK_ERROR:
                                                                Dialogs.connectionFailed(context);
                                                                break;
                                                            case RPCManager.RESULT_BAD_LOGIN:
                                                                Dialogs.badLogin(context);
                                                                break;
                                                        }
                                                    }
                                                }

                                        );
                                    } else {
                                        unlock();
                                        Dialogs.saved(context, pageId);
                                    }
                                    break;
                                case RPCManager.RESULT_NEED_RW:
                                    AuthenticationUtils.login(context, new AuthenticationUtils.Listener() {
                                        @Override
                                        public void onComplete() {
                                            savePage();
                                        }

                                        @Override
                                        public void onError() {
                                        }
                                    });
                                case RPCManager.RESULT_NETWORK_ERROR:
                                    unlock();
                                    Dialogs.connectionFailed(context);
                                    break;
                                case RPCManager.RESULT_BAD_LOGIN:
                                    Dialogs.badLogin(context);
                                    break;
                            }
                        }
                    }

            );
        }
    }

    void cancelEdit() {
        unlock();
        if (changedCode()) {
            Dialogs.unsavedChanges(context, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    setState(STATE_CHOOSE_ACTION);
                }
            });
        } else {
            setState(STATE_CHOOSE_ACTION);
        }
    }

    void editPage() {
        rpcManager.getAllPages(new RPCManager.Listener<List<Page>>() {
            @Override
            public void onResult(@NonNull RPCManager.Result<List<Page>> result) {
                unlock();
                if (result.getStatus() != RPCManager.RESULT_OK) {
                    Dialogs.connectionFailed(context);
                    return;
                }
                final HashSet<Page> pages = new HashSet<>();
                List<Page> list = result.getResult();
                assert list != null;
                for (Page p : list) {
                    if (p.id().startsWith(context.getString(R.string.prefix_script))) pages.add(p);
                }
                Page[] array = pages.toArray(new Page[pages.size()]);
                showSelectPageToEdit(array);
            }
        });
    }

    void createPage() {
        rpcManager.getPage(context.getString(R.string.id_scriptRepository), new RPCManager.Listener<String>() {
            @Override
            public void onResult(@NonNull RPCManager.Result<String> result) {
                unlock();
                if (result.getStatus() != RPCManager.RESULT_OK) {
                    Dialogs.connectionFailed(context);
                    return;
                }
                String page = result.getResult();
                assert page != null;
                repository = new Repository(page, context.getString(R.string.text_none));
                setState(STATE_CREATE);
                Spinner spinner = (Spinner) context.findViewById(R.id.spinner);
                spinner.setAdapter(new CategoryAdapter(context, repository.getCategories()));
            }
        });
    }

    void editTemplate() {
        isTemplate = true;
        showPageEditor(sharedPref.getString(R.string.pref_template, ""));
    }

    boolean changedCode() {
        return getState() == STATE_EDIT && editManager.isChanged();
    }
}
