package com.android.kalite27;

import com.android.kalite27.config.GlobalConstants;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

// this is really a Singleton class
public class GlobalValues
{
    private static GlobalValues _instance = null;
    private static Context context;
    private int python_exit_code;
    private SharedPreferences prefs;
    private Editor editor;
    
//    public static void initialize(Context ctx) {
//    	if (_instance == null) {
//        	_instance = new GlobalValues();
//        }
//        context = ctx;
//    }
    
//    public static boolean hasBeenInitialized() {
//        return context != null;
//    }
    
    private GlobalValues() {
    	python_exit_code = 9999;
        // Use context to initialize the variables.
    }
    
	public void setPythonExitCode(int code){
		python_exit_code = code;
//		prefs = context.getSharedPreferences("MyPrefs", Context.MODE_MULTI_PROCESS);
//		editor = prefs.edit();
//		editor.putInt("python_exit_code", code);
//		editor.commit();
//		System.out.println("elielieli: " + python_exit_code);
		Log.d(GlobalConstants.LOG_TAG, "elielieli: exited with result code " + python_exit_code);
	}
	
	public int getPythonExitCode(){
		return python_exit_code;
	}
    
    public static synchronized GlobalValues getInstance() {
//        if (context == null) {
//        	throw new IllegalArgumentException("Impossible to get the instance. This class must be initialized before");
//        }

        if (_instance == null) {
        	_instance = new GlobalValues();
        }

        return _instance;
    }

}