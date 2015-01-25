package com.app.lukas.template;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.trianguloy.llscript.repository.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;
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
        JSONObject script = new JSONObject();
        setContentView(R.layout.apply_templates);

        File directory = getExternalCacheDir();
        try {
            script.put("flags",0);
            script.put("name","Repository Importer");
            script.put("id",2);
            InputStream inCode = getResources().openRawResource(R.raw.script);
            byte[] buff = new byte[1024];
            int read;
            final File template = File.createTempFile("Template", "", directory);
            ZipInputStream in = new ZipInputStream(getResources().openRawResource(R.raw.template));
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(template));
            StringBuilder builder = new StringBuilder();
            try {
                while ((read = inCode.read(buff))>0){
                    builder.append(new String(buff, 0, read));
                }
                script.put("text", new String(builder));
                String s = script.toString();

                ZipEntry entry;
                while ((entry = in.getNextEntry())!=null)
                {
                    out.putNextEntry(entry);
                    if(!entry.getName().equals("core/scripts/2")) {
                        while ((read = in.read(buff)) > 0) {
                            out.write(buff, 0, read);
                        }
                    }
                    else out.write(s.getBytes());
                    in.closeEntry();
                    out.closeEntry();
                }
                out.flush();
                out.close();
                template.setReadable(true, false);
                sendToLL(template.getAbsolutePath());
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        template.delete();
                    }
                },120000);
            } finally {
                inCode.close();
                in.close();
                out.close();
                finish();
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

    }

    private void sendToLL(String path) {
        Intent intent = new Intent("net.pierrox.lightning_launcher.APPLY_TEMPLATE");
        intent.putExtra("p", path);
        startActivity(intent);
    }
}
