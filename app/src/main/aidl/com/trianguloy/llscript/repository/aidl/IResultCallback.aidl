package com.trianguloy.llscript.repository.aidl;

// Declare any non-default types here with import statements
import com.trianguloy.llscript.repository.aidl.Failure;

interface IResultCallback {
    void onResult(String result);

    void onFailure(in Failure failure);
}
