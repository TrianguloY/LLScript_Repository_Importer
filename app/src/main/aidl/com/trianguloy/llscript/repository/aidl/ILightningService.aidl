// IBinder.aidl
package com.trianguloy.llscript.repository.aidl;

// Declare any non-default types here with import statements
import com.trianguloy.llscript.repository.aidl.Script;
import com.trianguloy.llscript.repository.aidl.ICallback;

interface ILightningService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void importScript(in Script script, boolean overwriteIfExists,in ICallback callback);

    void runScript(int id, String data, boolean background);

    void runAction(int action, String data, boolean background);
}
