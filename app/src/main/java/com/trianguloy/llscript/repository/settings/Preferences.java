package com.trianguloy.llscript.repository.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Lukas on 19.12.2015.
 * Wraps normal SharedPreferences to allow higher API methods on lower Versions (by executing an alternative)
 */
public class Preferences implements SharedPreferences {

    private final SharedPreferences base;
    private final Context context;

    @NonNull
    public static Preferences getDefault(Context context) {
        return new Preferences(PreferenceManager.getDefaultSharedPreferences(context), context);
    }

    public Preferences(SharedPreferences base, Context context) {
        this.base = base;
        this.context = context;
    }

    @Override
    public Map<String, ?> getAll() {
        return base.getAll();
    }


    @Override
    public String getString(String key, String defValue) {
        return base.getString(key, defValue);
    }


    public String getString(@StringRes int key, String defValue) {
        return getString(context.getString(key), defValue);
    }


    @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        if (defValues == null) defValues = new HashSet<>();
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

    public Set<String> getStringSet(@StringRes int key, @Nullable Set<String> defValues) {
        return getStringSet(context.getString(key), defValues);
    }

    @Override
    public int getInt(String key, int defValue) {
        return base.getInt(key, defValue);
    }

    public int getInt(@StringRes int key, int defValue) {
        return getInt(context.getString(key), defValue);
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

    public boolean getBoolean(@StringRes int key, boolean defValue) {
        return getBoolean(context.getString(key), defValue);
    }

    @Override
    public boolean contains(String key) {
        return base.contains(key);
    }

    public boolean contains(@StringRes int key) {
        return contains(context.getString(key));
    }

    @NonNull
    @SuppressLint("CommitPrefEdits")
    @Override
    public Editor edit() {
        return new Editor(base.edit(), context);
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
        private final Context context;

        public Editor(SharedPreferences.Editor base, Context context) {
            this.base = base;
            this.context = context;
        }

        @NonNull
        @Override
        public Editor putString(String key, String value) {
            base.putString(key, value);
            return this;
        }

        @NonNull
        public Editor putString(@StringRes int key, String value) {
            return putString(context.getString(key), value);
        }

        @NonNull
        @Override
        public Editor putStringSet(String key, Set<String> values) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                base.putStringSet(key, values);
            } else {
                base.putString(key, new JSONArray(values).toString());
            }
            return this;
        }

        @NonNull
        public Editor putStringSet(@StringRes int key, Set<String> values) {
            return putStringSet(context.getString(key), values);
        }

        @NonNull
        @Override
        public Editor putInt(String key, int value) {
            base.putInt(key, value);
            return this;
        }

        @NonNull
        public Editor putInt(@StringRes int key, int value) {
            return putInt(context.getString(key), value);
        }

        @NonNull
        @Override
        public Editor putLong(String key, long value) {
            base.putLong(key, value);
            return this;
        }

        @NonNull
        @Override
        public Editor putFloat(String key, float value) {
            base.putFloat(key, value);
            return this;
        }

        @NonNull
        @Override
        public Editor putBoolean(String key, boolean value) {
            base.putBoolean(key, value);
            return this;
        }

        @NonNull
        public Editor putBoolean(@StringRes int key, boolean value) {
            return putBoolean(context.getString(key), value);
        }

        @NonNull
        @Override
        public Editor remove(String key) {
            base.remove(key);
            return this;
        }

        @NonNull
        public Editor remove(@StringRes int key) {
            return remove(context.getString(key));
        }

        @NonNull
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
