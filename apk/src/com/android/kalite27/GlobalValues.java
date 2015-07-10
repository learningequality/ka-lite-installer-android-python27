package com.android.kalite27;

import com.android.kalite27.config.GlobalConstants;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

// this is a Singleton class
public class GlobalValues {
    private static GlobalValues _instance = null;
    private static Context context;
    private int python_exit_code;
    private String kalite_command = "no command yet";
    private SharedPreferences prefs;
    private Editor editor;
    
    private GlobalValues() {
        python_exit_code = 9999;
    }

    public static void initialize(Context ctx) {
        if (_instance == null) {
            _instance = new GlobalValues();
        }
        context = ctx;
    }

    public static boolean hasBeenInitialized() {
        return context != null;
    }

    public String getCommand() {
        return kalite_command;
    }
    
    public int getPythonExitCode() {
        return python_exit_code;
    }

    public void setPythonExitCode(int code, String command) {
        python_exit_code = code;
        kalite_command = command;
        
        prefs = context.getSharedPreferences("MyPrefs", Context.MODE_MULTI_PROCESS);
        editor = prefs.edit();
        editor.putInt("python_exit_code", code);
        editor.putString("kalite_command", command);
        editor.putBoolean("from_process", true);
        editor.commit();
        Log.d(GlobalConstants.LOG_TAG, "elielieli: GlobalValues "+ python_exit_code);
    }

    public static synchronized GlobalValues getInstance() {
        if (context == null) {
            throw new IllegalArgumentException("Impossible to get the instance. This class must be initialized before");
        }

        if (_instance == null) {
            _instance = new GlobalValues();
        }

        return _instance;
    }

}