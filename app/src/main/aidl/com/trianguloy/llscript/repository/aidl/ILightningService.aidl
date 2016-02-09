package com.trianguloy.llscript.repository.aidl;

// Declare any non-default types here with import statements
import com.trianguloy.llscript.repository.aidl.Script;
import com.trianguloy.llscript.repository.aidl.IImportCallback;
import com.trianguloy.llscript.repository.aidl.IResultCallback;

interface ILightningService {

    void importScript(in Script script, boolean overwriteIfExists,in IImportCallback callback);

    void runScript(int id, in String data, boolean background);

    void runScriptForResult(in String code, in IResultCallback callback);

    void runAction(int action,in String data, boolean background);
}
