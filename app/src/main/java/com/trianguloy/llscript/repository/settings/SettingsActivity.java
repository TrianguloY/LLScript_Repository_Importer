package com.trianguloy.llscript.repository.settings;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlarmManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.annotation.NonNull;
import android.view.MenuItem;
import android.widget.Toast;

import com.trianguloy.llscript.repository.BootBroadcastReceiver;
import com.trianguloy.llscript.repository.BuildConfig;
import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.RepositoryImporter;
import com.trianguloy.llscript.repository.auth.AuthenticationUtils;
import com.trianguloy.llscript.repository.internal.Dialogs;
import com.trianguloy.llscript.repository.web.WebServiceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
public class SettingsActivity extends PreferenceActivity {

    private PreferenceListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final Preferences pref = Preferences.getDefault(this);
        RepositoryImporter.setTheme(this, pref);
        super.onCreate(savedInstanceState);

        setupActionBar();
        addPreferencesFromResource(R.xml.pref_general);
        listener = new PreferenceListener(getPreferenceScreen());

        final ListPreference intervalPreference = (ListPreference) findPreference(getString(R.string.pref_notificationInterval));
        listener.addPreferenceForSummary(intervalPreference, new Runnable() {
            @Override
            public void run() {
                startService(pref);
            }
        });
        final CheckBoxPreference checkBoxPreference = (CheckBoxPreference) findPreference(getString(R.string.pref_notifications));
        intervalPreference.setEnabled(checkBoxPreference.isChecked());
        listener.addPreference(checkBoxPreference, new Runnable() {
            @Override
            public void run() {
                if (checkBoxPreference.isChecked())
                    startService(pref);
                else stopService();
                intervalPreference.setEnabled(checkBoxPreference.isChecked());
            }
        });
        Preference resetPwPref = findPreference(getString(R.string.pref_resetPw));
        resetPwPref.setEnabled(AuthenticationUtils.hasPassword(this));
        resetPwPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AuthenticationUtils.resetPassword(SettingsActivity.this);
                Toast.makeText(SettingsActivity.this, getString(R.string.toast_resetPw), Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        listener.addPreference(getString(R.string.pref_theme), new Runnable() {
            @Override
            public void run() {
                Dialogs.themeChanged(SettingsActivity.this);
            }
        });
        listener.addPreferenceForSummary(getString(R.string.pref_newScripts));
        listener.addPreferenceForSummary(getString(R.string.pref_changedSubs));
        listener.addPreferenceForSummary(getString(R.string.pref_reportMode));
        if (!BuildConfig.DEBUG) {
            removeDebugOptions();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Preferences.getDefault(this).registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Preferences.getDefault(this).unregisterOnSharedPreferenceChangeListener(listener);
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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            // If Settings has multiple levels, Up should navigate up
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
    private static boolean isXLargeTablet(@NonNull Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD
                && (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Determines whether the simplified settings UI should be shown. This is
     * true if the device
     * doesn't have newer APIs like PreferenceFragment, or the device
     * doesn't have an extra-large screen. In these cases, a single-pane
     * "simplified" settings UI should be shown.
     */
    private static boolean isSimplePreferences(@NonNull Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
                || !isXLargeTablet(context);
    }


    private void stopService() {
        WebServiceManager.stopService(getApplicationContext());
        getPackageManager().setComponentEnabledSetting(new ComponentName(getApplicationContext(), BootBroadcastReceiver.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    private void startService(@NonNull Preferences pref) {
        WebServiceManager.startService(getApplicationContext(),
                Integer.parseInt(pref.getString(R.string.pref_notificationInterval,
                        String.valueOf(AlarmManager.INTERVAL_HOUR))));
        getPackageManager().setComponentEnabledSetting(new ComponentName(getApplicationContext(), BootBroadcastReceiver.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    /**
     * removes options which should not be visible to normal users
     */
    private void removeDebugOptions() {
        ListPreference intervalPreference = (ListPreference) findPreference(getString(R.string.pref_notificationInterval));
        //Remove every minute check
        List<CharSequence> entries = new ArrayList<>(Arrays.asList(intervalPreference.getEntries()));
        List<CharSequence> entryValues = new ArrayList<>(Arrays.asList(intervalPreference.getEntryValues()));
        entries.remove(0);
        entryValues.remove(0);
        intervalPreference.setEntries(entries.toArray(new CharSequence[entries.size()]));
        intervalPreference.setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));
        //remove enable acra preference
        CheckBoxPreference acraPref = (CheckBoxPreference) findPreference(getString(R.string.pref_enableAcra));
        acraPref.setChecked(true);
        getPreferenceScreen().removePreference(acraPref);
    }
}
