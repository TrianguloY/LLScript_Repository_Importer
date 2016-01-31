package com.trianguloy.llscript.repository.aidl;

// Declare any non-default types here with import statements
import com.trianguloy.llscript.repository.aidl.Failure;

interface ICallback {

     void onImportFinished(int scriptId);

     void onImportFailed(in Failure failure);
}
