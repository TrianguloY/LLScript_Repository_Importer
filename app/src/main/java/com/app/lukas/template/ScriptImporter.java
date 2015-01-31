package com.app.lukas.template;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.trianguloy.llscript.repository.Constants;
import com.trianguloy.llscript.repository.R;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Lukas on 31.01.2015.
 * Imports a script into LL
 */
public class ScriptImporter extends Service{

    private final IBinder binder = new LocalBinder();
    private Listener listener;

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder {
        public ScriptImporter getService(Context context){
            if(context.getPackageName().equals(getApplicationContext().getPackageName()) ||
                   context.checkCallingPermission("net.pierrox.lightning_launcher.IMPORT_SCRIPTS")== PackageManager.PERMISSION_GRANTED)
                return ScriptImporter.this;
            else throw new SecurityException("App is not allowed to import scripts into LL");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.hasExtra("id")&&listener!=null)listener.onFinish(intent.getIntExtra("id", -1));
        Log.d("Importer","ID: "+intent.getIntExtra("id",-1));
        return super.onStartCommand(intent, flags, startId);
    }

    /*
    * listener gets called if import was successful or not.
        * @returns: whether the import was successful or not.
         */
    public boolean installScript(String code,String name,int flags,@Nullable Listener listener) {
        this.listener = listener;
        if (Constants.id == -1) return false;
        JSONObject data = new JSONObject();
        try {
            data.put("version", Constants.managerVersion);
            data.put("code", code);
            data.put("name", name);
            data.put("flags", flags);
            data.put("returnId",listener!=null);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), getString(R.string.message_manager_error), Toast.LENGTH_LONG).show();
            return false;
        }
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setComponent(ComponentName.unflattenFromString(Constants.packageMain));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra("a", 35);
        i.putExtra("d", Constants.id + "/" + data.toString());
        startActivity(i);
        return true;
    }

    public interface Listener{
        public void onFinish(int id);
    }
}
