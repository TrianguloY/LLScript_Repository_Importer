package com.trianguloy.llscript.repository;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.app.lukas.template.ApplyTemplate;
import com.app.lukas.template.RootScriptInstaller;

/**
 * Activity launched when the script manager is not found.
 * Asks the user to import the script into the launcher (from template or script)
*/

public class noManager extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nomanager);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @SuppressWarnings({"unused", "unusedParameter"})
    public void buttonInjectFromTemplate(View v) {
        //start the script injection process from template
        Intent intent = new Intent(this, ApplyTemplate.class);
        startActivity(intent);
        finish();
    }

    @SuppressWarnings({"unused", "unusedParameter"})
    public void buttonInjectFromLauncher(View v) {
        //start the script injection process from launcher
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setComponent(ComponentName.unflattenFromString(Constants.packageMain));
        intent.putExtra("a", 7);
        startActivity(intent);
        finish();
    }
    @SuppressWarnings({"unused", "unusedParameter"})
    public void buttonInjectThroughRoot(View v){
        startActivity(new Intent(this, RootScriptInstaller.class));
        finish();
    }

}
