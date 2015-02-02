package com.app.lukas.llscript;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import com.trianguloy.llscript.repository.Constants;
import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.ReadRawFile;
import com.trianguloy.llscript.repository.webViewer;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Lukas on 31.01.2015.
 * Imports a script into LL
 */
public class ScriptImporter extends Service{

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.hasExtra("update")&&intent.getBooleanExtra("update",false))updateManager();
        else if(intent.hasExtra("code")&&intent.hasExtra("name")){
            ComponentName componentName = intent.hasExtra("receiver")?ComponentName.unflattenFromString(intent.getStringExtra("receiver")):null;
            boolean forceUpdate = intent.getBooleanExtra("forceUpdate",false);
            if(componentName == null){
                componentName = new ComponentName(this,webViewer.class);
            }
            installScript(intent.getStringExtra("code"),intent.getStringExtra("name"),intent.getIntExtra("flags",0),componentName,forceUpdate);
        }
        stopSelf();
        return super.onStartCommand(intent,flags,startId);
    }


    void installScript(String code, String name, int flags, ComponentName answerTo,boolean forceUpdate) {
        if (Constants.id == -1) return;
        JSONObject data = new JSONObject();
        try {
            data.put("version", Constants.managerVersion);
            data.put("code", code);
            data.put("name", name);
            data.put("flags", flags);
            data.put("returnTo",answerTo.flattenToString());
            data.put("forceUpdate",forceUpdate);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), getString(R.string.message_manager_error), Toast.LENGTH_LONG).show();
            return;
        }
        sendIntent(data);
    }

    void updateManager(){
        //Send the update to the manager to auto-update
        JSONObject data = new JSONObject();
        try {
            data.put("update", ReadRawFile.getString(getApplicationContext(), R.raw.script));
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), getString(R.string.message_manager_error), Toast.LENGTH_LONG).show();
            return;
        }
        sendIntent(data);
    }

    void sendIntent(JSONObject data){
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setComponent(ComponentName.unflattenFromString(Constants.packageMain));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra("a", 35);
        i.putExtra("d", Constants.id + "/" + data.toString());
        startActivity(i);
    }
}
