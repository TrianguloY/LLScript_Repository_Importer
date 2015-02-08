package com.trianguloy.llscript.repository;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import com.app.lukas.llscript.RootScriptInstaller;

import java.util.List;

import eu.chainfire.libsuperuser.Shell;

/**
 * Activity launched when the script manager is not found.
 * Asks the user to import the script into the launcher (from template or script)
*/

public class noManager extends Activity {

    private Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nomanager);
        activity = this;
        testForRoot();
    }

    @Override
    public void onBackPressed() {
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

    private void testForRoot(){
        new RootChecker().execute();
    }

    //checks if su binary is available
    private class RootChecker extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... params) {
            String[] paths = System.getenv("PATH").split(":");
            try {
                String[] command = new String[paths.length];
                for(int i=0;i<paths.length;i++){
                    String path = paths[i];
                    if(!path.endsWith("/"))
                    {
                        path += "/";
                    }
                    command[i] = "stat "+path+"su";
                }
                List<String> output = Shell.SH.run(command);
                for (String out:output){
                    //apply matching layout if binary was found
                    if(out.contains("File:")&&out.contains("su"))
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                activity.setContentView(R.layout.activity_nomanager_hasroot);
                            }
                        });
                }

            }
            catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }
    }

}
