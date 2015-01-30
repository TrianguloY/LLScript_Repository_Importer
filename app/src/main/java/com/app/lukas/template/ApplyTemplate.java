package com.app.lukas.template;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.trianguloy.llscript.repository.Constants;
import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.ReadRawFile;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


/**
 * Created by Lukas on 25.01.2015.
 * Creates a template containing the script and asks lightning to install it
 */
public class ApplyTemplate extends Activity{

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        File directory = getExternalCacheDir();
        JSONObject script = new JSONObject(); //the script in JSON form, as needed in a template
        try {
            //put general information
            script.put("flags",0);
            script.put("name",getString(R.string.script_name));
            script.put("id",2);
            final File template = File.createTempFile("Template", "", directory);//temporary file for the new template with the updated script
            ZipInputStream in = new ZipInputStream(getResources().openRawResource(R.raw.template));//stream from the template without updated script
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(template));//stream to the new template

            //initialize used temp vars, buffer
            byte[] buff = new byte[1024];
            int read;
            ZipEntry entry;

            try {
                script.put("text", ReadRawFile.getString(this,R.raw.script));

                //write the old template to the new one
                while ((entry = in.getNextEntry())!=null)
                {
                    out.putNextEntry(entry);
                    //normal zip entry
                    if(!entry.getName().equals("core/scripts/2")) {
                        while ((read = in.read(buff)) > 0) {
                            out.write(buff, 0, read);
                        }
                    }
                    //script entry, replace with script JSON object
                    else out.write(script.toString().getBytes());
                    in.closeEntry();
                    out.closeEntry();
                }
                //make sure file is closed before LL tries to read it
                out.flush();
                out.close();
                //give read permissions to other apps
                if(!template.setReadable(true, false))throw new IOException("Not able to modify file permissions of "+template.getAbsolutePath());
                //notify LL to import the template
                sendToLL(template.getAbsolutePath());
                //delete the file on next restart
                template.deleteOnExit();
            } finally {
                //make sure all streams get closed even if sth goes wrong
                in.close();
                out.close();
                finish();
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Toast.makeText(this,getString(R.string.message_template_error),Toast.LENGTH_LONG).show();
        }

    }

    private void sendToLL(String path) {
        //send intent to LL with path to the template
        Intent intent = new Intent(Constants.packageTemplate);
        intent.putExtra("p", path);
        startActivity(intent);
    }
}
