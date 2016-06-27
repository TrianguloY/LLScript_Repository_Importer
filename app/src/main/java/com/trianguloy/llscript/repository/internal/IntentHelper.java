package com.trianguloy.llscript.repository.internal;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;

/**
 * Helps to send urls to all apps but this one
 */

public final class IntentHelper {
    private IntentHelper() {
    }

    public static void sendToAllButSelf(final Context context, Uri uri) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(uri);
        setHttpFilterState(context, false);
        context.startActivity(i);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setHttpFilterState(context, true);
            }
        }, 100);
    }

    private static void setHttpFilterState(Context context, boolean enabled) {
        String packageName = context.getPackageName();
        ComponentName componentName = new ComponentName(packageName, packageName + ".HttpFilter"); // Activity alias
        context.getPackageManager().setComponentEnabledSetting(componentName,
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
}
