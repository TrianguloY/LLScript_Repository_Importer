package com.trianguloy.llscript.repository;


/**
 * Static constants used in the project (like R.strings, but also other types)
 */
public class Constants {

    //script flags
    static final int FLAG_APP_MENU = 2;
    static final int FLAG_ITEM_MENU = 4;
    static final int FLAG_CUSTOM_MENU = 8;


    //Legacy variables. To remove with loadInLauncher
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
    static public final int managerId = -3;

    //Where to search in the HTML source
    static final String[] beginning = {"class=\"brush: javascript\">", "class=\"brush: javascript;\">", "class=\"code\">"};
    static final String ending = "</pre>";

    //package constants
    static public final String packageMain = "net.pierrox.lightning_launcher_extreme/net.pierrox.lightning_launcher.activities.Dashboard";
    static public final String[] packages = new String[]{"net.pierrox.lightning_launcher_extreme", "net.pierrox.lightning_launcher"};
    static public String installedPackage = "";
    static public final int minimumNecessaryVersion = 2225; // V12.2b2

    //sharedPrefs
    @Deprecated
    static public final String keyId = "id";

    //Intent Extras
    static public final String extraCode = "code";
    static public final String extraFlags = "flags";
    static public final String extraReceiver = "receiver";
    static public final String extraName = "name";
    static public final String extraForceUpdate = "forceUpdate";

    //Run script parameters
    static public final String RunActionExtra = "a";
    static public final String RunDataExtra = "d";
    static public final int RunActionKey = 35;

    //Script JSON names for script drop
    static public final String ScriptName = "name";
    static public final String ScriptFlags = "flags";
    static public final String ScriptForceUpdate = "forceUpdate";
    static public final String ScriptCode = "code";
    static public final String ScriptReturnResultTo = "returnTo";
}