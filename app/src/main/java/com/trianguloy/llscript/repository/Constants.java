package com.trianguloy.llscript.repository;


/**
 * Static constants used in the project (like R.strings, but also other types)
 */
public final class Constants {

    private Constants(){}

    //script flags
    public static final int FLAG_APP_MENU = 2;
    public static final int FLAG_ITEM_MENU = 4;
    public static final int FLAG_CUSTOM_MENU = 8;


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
    public static final int managerId = -3;

    //Where to search in the HTML source
    static final String[] beginning = {"class=\"brush: javascript\">", "class=\"brush: javascript;\">", "class=\"code\">"};
    static final String ending = "</pre>";

    //package constants
    public static final String activityRunScript = "net.pierrox.lightning_launcher.activities.Dashboard";
    public static final String[] packages = new String[]{"net.pierrox.lightning_launcher_extreme", "net.pierrox.lightning_launcher"};
    public static String installedPackage = "";
    public static final int minimumNecessaryVersion = 225; // valid for both packages. Number module 1000

    //sharedPrefs
    @Deprecated
    public static final String keyId = "id";

    //Intent Extras
    public static final String extraCode = "code";
    public static final String extraFlags = "flags";
    public static final String extraReceiver = "receiver";
    public static final String extraName = "name";
    public static final String extraForceUpdate = "forceUpdate";
    public static final String extraOpenUrl = "openUrl";
    public static final String extraOpenUrlTime = "openUrlTime";
    public static final String extraReload = "reload";


    public static final String extraLauncherProblem = "launcherProblem";

    //Run script parameters
    public static final String RunActionExtra = "a";
    public static final String RunDataExtra = "d";
    public static final int RunActionKey = 35;

    //Script JSON names for script drop
    public static final String ScriptName = "name";
    public static final String ScriptFlags = "flags";
    public static final String ScriptForceUpdate = "forceUpdate";
    public static final String ScriptCode = "code";
    public static final String ScriptReturnResultTo = "returnTo";

    //internal return values in AsyncTasks
    public static final int RESULT_OK = 1;
    public static final int RESULT_NETWORK_ERROR = 0;
    public static final int RESULT_BAD_LOGIN = -1;
}