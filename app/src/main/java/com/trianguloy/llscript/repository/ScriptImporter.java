package com.trianguloy.llscript.repository;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import com.trianguloy.llscript.repository.internal.Utils;

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
            if (Utils.checkForLauncher(this)) {
                if (componentName == null) {
                    componentName = new ComponentName(this, IntentHandle.class);
                }
                boolean forceUpdate = intent.getBooleanExtra(Constants.extraForceUpdate, false);
                installScript(intent.getStringExtra(Constants.extraCode), intent.getStringExtra(Constants.extraName), intent.getIntExtra(Constants.extraFlags, 0), componentName, forceUpdate);
            } else if (componentName != null) {
                //callback for other apps
                Intent response = new Intent(Intent.ACTION_VIEW);
                response.setComponent(componentName);
                response.putExtra(Constants.extraStatus, Constants.STATUS_LAUNCHER_PROBLEM);
                response.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(response);
            }
        } else if (intent.hasExtra(Constants.extraForward)) {
            Intent forward = intent.getParcelableExtra(Constants.extraForward);
            if(forward.hasExtra(Constants.extraBackground) && forward.getBooleanExtra(Constants.extraBackground,false)){
                runScriptInBackground(forward.getStringExtra(Constants.RunDataExtra));
            }
            else {
                forward.setPackage(Constants.installedPackage);
                forward.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(forward);
            }
        }
        stopSelf();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void installScript(String code, String name, int flags, ComponentName answerTo, boolean forceUpdate) {
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
        runScriptInBackground(Constants.managerId, data.toString());
    }


    private void runScriptInBackground(int id, String data) {
        runScriptInBackground(id + "/" + data);
    }

    private void runScriptInBackground(String idAndData){
        Intent i = new Intent(getString(R.string.intent_actionBackgroundReceiver));
        i.setClassName(Constants.installedPackage, getString(R.string.intent_backgroundReceiverClass));
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.RunBackgroundExtra, Constants.RunBackgroundKey);
        bundle.putInt(Constants.RunActionExtra, Constants.RunActionKey);
        bundle.putString(Constants.RunDataExtra, idAndData);
        i.putExtra(getString(R.string.intent_backgroundReceiverExtraBundle), bundle);
        sendBroadcast(i);
    }
}
