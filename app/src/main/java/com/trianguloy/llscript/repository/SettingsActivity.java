package com.trianguloy.llscript.repository;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlarmManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.Toast;

import com.trianguloy.llscript.repository.internal.Dialogs;
import com.trianguloy.llscript.repository.internal.ServiceManager;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        RepositoryImporter.setTheme(this, PreferenceManager.getDefaultSharedPreferences(this));

        super.onCreate(savedInstanceState);

        setupActionBar();
        addPreferencesFromResource(R.xml.pref_general);


        ListPreference listPreference = (ListPreference) findPreference(getString(R.string.pref_notificationInterval));
        listPreference.setSummary(listPreference.getEntry());
        CheckBoxPreference checkBoxPreference = (CheckBoxPreference) findPreference(getString(R.string.pref_notifications));
        listPreference.setEnabled(checkBoxPreference.isChecked());
        final Preference resetPwPref = findPreference(getString(R.string.pref_resetPw));
        final AccountManager accountManager = AccountManager.get(this);
        final Account[] accounts = accountManager.getAccountsByType(getString(R.string.account_type));
        resetPwPref.setEnabled(accounts.length > 0 && accountManager.getPassword(accounts[0]) != null);
        resetPwPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                accountManager.clearPassword(accounts[0]);
                resetPwPref.setEnabled(false);
                Toast.makeText(SettingsActivity.this, getString(R.string.toast_resetPw), Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        ListPreference newScript = (ListPreference) findPreference(getString(R.string.pref_newScripts));
        newScript.setSummary(newScript.getEntry());
        ListPreference changedSubs = (ListPreference) findPreference(getString(R.string.pref_changedSubs));
        changedSubs.setSummary(changedSubs.getEntry());
        CheckBoxPreference theme = (CheckBoxPreference) findPreference(getString(R.string.key_theme));
        theme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Dialogs.themeChanged(SettingsActivity.this);
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            ActionBar bar = getActionBar();
            assert bar != null;
            bar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            // TODO: If Settings has multiple levels, Up should navigate up
            // that hierarchy.
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this) && !isSimplePreferences(this);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Determines whether the simplified settings UI should be shown. This is
     * true if the device
     * doesn't have newer APIs like PreferenceFragment, or the device
     * doesn't have an extra-large screen. In these cases, a single-pane
     * "simplified" settings UI should be shown.
     */
    private static boolean isSimplePreferences(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
                || !isXLargeTablet(context);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_notificationInterval))) {
            ListPreference listPreference = (ListPreference) findPreference(key);
            listPreference.setSummary(listPreference.getEntry());
            startService(sharedPreferences);
        } else if (key.equals(getString(R.string.pref_notifications))) {
            CheckBoxPreference checkBoxPreference = (CheckBoxPreference) findPreference(key);
            if (checkBoxPreference.isChecked()) startService(sharedPreferences);
            else stopService();
            ListPreference listPreference = (ListPreference) findPreference(getString(R.string.pref_notificationInterval));
            listPreference.setEnabled(checkBoxPreference.isChecked());
        } else if (key.equals(getString(R.string.pref_newScripts))) {
            ListPreference listPreference = (ListPreference) findPreference(key);
            listPreference.setSummary(listPreference.getEntry());
        } else if (key.equals(getString(R.string.pref_changedSubs))) {
            ListPreference listPreference = (ListPreference) findPreference(key);
            listPreference.setSummary(listPreference.getEntry());
        }
    }


    private void stopService() {
        ServiceManager.stopService(getApplicationContext());
        getPackageManager().setComponentEnabledSetting(new ComponentName(getApplicationContext(), BootBroadcastReceiver.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    private void startService(SharedPreferences sharedPreferences) {
        ServiceManager.startService(getApplicationContext(),
                Integer.parseInt(sharedPreferences.getString(getString(R.string.pref_notificationInterval),
                        String.valueOf(AlarmManager.INTERVAL_HOUR))));
        getPackageManager().setComponentEnabledSetting(new ComponentName(getApplicationContext(), BootBroadcastReceiver.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }
}
