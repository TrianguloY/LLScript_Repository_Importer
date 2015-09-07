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

        if (intent.hasExtra(Constants.EXTRA_CODE) && intent.hasExtra(Constants.EXTRA_NAME)) {
            ComponentName componentName = intent.hasExtra(Constants.EXTRA_RECEIVER) ? ComponentName.unflattenFromString(intent.getStringExtra(Constants.EXTRA_RECEIVER)) : null;
            if (Utils.checkForLauncher(this)) {
                if (componentName == null) {
                    componentName = new ComponentName(this, IntentHandle.class);
                }
                boolean forceUpdate = intent.getBooleanExtra(Constants.EXTRA_FORCE_UPDATE, false);
                installScript(intent.getStringExtra(Constants.EXTRA_CODE), intent.getStringExtra(Constants.EXTRA_NAME), intent.getIntExtra(Constants.EXTRA_FLAGS, 0), componentName, forceUpdate);
            } else if (componentName != null) {
                //callback for other apps
                Intent response = new Intent(Intent.ACTION_VIEW);
                response.setComponent(componentName);
                response.putExtra(Constants.EXTRA_STATUS, Constants.STATUS_LAUNCHER_PROBLEM);
                response.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(response);
            }
        } else if (intent.hasExtra(Constants.EXTRA_FORWARD)) {
            Intent forward = intent.getParcelableExtra(Constants.EXTRA_FORWARD);
            if(forward.hasExtra(Constants.EXTRA_BACKGROUND) && forward.getBooleanExtra(Constants.EXTRA_BACKGROUND,false)){
                runScriptInBackground(forward.getStringExtra(Constants.EXTRA_DATA));
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
            data.put(Constants.KEY_CODE, code);
            data.put(Constants.KEY_NAME, name);
            data.put(Constants.KEY_FLAGS, flags);
            data.put(Constants.KEY_RETURN_RESULT_TO, answerTo.flattenToString());
            data.put(Constants.KEY_FORCE_UPDATE, forceUpdate);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), getString(R.string.toast_managerError), Toast.LENGTH_LONG).show();
            return;
        }
        runExecutorInBackground(data.toString());
    }


    private void runExecutorInBackground(String data) {
        runScriptInBackground(Constants.MANAGER_ID + "/" + data);
    }

    private void runScriptInBackground(String idAndData){
        Intent i = new Intent(getString(R.string.intent_actionBackgroundReceiver));
        i.setClassName(Constants.installedPackage, getString(R.string.intent_backgroundReceiverClass));
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.EXTRA_TARGET, Constants.TARGET_BACKGROUND);
        bundle.putInt(Constants.EXTRA_ACTION, Constants.ACTION_RUN);
        bundle.putString(Constants.EXTRA_DATA, idAndData);
        i.putExtra(getString(R.string.intent_backgroundReceiverExtraBundle), bundle);
        sendBroadcast(i);
    }
}
