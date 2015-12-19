package com.trianguloy.llscript.repository.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Map;
import java.util.Set;

/**
 * Created by Lukas on 19.12.2015.
 */
public class Preferences implements SharedPreferences {

    private final SharedPreferences base;

    public static Preferences getDefault(Context context){
        return new Preferences(PreferenceManager.getDefaultSharedPreferences(context));
    }

    public Preferences(SharedPreferences base) {
        this.base = base;
    }

    @Override
    public Map<String, ?> getAll() {
        return base.getAll();
    }

    @Nullable
    @Override
    public String getString(String key, String defValue) {
        return base.getString(key, defValue);
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        if (base.contains(key)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                return base.getStringSet(key, defValues);
            JSONArray array;
            try {
                defValues.clear();
                array = new JSONArray(base.getString(key, ""));
                for (int i = 0; i < array.length(); i++) {
                    defValues.add(array.getString(i));
                }
                return defValues;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
        return base.getInt(key, defValue);
    }

    @Override
    public long getLong(String key, long defValue) {
        return base.getLong(key, defValue);
    }

    @Override
    public float getFloat(String key, float defValue) {
        return base.getFloat(key, defValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return base.getBoolean(key, defValue);
    }

    @Override
    public boolean contains(String key) {
        return base.contains(key);
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public Editor edit() {
        return new Editor(base.edit());
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        base.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        base.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public static class Editor implements SharedPreferences.Editor {

        private final SharedPreferences.Editor base;

        public Editor(SharedPreferences.Editor base) {
            this.base = base;
        }

        @Override
        public Editor putString(String key, String value) {
            base.putString(key, value);
            return this;
        }

        @Override
        public Editor putStringSet(String key, Set<String> values) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                base.putStringSet(key, values);
            }
            else {
                base.putString(key, new JSONArray(values).toString());
            }
            return this;
        }

        @Override
        public Editor putInt(String key, int value) {
            base.putInt(key, value);
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            base.putLong(key, value);
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            base.putFloat(key, value);
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            base.putBoolean(key, value);
            return this;
        }

        @Override
        public Editor remove(String key) {
            base.remove(key);
            return this;
        }

        @Override
        public Editor clear() {
            base.clear();
            return this;
        }

        @Override
        public boolean commit() {
            return base.commit();
        }

        @Override
        public void apply() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                base.apply();
            } else {
                commit();
            }
        }
    }
}
