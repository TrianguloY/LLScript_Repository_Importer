package com.app.lukas.llscript;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.trianguloy.llscript.repository.Constants;
import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.StringFunctions;
import com.trianguloy.llscript.repository.webViewer;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Lukas on 31.01.2015.
 * Imports a script into LL, can be called from outside of the app
 */
public class ScriptImporter extends Service {

    private int id = -1;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //initialize variables
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        id = sharedPref.getInt(Constants.keyId, Constants.notId);

        if (intent.hasExtra(Constants.extraUpdate) && intent.getBooleanExtra(Constants.extraUpdate, false))
            updateManager();
        else if (intent.hasExtra(Constants.extraCode) && intent.hasExtra(Constants.extraName)) {
            ComponentName componentName = intent.hasExtra(Constants.extraReceiver) ? ComponentName.unflattenFromString(intent.getStringExtra(Constants.extraReceiver)) : null;
            boolean forceUpdate = intent.getBooleanExtra(Constants.extraForceUpdate, false);
            if (componentName == null) {
                componentName = new ComponentName(this, webViewer.class);
            }
            installScript(intent.getStringExtra(Constants.extraCode), intent.getStringExtra(Constants.extraName), intent.getIntExtra(Constants.extraFlags, 0), componentName, forceUpdate);
        }
        stopSelf();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    void installScript(String code, String name, int flags, ComponentName answerTo, boolean forceUpdate) {
        if (id == -1) return;
        JSONObject data = new JSONObject();
        try {
            data.put(Constants.ScriptVersion, Constants.managerVersion);
            data.put(Constants.ScriptCode, code);
            data.put(Constants.ScriptName, name);
            data.put(Constants.ScriptFlags, flags);
            data.put(Constants.ScriptReturnResultTo, answerTo.flattenToString());
            data.put(Constants.ScriptForceUpdate, forceUpdate);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), getString(R.string.message_manager_error), Toast.LENGTH_LONG).show();
            return;
        }
        sendIntent(data);
    }

    @Deprecated
    void updateManager() {
        //Send the update to the manager to auto-update
        JSONObject data = new JSONObject();
        try {
            data.put(Constants.ScriptUpdate, StringFunctions.getRawFile(getApplicationContext(), R.raw.manager));
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), getString(R.string.message_manager_error), Toast.LENGTH_LONG).show();
            return;
        }
        sendIntent(data);
    }

    void sendIntent(JSONObject data) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setComponent(ComponentName.unflattenFromString(Constants.packageMain));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra(Constants.extraRunAction, 35);
        i.putExtra(Constants.extraRunData, id + "/" + data.toString());
        startActivity(i);
    }
}
