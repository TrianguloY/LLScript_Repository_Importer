package com.trianguloy.llscript.repository.aidl;

// Declare any non-default types here with import statements
import com.trianguloy.llscript.repository.aidl.Script;
import com.trianguloy.llscript.repository.aidl.ICallback;

interface ILightningService {

    void importScript(in Script script, boolean overwriteIfExists,in ICallback callback);

    void runScript(int id,in String data, boolean background);

    void runAction(int action,in String data, boolean background);
}
