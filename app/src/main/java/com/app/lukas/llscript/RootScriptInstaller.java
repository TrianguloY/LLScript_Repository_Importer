package com.app.lukas.llscript;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import com.trianguloy.llscript.repository.Constants;
import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.StringFunctions;
import com.trianguloy.llscript.repository.noManager;
import com.trianguloy.llscript.repository.webViewer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

/**
 * Created by Lukas on 30.01.2015.
 * Loads the script into the launcher by dropping it into the data folder
 */
public class RootScriptInstaller extends Activity{

    private Context context;
    private ProgressDialog dialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        new Root().execute();
    }

    private class Root extends AsyncTask<Void,Void,Void>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //display a progressDialog to tell the user something is going on
            dialog = new ProgressDialog(context);
            dialog.setTitle(R.string.title_root);
            dialog.setMessage(getString(R.string.message_root));
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            BufferedWriter writer = null;
                try {
                    if(Shell.SU.available()){
                        //read the script into json
                        JSONObject script = new JSONObject();
                        script.put("flags",0);
                        script.put("name",getString(R.string.script_name));
                        script.put("text", StringFunctions.getRawFile(getApplicationContext(),R.raw.script));
                        //find the lowest free script id
                        int i=0;
                        while (true){
                            List<String> output =  Shell.SU.run("if [ -f " + Constants.scriptsPath + i + " ]\nthen\necho true\nelse\necho false\nfi");
                            if(output==null)throw new IOException("Device Root is broken");
                            if(!Boolean.parseBoolean(output.get(0)))break;
                            i++;
                        }
                        script.put("id", i);
                        //write script to temp file
                        File file = File.createTempFile("script","",getCacheDir());
                        writer = new BufferedWriter(new FileWriter(file));
                        writer.write(script.toString());
                        writer.flush();
                        //copy temp file to LLs directory
                        Shell.SU.run("cp -f "+file.getAbsolutePath()+" "+Constants.scriptsPath+i);
                        //check if file was created
                        List<String> output =  Shell.SU.run("if [ -f " + Constants.scriptsPath + i + " ]\nthen\necho true\nelse\necho false\nfi");
                        if(output == null)throw new IOException("Device Root is broken");
                        else if(!Boolean.parseBoolean(output.get(0)))throw new IOException("Failed to create file at "+Constants.scriptsPath+i);
                        //give LL access to the file TODO set LL as owner instead
                        Shell.SU.run("chmod 666 \""+ Constants.scriptsPath + i+ "\"");
                        onSuccessful(i);
                        file.deleteOnExit();
                    }
                    else onNotSuccessful();
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    onNotSuccessful();
                }
            finally {
                    try {
                        if (writer != null) writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            return null;
        }
    }

    private void onSuccessful(int id){
        dialog.dismiss();
        //pass the id to webViewer
        Intent intent = new Intent(this,webViewer.class);
        intent.putExtra("id",(double)id);
        startActivity(intent);
        finish();
    }

    private void onNotSuccessful(){
        dialog.dismiss();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //show a dialog because something failed
                new AlertDialog.Builder(context)
                        .setTitle("")
                        .setMessage(getString(R.string.message_root_load_failed))
                        .setNeutralButton(getString(R.string.button_exit), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                //return to the noManager screen
                                Intent intent = new Intent(getApplicationContext(), noManager.class);
                                startActivity(intent);
                                finish();
                            }
                        })
                        .setIcon(R.drawable.ic_launcher)
                        .show();
            }
        });
    }
}
