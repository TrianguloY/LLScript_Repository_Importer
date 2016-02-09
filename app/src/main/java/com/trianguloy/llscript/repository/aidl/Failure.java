package com.trianguloy.llscript.repository.aidl;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents an issue while importing a script
 * Created by Lukas on 30.01.2016.
 * SHARED CLASS, BE CAREFUL WHEN MODIFYING
 */
public enum Failure implements Parcelable {

    LAUNCHER_INVALID,
    INVALID_INPUT,
    SCRIPT_ALREADY_EXISTS,
    EVAL_FAILED;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name());
    }

    public static final Creator<Failure> CREATOR = new Creator<Failure>() {
        @Override
        public Failure createFromParcel(Parcel source) {
            return valueOf(source.readString());
        }

        @Override
        public Failure[] newArray(int size) {
            return new Failure[size];
        }
    };
}
