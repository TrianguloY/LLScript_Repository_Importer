package com.trianguloy.llscript.repository.editor;

import android.app.ActionBar;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.RepositoryImporter;
import com.trianguloy.llscript.repository.auth.AuthenticationUtils;
import com.trianguloy.llscript.repository.internal.Dialogs;
import com.trianguloy.llscript.repository.settings.SettingsActivity;
import com.trianguloy.llscript.repository.web.RPCManager;

/**
 * Created by Lukas on 20.04.2015.
 * Provides an UI to edit/create a script page
 */
public class EditorActivity extends Activity {

    private SharedPreferences sharedPref;
    private Bundle savedInstanceState;
    private EditManager editManager;
    ViewManager viewManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.savedInstanceState = savedInstanceState;
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        RepositoryImporter.setTheme(this, sharedPref);
        editManager = new EditManager();
        viewManager = new ViewManager(this, editManager);
        viewManager.lock();

        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        viewManager.lock();
        String action = intent.getAction();
        if (action != null && !action.equals(getString(R.string.link_repository)) &&
                action.startsWith(getString(R.string.link_scriptPagePrefix)) && sharedPref.getBoolean(getString(R.string.pref_directEdit), false))
            editManager.setPageId(action.substring(action.indexOf(getString(R.string.prefix_script))));
        else editManager.setPageId(null);
        if (RPCManager.isLoggedIn() >= RPCManager.LOGIN_USER) load();
        else {
            AuthenticationUtils.login(this, new AuthenticationUtils.Listener() {
                @Override
                public void onComplete() {
                    load();
                }

                @Override
                public void onError() {
                    finish();
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_logout:
                RPCManager.logout();
                finish();
                break;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (viewManager.getState() == ViewManager.STATE_PREVIEW) {
            viewManager.setState(ViewManager.STATE_EDIT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                ActionBar bar = getActionBar();
                assert bar != null;
                bar.setDisplayHomeAsUpEnabled(false);
            }
            editManager.assign((EditText) findViewById(R.id.editor));
        } else if (viewManager.changedCode()) {
            Dialogs.unsavedChanges(this, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            });
        } else finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        viewManager.toBundle(outState, editManager);
    }

    private void restore(@NonNull Bundle restoreState) {
        viewManager.fromBundle(restoreState);
    }

    private void load() {
        if (editManager.hasPageId()) viewManager.loadPageToEdit(editManager.getPageId());
        else if (savedInstanceState != null) restore(savedInstanceState);
        else viewManager.setState(ViewManager.STATE_CHOOSE_ACTION);
    }

    public void button(View view) {
        if (!viewManager.isLocked()) {
            viewManager.lock();
            switch (view.getId()) {
                case R.id.buttonCreatePage:
                    viewManager.createPage();
                    break;
                case R.id.buttonEditPage:
                    viewManager.editPage();
                    break;
                case R.id.buttonCancel:
                    viewManager.cancelEdit();
                    break;
                case R.id.buttonSave:
                    viewManager.savePage();
                    break;
                case R.id.buttonCreate:
                    viewManager.commitCreate();
                    break;
                case R.id.buttonPreview:
                    viewManager.preview();
                    break;
                case R.id.buttonTemplate:
                    viewManager.editTemplate();
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }
    }

    public void action(View view) {
        if (viewManager.getState() != ViewManager.STATE_EDIT)
            throw new IllegalStateException("Can't execute actions when not in editor");
        if (!viewManager.isLocked()) {
            editManager.action(view.getId());
        }
    }
}