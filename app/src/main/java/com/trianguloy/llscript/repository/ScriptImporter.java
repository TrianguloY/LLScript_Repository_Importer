package com.trianguloy.llscript.repository;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Lukas on 31.01.2015.
 * Imports a script into LL, can be called from outside of the app
 */
public class ScriptImporter extends Service {


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.hasExtra(Constants.extraCode) && intent.hasExtra(Constants.extraName)) {
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
        JSONObject data = new JSONObject();
        try {
            data.put(Constants.ScriptCode, code);
            data.put(Constants.ScriptName, name);
            data.put(Constants.ScriptFlags, flags);
            data.put(Constants.ScriptReturnResultTo, answerTo.flattenToString());
            data.put(Constants.ScriptForceUpdate, forceUpdate);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), getString(R.string.toast_managerError), Toast.LENGTH_LONG).show();
            return;
        }
        sendIntent(data);
    }


    void sendIntent(JSONObject data) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setComponent(new ComponentName(Constants.installedPackage, Constants.activityRunScript));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra(Constants.RunActionExtra, Constants.RunActionKey);
        i.putExtra(Constants.RunDataExtra, Constants.managerId + "/" + data.toString());
        startActivity(i);
    }
}
