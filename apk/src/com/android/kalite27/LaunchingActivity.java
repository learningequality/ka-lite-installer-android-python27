package com.android.kalite27;

import com.android.kalite27.DirectoryPicker;
import com.android.kalite27.config.GlobalConstants;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

public class LaunchingActivity extends Activity {
	
	private SharedPreferences prefs;
	private SharedPreferences.OnSharedPreferenceChangeListener prefs_listener;
	private TextView ServerStatusTextView;
	private WebView wv;
	private Context context;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ServerStatusTextView = (TextView)findViewById(R.id.ServerStatus);
		Log.d(GlobalConstants.LOG_TAG, "Process " + android.os.Process.myPid() + "LaunchingActivity is checked elieli.");
//		context = this;
//		wv = new WebView(context);
//		WebSettings ws = wv.getSettings();
//		ws.setJavaScriptEnabled(true);
//		prefs = getSharedPreferences("MyPrefs", MODE_MULTI_PROCESS);
		
//		GlobalValues.initialize(this);
		
//		prefs_listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
//		  public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
//			  int server_status = prefs.getInt("python_exit_code", -7);
//			  switch(server_status) {
//			  case -7:
//				  ServerStatusTextView.setText("Please wait, server is starting up");
//				  break;
//			  case 0:
//				  ServerStatusTextView.setText("Server is running");
//				  wv.loadUrl("http://0.0.0.0:8008/");
//				  setContentView(wv);
//				  prefs.unregisterOnSharedPreferenceChangeListener(this);
//				  break;
//			  case 1:
//				  ServerStatusTextView.setText("Server is stopped (1)");
//				  break;
//			  case 4:
//				  ServerStatusTextView.setText("Server is starting up (4)");
//				  break;
//			  case 5:
//				  ServerStatusTextView.setText("Not responding (5)");
//				  break;
//			  case 6:
//				  ServerStatusTextView.setText("Failed to start (6)");
//				  break;
//			  case 7:
//				  ServerStatusTextView.setText("Unclean shutdown (7)");
//				  break;
//			  case 8:
//				  ServerStatusTextView.setText("Unknown KA Lite running on port (8)");
//				  break;
//			  case 9:
//				  ServerStatusTextView.setText("KA Lite server configuration error (9)");
//				  break;
//			  case 99:
//				  ServerStatusTextView.setText("Could not read PID file (99)");
//				  break;
//			  case 100:
//				  ServerStatusTextView.setText("Invalid PID file (100)");
//				  break;
//			  case 101:
//				  ServerStatusTextView.setText("Could not determine status (101)");
//				  break;
//			  }
//		  }
//		};

//		prefs.registerOnSharedPreferenceChangeListener(prefs_listener);
		
		setContentView(R.layout.activity_launching);
	}
	
	@Override
	protected void onStop() {
	    super.onStop();
//	    prefs.unregisterOnSharedPreferenceChangeListener(prefs_listener);
	}
	
	public void startServer(View view) {
		Intent intent = new Intent(this, ScriptActivity.class);
		startActivity(intent);
	}
	
	public void openDirPicker(View view) {
		Intent intent = new Intent(this, DirectoryPicker.class); 
		// optionally set options here 
		intent.putExtra(DirectoryPicker.START_DIR,"/storage");
		intent.putExtra(DirectoryPicker.ONLY_DIRS,true);
		startActivityForResult(intent, DirectoryPicker.PICK_DIRECTORY);
	}
	
	@Override 
	protected void onActivityResult(int requestCode, int resultCode, Intent data) { 
		if(requestCode == DirectoryPicker.PICK_DIRECTORY && resultCode == RESULT_OK) { 
			Bundle extras = data.getExtras(); 
			String path = (String) extras.get(DirectoryPicker.CHOSEN_DIRECTORY); 
			TextView FileTextView = (TextView)findViewById(R.id.FileDirectory);
			FileTextView.setText(path);
			// do stuff with path
		}
	}
}
