package com.trianguloy.llscript.repository;


/**
 * Static constants used in the project (like R.strings, but also other types)
 */
public final class Constants {

    private Constants() {
    }

    //script flags
    public static final int FLAG_APP_MENU = 2;
    public static final int FLAG_ITEM_MENU = 4;
    public static final int FLAG_CUSTOM_MENU = 8;

    //App constants
    public static final int managerId = -3;

    //Where to search in the HTML source
    public static final String[] beginning = {"class=\"brush: javascript\">", "class=\"brush: javascript;\">", "class=\"code\">"};
    public static final String ending = "</pre>";

    //package constants
    public static final String activityRunScript = "net.pierrox.lightning_launcher.activities.Dashboard";
    public static final String[] packages = new String[]{"net.pierrox.lightning_launcher_extreme", "net.pierrox.lightning_launcher"};
    public static String installedPackage = "";
    public static final int minimumNecessaryVersion = 225; // valid for both packages. Number module 1000

    //Intent Extras
    public static final String extraCode = "code";
    public static final String extraFlags = "flags";
    public static final String extraReceiver = "receiver";
    public static final String extraName = "name";
    public static final String extraForceUpdate = "forceUpdate";
    public static final String extraOpenUrl = "openUrl";
    public static final String extraOpenUrlTime = "openUrlTime";
    public static final String extraReload = "reload";


    public static final String extraStatus = "status";

    public static final int STATUS_LAUNCHER_PROBLEM = 3;
    public static final int STATUS_UPDATE_CONFIRMATION_REQUIRED = 2;
    public static final int STATUS_OK = 1;

    //Run script parameters
    public static final String RunActionExtra = "a";
    public static final String RunDataExtra = "d";
    public static final int RunActionKey = 35;
    public static final String RunBackgroundExtra = "t";
    public static final int RunBackgroundKey = 3;

    //Script JSON names for script drop
    public static final String ScriptName = "name";
    public static final String ScriptFlags = "flags";
    public static final String ScriptForceUpdate = "forceUpdate";
    public static final String ScriptCode = "code";
    public static final String ScriptReturnResultTo = "returnTo";

    public static final int BUFFER_SIZE = 1024;
    public static final int TEN_MEGABYTE = 10 * 1024 * 1024;
    public static final int HUNDRED_MILLISECONDS = 100;
    public static final int FIVE_SECONDS = 5000;
    public static final int TWO_SECONDS = 2000; //equals default TOAST_LENGTH_SHORT
    public static final int RGB_MAX = 255;
    public static final int VERSIONCODE_MODULO = 1000;//versioncode difference between the two packages

}