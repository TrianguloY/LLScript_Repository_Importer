package com.trianguloy.llscript.repository;

import android.annotation.SuppressLint;

/**
 * Static constants used in the project (like R.strings, but also other types)
 * */
public class Constants {

    //The script appears in the lighting app menu.
    static final int FLAG_APP_MENU = 2;
    // The script appears in the item menu.
    static final int FLAG_ITEM_MENU = 4;
    // The script appears in the custom menu.
    static final int FLAG_CUSTOM_MENU = 8;
    //Id of the script
    static final String INTENT_EXTRA_SCRIPT_ID = "i";
    // Optional: or'ed combination of the FLAG_* constants above (default is 0)
    static final String INTENT_EXTRA_SCRIPT_FLAGS = "f";
    //Optional: name of the script (default is the activity name)
    static final String INTENT_EXTRA_SCRIPT_NAME = "n";
    //Optional: execute the script right after loading it (default is false)
    static final String INTENT_EXTRA_EXECUTE_ON_LOAD = "e";
    /**
     * Optional: delete the script right after loading and (presumably) executing it.
     * This is useful when the script is meant to configure the home screen, create items or
     * install some other scripts, and is no longer needed after this initial setup (default is false).
     */
    static final String INTENT_EXTRA_DELETE_AFTER_EXECUTION = "d";

    //App constants
    public static final int managerVersion = 8; //TODO: get this version directly from the raw file
    static final int notId = -1;

    //WebViewer constants
    static final String pageMain = "http://www.pierrox.net/android/applications/lightning_launcher/wiki/doku.php?id=script_repository";
    static final String pageRoot = "http://www.pierrox.net";
    static final String pagePrefix = "http://www.pierrox.net/android/applications/lightning_launcher/wiki/doku.php?id=script_";

    //Where to search in the HTML source
    static final String[] beginning = {"class=\"brush: javascript\">", "class=\"brush: javascript;\">", "class=\"code\">"};
    static final String ending = "</pre>";

    //package constants
    public static final String packageMain = "net.pierrox.lightning_launcher_extreme/net.pierrox.lightning_launcher.activities.Dashboard";
    @SuppressLint("SdCardPath")
    static public final String scriptsPath = "/data/data/net.pierrox.lightning_launcher_extreme/files/scripts/";
    static public final String packageTemplate = "net.pierrox.lightning_launcher.APPLY_TEMPLATE";

    static public int id = -1;
}