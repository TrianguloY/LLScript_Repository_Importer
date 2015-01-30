package com.app.lukas.template;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;
import com.trianguloy.llscript.repository.Constants;
import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.ReadRawFile;
import com.trianguloy.llscript.repository.noManager;
import com.trianguloy.llscript.repository.webViewer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeoutException;

/**
 * Created by Lukas on 30.01.2015.
 * Loads the script into the launcher by dropping it there
 */
public class RootScriptInstaller extends Activity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        InputStream in = getResources().openRawResource(R.raw.script);//script code stream
        try {
            File file = File.createTempFile("script", "", getCacheDir());
            OutputStream out = new FileOutputStream(file);
            try {
                if(RootTools.isAccessGiven()){
                    //read the script into json
                    JSONObject script = new JSONObject();
                    script.put("flags",0);
                    script.put("name",getString(R.string.script_name));
                    script.put("text", ReadRawFile.getString(this,R.raw.script));
                    //find the lowest free script id
                    int i=0;
                    while (RootTools.exists((Constants.scriptsPath+String.valueOf(i))))i++;
                    script.put("id", i);
                    //write the script to a temp file
                    out.write(script.toString().getBytes());
                    out.flush();
                    //copy the file to LL's data directory
                    RootTools.copyFile(file.getAbsolutePath(), (Constants.scriptsPath + String.valueOf(i)), false, true);
                    //give LL access to the file TODO set LL as owner instead
                    Command chmod = new Command(0,"su","chmod 666 \""+ (Constants.scriptsPath + String.valueOf(i))+ "\"","exit");
                    RootTools.getShell(true).add(chmod).finish();
                    onSuccessful(i);
                    //remove the temporary file on next shutdown
                    file.deleteOnExit();
                }
                else onNotSuccessful();
            } catch (IOException | JSONException | RootDeniedException |TimeoutException e) {
                onNotSuccessful();
            } finally {
                in.close();
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            onNotSuccessful();
        }
    }

    private void onSuccessful(int id){
        //pass the id to webViewer
        Intent intent = new Intent(this,webViewer.class);
        intent.putExtra("id",(double)id);
        startActivity(intent);
        finish();
    }

    private void onNotSuccessful(){
        //show a dialog because something failed
        new AlertDialog.Builder(this)
                .setTitle("")
                .setMessage(getString(R.string.message_root_load_failed))
                .setNeutralButton(getString(R.string.button_exit), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        //return to the noManager screen
                        Intent intent = new Intent(getApplicationContext(),noManager.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .setIcon(R.drawable.ic_launcher)
                .show();
    }
}
