package com.trianguloy.llscript.repository;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by Lukas on 27.04.2015.
 * Returns the authentication object to the android system
 */

public class AuthenticationService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return new AccountAuthenticator(this).getIBinder();
    }

}
