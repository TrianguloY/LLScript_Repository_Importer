package com.app.lukas.llscript;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.Toast;

import com.trianguloy.llscript.repository.Constants;
import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.StringFunctions;

/**
 * Created by Lukas on 10.02.2015.
 * Sends the script text to the binding activity. Used by the script to update itself
 */
public class ScriptUpdateService extends Service {

    private final Messenger messenger;

    public ScriptUpdateService() {
        messenger = new Messenger(new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.replyTo != null) {
                    Message message = Message.obtain();
                    Bundle bundle = new Bundle(1);
                    bundle.putString(Constants.ScriptCode, StringFunctions.getRawFile(getApplicationContext(), R.raw.script));
                    message.setData(bundle);
                    try {
                        msg.replyTo.send(message);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), getString(R.string.message_manager_error), Toast.LENGTH_LONG).show();
                    }
                }
                return true;
            }
        }));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }
}
