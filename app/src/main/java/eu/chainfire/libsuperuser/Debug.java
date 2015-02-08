/*
 * Copyright (C) 2012-2014 Jorrit "Chainfire" Jongma
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.chainfire.libsuperuser;

import android.os.Looper;
import android.util.Log;

import com.trianguloy.llscript.repository.BuildConfig;

/**
 * Utility class for logging and debug features that (by default) does nothing when not in debug mode
 */
class Debug {

    // ----- LOGGING -----

    private static final String TAG = "libsuperuser";

    private static final int LOG_GENERAL = 0x0001;
    private static final int LOG_COMMAND = 0x0002;
    private static final int LOG_OUTPUT = 0x0004;

    private static final int LOG_ALL = 0xFFFF;

    /**
     * <p>Log a message (internal)</p>
     * 
     * <p>Current debug and enabled logtypes decide what gets logged - 
     * even if a custom callback is registered</p>  
     * 
     * @param type Type of message to log
     * @param typeIndicator String indicator for message type
     * @param message The message to log
     */
    private static void logCommon(int type, String typeIndicator, String message) {
        int logTypes = LOG_ALL;
        if (BuildConfig.DEBUG && ((logTypes & type) == type)) {
                Log.d(TAG, "[" + TAG + "][" + typeIndicator + "]" + (!message.startsWith("[") && !message.startsWith(" ") ? " " : "") + message);
        }
    }	

    /**
     * <p>Log a "general" message</p>
     * 
     * <p>These messages are infrequent and mostly occur at startup/shutdown or on error</p>
     *
     */
    public static void log() {
        logCommon(LOG_GENERAL, "G", ShellOnMainThreadException.EXCEPTION_COMMAND);
    }

    /**
     * <p>Log a "per-command" message</p>
     * 
     * <p>This could produce a lot of output if the client runs many commands in the session</p>
     * 
     * @param message The message to log
     */
    public static void logCommand(String message) {
        logCommon(LOG_COMMAND, "C", message);
    }

    /**
     * <p>Log a line of stdout/stderr output</p>
     * 
     * <p>This could produce a lot of output if the shell commands are noisy</p>
     * 
     * @param message The message to log
     */
    public static void logOutput(String message) {
        logCommon(LOG_OUTPUT, "O", message);
    }


    // ----- SANITY CHECKS -----



    /**
     * <p>Are we running on the main thread ?</p>
     * 
     * @return Running on main thread ?
     */	
    public static boolean onMainThread() {
        return ((Looper.myLooper() != null) && (Looper.myLooper() == Looper.getMainLooper()));
    }

}
