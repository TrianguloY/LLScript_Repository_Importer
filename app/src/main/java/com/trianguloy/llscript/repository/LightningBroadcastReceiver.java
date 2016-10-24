package com.trianguloy.llscript.repository;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import com.trianguloy.llscript.repository.aidl.ILightningService;

/**
 * Created on 10.08.2016.
 *
 * @author F43nd1r
 */

public class LightningBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        IBinder binder = peekService(context, new Intent(context, ScriptImporter.class));
        if(binder != null){
            try {
                ILightningService.Stub.asInterface(binder).returnResult(intent);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
