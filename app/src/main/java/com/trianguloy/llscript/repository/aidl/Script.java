package com.trianguloy.llscript.repository.aidl;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents an LL script
 * SHARED CLASS, BE CAREFUL WHEN MODIFYING
 *
 * @author F43nd1r
 * @since 30.1.2016
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
    private String path;

    public Script(String code, String name, @ScriptFlag int flags){
        this(code, name, flags, "/");
    }

    public Script(String code, String name, @ScriptFlag int flags, String path) {
        this.code = code;
        this.name = name;
        this.flags = ensureFlags(flags);
        this.path = path;
    }

    public Script(Context context, @RawRes int codeRes, String name, @ScriptFlag int flags, String path) {
        this(rawResourceToString(context, codeRes), name, flags, path);
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
        this.flags = ensureFlags(flags);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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
    public void writeToParcel(Parcel dest, int f) {
        dest.writeString(code);
        dest.writeString(name);
        dest.writeInt(flags);
        dest.writeString(path);
    }

    public static final Creator<Script> CREATOR = new Creator<Script>() {
        @Override
        public Script createFromParcel(Parcel in) {
            String code = in.readString();
            String name = in.readString();
            int flagsInsecure = in.readInt();
            String path = in.readString();
            return new Script(code, name, ensureFlags(flagsInsecure), path);
        }

        @Override
        public Script[] newArray(int size) {
            return new Script[size];
        }
    };

    @Nullable
    private static String rawResourceToString(@NonNull Context context, @RawRes int rawRes) {
        try {
            InputStream inputStream = context.getResources().openRawResource(rawRes);
            byte[] bytes = new byte[1024];
            StringBuilder builder = new StringBuilder(inputStream.available());
            int count;
            while ((count = inputStream.read(bytes)) > 0) {
                builder.append(new String(bytes, 0, count));
            }
            inputStream.close();
            return builder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Script.ScriptFlag
    private static int ensureFlags(int flagsInsecure) {
        int flags = 0;
        if ((flagsInsecure & FLAG_APP_MENU) != 0) flags |= FLAG_APP_MENU;
        if ((flagsInsecure & FLAG_ITEM_MENU) != 0) flags |= FLAG_ITEM_MENU;
        if ((flagsInsecure & FLAG_CUSTOM_MENU) != 0) flags |= FLAG_CUSTOM_MENU;
        return flags;
    }
}
