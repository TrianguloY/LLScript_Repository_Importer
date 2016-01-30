package com.trianguloy.llscript.repository.aidl;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents an LL script
 * Created by Lukas on 30.01.2016.
 */
public class Script implements Parcelable {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, value = {FLAG_APP_MENU, FLAG_ITEM_MENU, FLAG_CUSTOM_MENU})
    public @interface ScriptFlag {
    }

    public static final int FLAG_APP_MENU = 1 << 1;
    public static final int FLAG_ITEM_MENU = 1 << 2;
    public static final int FLAG_CUSTOM_MENU = 1 << 3;

    private String code;
    private String name;
    @ScriptFlag
    private int flags;


    public Script(String code, String name, @ScriptFlag int flags) {
        this.code = code;
        this.name = name;
        this.flags = flags;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @ScriptFlag
    public int getFlags() {
        return flags;
    }

    public void setFlags(@ScriptFlag int flags) {
        this.flags = flags;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isValid() {
        if (code == null || code.equals("") || name == null || name.equals("") || name.length() > 100) {
            return false;
        }
        Pattern pattern = Pattern.compile("[<>:\"/\\|\\?\\*\\n\\t\\\\]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(name);
        return !matcher.find();
        //TODO also check code
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(code);
        dest.writeString(name);
        dest.writeInt(flags);
    }

    public static final Creator<Script> CREATOR = new Creator<Script>() {
        @Override
        public Script createFromParcel(Parcel in) {
            String code = in.readString();
            String name = in.readString();
            int flagsInsecure = in.readInt();
            int flags = 0;
            if ((flagsInsecure & FLAG_APP_MENU) != 0) flags |= FLAG_APP_MENU;
            if ((flagsInsecure & FLAG_ITEM_MENU) != 0) flags |= FLAG_ITEM_MENU;
            if ((flagsInsecure & FLAG_CUSTOM_MENU) != 0) flags |= FLAG_CUSTOM_MENU;
            return new Script(code, name, flags);
        }

        @Override
        public Script[] newArray(int size) {
            return new Script[size];
        }
    };
}
