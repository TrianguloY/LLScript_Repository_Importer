package com.app.lukas.llscript;

import android.content.Context;
import android.os.AsyncTask;

import com.trianguloy.llscript.repository.R;

import java.io.IOException;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

/**
 * Created by Lukas on 09.02.2015.
 * Does everything related to root using libsuperuser
 */
public class RootFunctions {
    private static OnRootCheckResultListener listener;

    public static boolean fileExists(Context ctx, String path) throws IOException {
        List<String> output = Shell.SU.run("if [ -f " + path + " ]\nthen\necho true\nelse\necho false\nfi");
        if (output == null)
            throw new IOException(ctx.getString(R.string.message_unexpected_root_failure));
        return Boolean.parseBoolean(output.get(0));
    }

    public static void copyFile(String pathFrom, String pathTo) {
        Shell.SU.run("cp -f " + pathFrom + " " + pathTo);
    }

    public static void permitReadWrite(String path) {
        Shell.SU.run("chmod 666 \"" + path + "\"");
    }

    //this actually test for root, user must grant permission
    public static boolean checkForRoot() {
        return Shell.SU.available();
    }

    //this is not a safe check, no guarantee to be correct
    public static void testForProbablyRooted(OnRootCheckResultListener listener) {
        RootFunctions.listener = listener;
        new RootChecker().execute();
    }

    //checks if su binary is available
    private static class RootChecker extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            String[] paths = System.getenv("PATH").split(":");
            try {
                String[] command = new String[paths.length];
                for (int i = 0; i < paths.length; i++) {
                    String path = paths[i];
                    if (!path.endsWith("/")) {
                        path += "/";
                    }
                    command[i] = "stat " + path + "su";
                }
                List<String> output = Shell.SH.run(command);
                for (String out : output) {
                    //found binary, call listener
                    if (out.contains("File:") && out.contains("su")) {
                        listener.onResult(true);
                        return null;
                    }

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            //binary not found
            listener.onResult(false);
            return null;
        }
    }

    public interface OnRootCheckResultListener {
        public void onResult(boolean result);
    }
}
