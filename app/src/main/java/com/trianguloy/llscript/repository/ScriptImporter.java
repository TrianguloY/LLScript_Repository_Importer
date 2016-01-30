package com.trianguloy.llscript.repository;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.trianguloy.llscript.repository.aidl.Failure;
import com.trianguloy.llscript.repository.aidl.ICallback;
import com.trianguloy.llscript.repository.aidl.ILightningService;
import com.trianguloy.llscript.repository.aidl.Script;
import com.trianguloy.llscript.repository.internal.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Lukas on 31.01.2015.
 * Imports a script into LL, can be called from outside of the app
 */
public class ScriptImporter extends Service {

    private Map<Integer, ICallback> callbackMap = new HashMap<>();
    private int nextRequestId = 0;

    /**
     * as long as a caller is active, we need to listen for intents from Lightning
     */
    @Override
    public void onCreate() {
        registerReceiver(receiver, new IntentFilter("net.pierrox.lightning_launcher.script.IMPORT_RESPONSE"));
    }

    /**
     * all callers have unbound, we don't need to listen anymore
     */
    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
    }

    /**
     * only legacy callers enter through here
     *
     * @param intent  the intent
     * @param flags   ignored
     * @param startId ignored
     * @return START_NOT_STICKY
     */
    @Override
    public int onStartCommand(@NonNull Intent intent, int flags, int startId) {
        onStartCommand(intent);
        return START_NOT_STICKY;
    }

    /**
     * backwards-compatibility
     */
    @Deprecated
    public void onStartCommand(@NonNull Intent intent) {
        final ComponentName componentName = intent.hasExtra(Constants.EXTRA_RECEIVER) ? ComponentName.unflattenFromString(intent.getStringExtra(Constants.EXTRA_RECEIVER)) : null;
        if (Utils.hasValidLauncher(this)) {
            if (intent.hasExtra(Constants.EXTRA_CODE) && intent.hasExtra(Constants.EXTRA_NAME)) {
                boolean forceUpdate = intent.getBooleanExtra(Constants.EXTRA_FORCE_UPDATE, false);
                final String code = intent.getStringExtra(Constants.EXTRA_CODE);
                final String name = intent.getStringExtra(Constants.EXTRA_NAME);
                @Script.ScriptFlag final int flags = intent.getIntExtra(Constants.EXTRA_FLAGS, 0);
                try {
                    lightningService.importScript(new Script(code, name, flags), forceUpdate, new ICallback.Stub() {
                        @Override
                        public void onImportFinished(int scriptId) throws RemoteException {
                            Intent response = new Intent(Intent.ACTION_VIEW);
                            response.setComponent(componentName);
                            response.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                            response.putExtra(Constants.EXTRA_LOADED_SCRIPT_ID, scriptId);
                            response.putExtra(Constants.EXTRA_STATUS, Constants.STATUS_OK);
                            startActivity(response);
                            stopSelf();
                        }

                        @Override
                        public void onImportFailed(Failure failure) throws RemoteException {
                            if (failure == Failure.SCRIPT_ALREADY_EXISTS) {
                                Intent response = new Intent(Intent.ACTION_VIEW);
                                response.setComponent(componentName);
                                response.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                response.putExtra(Constants.EXTRA_STATUS, Constants.STATUS_UPDATE_CONFIRMATION_REQUIRED);
                                response.putExtra(Constants.EXTRA_NAME, name);
                                response.putExtra(Constants.EXTRA_CODE, code);
                                response.putExtra(Constants.EXTRA_FLAGS, flags);
                                startActivity(response);
                            }
                            stopSelf();
                        }
                    });
                } catch (RemoteException e) {
                    e.printStackTrace();
                    stopSelf();
                }
            } else if (intent.hasExtra(Constants.EXTRA_FORWARD)) {
                Intent forward = intent.getParcelableExtra(Constants.EXTRA_FORWARD);
                int action = forward.getIntExtra(Constants.EXTRA_ACTION, Constants.ACTION_RUN);
                String data = forward.getStringExtra(Constants.EXTRA_DATA);
                boolean background = forward.getBooleanExtra(Constants.EXTRA_BACKGROUND, false);
                sendAction(action, data, background);
                stopSelf();
            }
        } else if (componentName != null) {
            //callback for other apps
            Intent response = new Intent(Intent.ACTION_VIEW);
            response.setComponent(componentName);
            response.putExtra(Constants.EXTRA_STATUS, Constants.STATUS_LAUNCHER_PROBLEM);
            response.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(response);
            stopSelf();
        }
    }

    /**
     * up-to-date callers (and the app itself) enter here
     *
     * @param intent the intent
     * @return an ILightningService instance as a Binder
     */
    @Override
    public IBinder onBind(Intent intent) {
        return lightningService.asBinder();
    }


    /**
     * install script into LL
     *
     * @param script      the script
     * @param callBackId  callback which receives the id when finished, or errors when failed
     * @param forceUpdate if the script should be overwritten if it exists already
     */
    private void installScript(Script script, int callBackId, boolean forceUpdate) {
        JSONObject data = new JSONObject();
        try {
            data.put(Constants.KEY_CODE, script.getCode());
            data.put(Constants.KEY_NAME, script.getName());
            data.put(Constants.KEY_FLAGS, script.getFlags());
            data.put(Constants.KEY_CALLBACK_ID, callBackId);
            data.put(Constants.KEY_FORCE_UPDATE, forceUpdate);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), getString(R.string.toast_managerError), Toast.LENGTH_LONG).show();
            return;
        }
        runScript(Constants.MANAGER_ID, data.toString(), true);
    }

    /**
     * run a script in LL
     *
     * @param id         the script identifier
     * @param data       optional script data
     * @param background if the script should be run in background
     */
    private void runScript(int id, String data, boolean background) {
        sendAction(Constants.ACTION_RUN, id + "/" + data, background);
    }

    /**
     * send an action to LL
     *
     * @param action     the action ID as defined by LL
     * @param data       optional data
     * @param background if the action should be executed in background
     */
    private void sendAction(int action, String data, boolean background) {
        if (background) {
            sendActionBackground(action, data);
        } else {
            sendActionForeground(action, data);
        }
    }

    /**
     * send an action to LL (executed in background)
     *
     * @param action the action ID as defined by LL
     * @param data   optional data
     */
    private void sendActionBackground(int action, String data) {
        Intent i = new Intent(getString(R.string.intent_actionBackgroundReceiver));
        i.setClassName(Utils.getLauncherPackage(this), getString(R.string.intent_backgroundReceiverClass));
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.EXTRA_TARGET, Constants.TARGET_BACKGROUND);
        bundle.putInt(Constants.EXTRA_ACTION, action);
        bundle.putString(Constants.EXTRA_DATA, data);
        i.putExtra(getString(R.string.intent_backgroundReceiverExtraBundle), bundle);
        sendBroadcast(i);
    }

    /**
     * send an action to LL (executed in foreground)
     *
     * @param action the action ID as defined by LL
     * @param data   optional data
     */
    private void sendActionForeground(int action, String data) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setClassName(Utils.getLauncherPackage(this), getString(R.string.intent_foregroundReceiverClass));
        i.putExtra(Constants.EXTRA_ACTION, action);
        i.putExtra(Constants.EXTRA_DATA, data);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    /**
     * the interface exposed to callers
     */
    private ILightningService lightningService = new ILightningService.Stub() {
        @Override
        public synchronized void importScript(Script script, boolean overwriteIfExists, ICallback callback) throws RemoteException {
            if (script.isValid()) {
                if (Utils.hasValidLauncher(ScriptImporter.this)) {
                    int requestId = nextRequestId++;
                    callbackMap.put(requestId, callback);
                    installScript(script, requestId, overwriteIfExists);
                } else if (callback != null) {
                    callback.onImportFailed(Failure.LAUNCHER_INVALID);
                }
            } else if (callback != null) {
                callback.onImportFailed(Failure.INVALID_INPUT);
            }
        }

        @Override
        public synchronized void runScript(int id, String data, boolean background) throws RemoteException {
            if (Utils.hasValidLauncher(ScriptImporter.this)) {
                ScriptImporter.this.runScript(id, data, background);
            }
        }

        @Override
        public synchronized void runAction(int action, String data, boolean background) throws RemoteException {
            if (Utils.hasValidLauncher(ScriptImporter.this)) {
                sendAction(action, data, background);
            }
        }
    };

    /**
     * the receiver for communication with LL
     */
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(Constants.KEY_CALLBACK_ID) && intent.hasExtra(Constants.EXTRA_STATUS)) {
                int status = (int) intent.getDoubleExtra(Constants.EXTRA_STATUS, 0);
                int callbackId = (int) intent.getDoubleExtra(Constants.KEY_CALLBACK_ID, -1);
                ICallback callback = callbackMap.get(callbackId);
                if (callback != null) {
                    try {
                        switch (status) {
                            case Constants.STATUS_OK:
                                callback.onImportFinished((int) intent.getDoubleExtra(Constants.EXTRA_LOADED_SCRIPT_ID, 0));
                                break;
                            case Constants.STATUS_UPDATE_CONFIRMATION_REQUIRED:
                                callback.onImportFailed(Failure.SCRIPT_ALREADY_EXISTS);
                                break;
                            default:
                                throw new IllegalArgumentException("Invalid status code returned from script: " + status);
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };
}
