package com.trianguloy.llscript.repository.settings;

import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Lukas on 14.12.2015.
 * Manages Preference change events
 */
class PreferenceListener implements SharedPreferences.OnSharedPreferenceChangeListener {

    @NonNull
    private final Map<String, Wrapper> map;
    private final PreferenceScreen screen;

    public PreferenceListener(PreferenceScreen screen) {
        this.screen = screen;
        map = new HashMap<>();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (map.keySet().contains(key)) {
            if (map.get(key).setSummaryToValue) {
                setSummary(key);
            }
            Runnable run = map.get(key).action;
            if (run != null) run.run();
        }
    }

    private void setSummary(String key) {
        Preference preference = screen.findPreference(key);
        if (preference instanceof ListPreference) {
            preference.setSummary(((ListPreference) preference).getEntry());
        } else {
            preference.setSummary(String.valueOf(screen.getSharedPreferences().getAll().get(key)));
        }
    }

    /**
     * add a preference to keep its summary set to its value
     *
     * @param preference the preference
     */
    public void addPreferenceForSummary(@NonNull Preference preference) {
        addPreferenceForSummary(preference.getKey());
    }

    /**
     * add a preference to keep its summary set to its value
     *
     * @param key the preference identifier
     */
    public void addPreferenceForSummary(String key) {
        addPreference(key, true, null);
    }

    /**
     * add an action to execute when the preference changes
     *
     * @param preference the preference
     * @param action     the action
     */
    public void addPreference(@NonNull Preference preference, Runnable action) {
        addPreference(preference.getKey(), action);
    }

    /**
     * add an action to execute when the preference changes
     *
     * @param key    the preference identifier
     * @param action the action
     */
    public void addPreference(String key, Runnable action) {
        addPreference(key, false, action);
    }

    /**
     * add an action to execute when the preference changes and keep its summary set to its value
     *  @param preference        the preference
     * @param action            the action
     */
    public void addPreferenceForSummary(@NonNull Preference preference, Runnable action) {
        addPreference(preference.getKey(), true, action);
    }

    /**
     * add an action to execute when the preference changes and keep its summary set to its value
     * @param key    the preference identifier
     * @param action            the action
     */
    public void addPreferenceForSummary(String key, Runnable action) {
        addPreference(key, true, action);
    }

    /**
     * add an action to execute when the preference changes and optionally keep its summary set to its value
     *
     * @param key               the preference identifier
     * @param setSummaryToValue if the summary should be kept set to the value
     * @param action            the action
     */
    private void addPreference(String key, boolean setSummaryToValue, Runnable action) {
        map.put(key, new Wrapper(action, setSummaryToValue));
        if (setSummaryToValue) {
            setSummary(key);
        }
    }

    private static class Wrapper {
        final boolean setSummaryToValue;
        final Runnable action;

        public Wrapper(Runnable action, boolean setSummaryToValue) {
            this.action = action;
            this.setSummaryToValue = setSummaryToValue;
        }
    }
}
