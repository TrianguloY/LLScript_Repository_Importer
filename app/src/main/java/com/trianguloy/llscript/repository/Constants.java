package com.trianguloy.llscript.repository;


/**
 * Static constants used in the project (like R.strings, but also other types)
 */
public final class Constants {


    private Constants() {
    }

    //App constants
    public static final int MANAGER_ID = -3;

    //Where to search in the HTML source
    public static final String SCRIPT_SELECTORS = "[class=brush: javascript],[class=brush: javascript;],[class=code]";

    //package constants
    public static final String[] PACKAGES = {"net.pierrox.lightning_launcher_extreme", "net.pierrox.lightning_launcher"};
    public static final int MINIMUM_NECESSARY_VERSION = 225; // valid for both packages. Number module 1000

    //Intent Extras
    public static final String EXTRA_CODE = "code";
    public static final String EXTRA_FLAGS = "flags";
    public static final String EXTRA_RECEIVER = "receiver";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_FORCE_UPDATE = "forceUpdate";
    public static final String EXTRA_OPEN_URL = "openUrl";
    public static final String EXTRA_OPEN_URL_TIME = "openUrlTime";
    public static final String EXTRA_RELOAD = "reload";
    public static final String EXTRA_FORWARD = "forward";
    public static final String EXTRA_BACKGROUND = "background";
    public static final String EXTRA_STATUS = "status";
    public static final String EXTRA_LOADED_SCRIPT_ID = "loadedScriptId";
    public static final String EXTRA_RUN_ONLY = "runOnly";
    public static final String EXTRA_RESULT = "result";

    public static final int STATUS_EVAL_FAILED = 4;
    public static final int STATUS_LAUNCHER_PROBLEM = 3;
    public static final int STATUS_UPDATE_CONFIRMATION_REQUIRED = 2;
    public static final int STATUS_OK = 1;

    //Run script parameters, matching lightning launcher constants
    public static final String EXTRA_ACTION = "a";
    public static final String EXTRA_DATA = "d";
    public static final int ACTION_RUN = 35;
    public static final String EXTRA_TARGET = "t";
    public static final int TARGET_BACKGROUND = 3;

    //Script JSON names for script drop
    public static final String KEY_NAME = "name";
    public static final String KEY_FLAGS = "flags";
    public static final String KEY_FORCE_UPDATE = "forceUpdate";
    public static final String KEY_CODE = "code";
    public static final String KEY_RETURN_RESULT_TO = "returnTo";
    public static final String KEY_CALLBACK_ID = "callbackId";;

    public static final int TEN_MEGABYTE = 10 * 1024 * 1024;
    public static final int HUNDRED_MILLISECONDS = 100;
    public static final int FIVE_SECONDS = 5000;
    public static final int TWO_SECONDS = 2000; //equals default TOAST_LENGTH_SHORT
    public static final int RGB_MAX = 255;
    public static final int VERSIONCODE_MODULO = 1000;//versioncode difference between the two packages

}