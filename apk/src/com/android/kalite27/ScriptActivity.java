/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/*
 * Copyright (C) 2012, Anthony Prieur & Daniel Oppenheim. All rights reserved.
 *
 * Original from SL4A modified to allow to embed Interpreter and scripts into an APK
 */

package com.android.kalite27;

import com.android.kalite27.config.GlobalConstants;
import com.android.kalite27.support.Utils;
import com.googlecode.android_scripting.FileUtils;

import java.io.File;
import java.io.InputStream;

import android.util.Log;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class ScriptActivity extends Activity {
	ProgressDialog myProgressDialog; 
	
	// Path is depending on the ka_lite.zip file
	private final String local_settings_path = "/kalite/local_settings.py";
	
	private SharedPreferences prefs;
	private SharedPreferences.OnSharedPreferenceChangeListener prefs_listener;
	private TextView ServerStatusTextView;
	private TextView FileTextView;
	private WebView wv;
	private String path;
	private KaliteUtilities mUtilities;
	private Button retryButton;
	private ProgressBar spinner;
	private boolean OpenWebViewConditionA = false;
	private boolean OpenWebViewConditionB = true;
	GlobalValues gv;
	  
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// mounted sdcard ?
		//if (!Environment.getExternalStorageState().equals("mounted")) {
		//  Log.e(GlobalConstants.LOG_TAG, "External storage is not mounted");
		//  
		//  Toast toast = Toast.makeText( getApplicationContext(), "External storage not mounted", Toast.LENGTH_LONG);
		//  toast.show();
		//  return;
		//}
	  	mUtilities = new KaliteUtilities();
	  	GlobalValues.initialize(this);
		gv = GlobalValues.getInstance();
		
		// set the lauching ui
		setContentView(R.layout.activity_launching);
		
		retryButton = (Button) findViewById(R.id.buttonStart);
		retryButton.setVisibility(View.INVISIBLE);
		spinner = (ProgressBar)findViewById(R.id.progressBar);
		
		// set the file path
		// first check if the user has setting saved
		File copy_settings = new File(Environment.getExternalStorageDirectory().getPath() + 
				"/kalite_essential/local_settings.py");
        if(copy_settings.exists()){
        	this.path = mUtilities.readPath(copy_settings);
        	this.path = this.path.replaceAll("\n","");
        } else {
        	// if there is no setting saved, use the external storage
        	this.path = Environment.getExternalStorageDirectory().getPath();
        }
		FileTextView = (TextView)findViewById(R.id.FileDirectory);
		if(path.length() != 0){
			FileTextView.setText("Content Location: " + this.path);
			FileTextView.setBackgroundColor(Color.parseColor("#A3CC7A"));
		}
		
		// install needed ?
    	boolean installNeeded = isInstallNeeded();
		
    	// first time running
    	if(installNeeded) {
    		// this will also call generate_local_settings after unzip library
    		spinner.setVisibility(View.INVISIBLE);
  		  	new InstallAsyncTask().execute();
    	}else{
			runScriptService("start");
		}

		ServerStatusTextView = (TextView)findViewById(R.id.ServerStatus);
		wv = new WebView(this);
		WebSettings ws = wv.getSettings();
		ws.setJavaScriptEnabled(true);
		wv.setWebChromeClient(new WebChromeClient());
		wv.setWebViewClient(new WebViewClient());
		prefs = getSharedPreferences("MyPrefs", MODE_MULTI_PROCESS);

		// new
		// ExitCodeAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

		prefs_listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			public void onSharedPreferenceChanged(SharedPreferences prefs,
					String key) {
				int server_status = prefs.getInt("python_exit_code", -7);
				String kalite_command = prefs.getString("kalite_command", "no command yet");
				
				if (server_status == 0) {  // 0 means the server is running
					OpenWebViewConditionA = true;
					openWebViewIfMeetAllConditions();
				}else if(server_status != 0 && kalite_command.equals("start")){
					runScriptService("status");
				}else if(kalite_command.equals("status")){
					ServerStatusTextView.setText(mUtilities.exitCodeMatch(server_status));
					ServerStatusTextView.setTextColor(Color.parseColor("#FF9966"));
					spinner.setVisibility(View.INVISIBLE);
					retryButton.setVisibility(View.VISIBLE);
				}
			}
		};
		prefs.registerOnSharedPreferenceChangeListener(prefs_listener);
  }
	
// 	@Override
// 	protected void onStop() {
// 	    super.onStop();
//	    prefs.unregisterOnSharedPreferenceChangeListener(prefs_listener);
// 	}
	
	private void openWebViewIfMeetAllConditions(){
		if(OpenWebViewConditionA && OpenWebViewConditionB){
			spinner.setVisibility(View.GONE);
			wv.loadUrl("http://0.0.0.0:8008/");
			setContentView(wv);
			prefs.unregisterOnSharedPreferenceChangeListener(prefs_listener);
		}
	}
	
	/**
	 * When user click start 
	 * @param view
	 */
	public void startServer(View view) {
		retryButton.setVisibility(View.INVISIBLE);
		spinner.setVisibility(View.VISIBLE);
		ServerStatusTextView.setText("Retry to start the server ... ");
		runScriptService("start");
	}
	
	/**
	 * When user click file browser
	 * @param view
	 */
	public void openDirPicker(View view) {
		OpenWebViewConditionB = false;
		Intent intent = new Intent(this, DirectoryPicker.class); 
		// set options here 
		intent.putExtra(DirectoryPicker.START_DIR,Environment.getExternalStorageDirectory().getPath());
		intent.putExtra(DirectoryPicker.ONLY_DIRS,true);
		startActivityForResult(intent, DirectoryPicker.PICK_DIRECTORY);
	}
	
	/**
	 * When the file pick is finished
	 */
	@Override 
	protected void onActivityResult(int requestCode, int resultCode, Intent data) { 
		if(requestCode == DirectoryPicker.PICK_DIRECTORY && resultCode == RESULT_OK) { 
			Bundle extras = data.getExtras(); 
			String path = (String) extras.get(DirectoryPicker.CHOSEN_DIRECTORY); 
			// do stuff with path
            if(check_directory(path)){
            	// if the path is changed
            	if (this.path != path) {
            		this.path = path;
	            	// set the local settings
					mUtilities.generate_local_settings(path, this);
					FileTextView.setText("Content location: " + path);
					FileTextView.setBackgroundColor(Color.parseColor("#A3CC7A"));
					ServerStatusTextView.setText("Starting server ... ");
					ServerStatusTextView.setTextColor(Color.parseColor("#005987"));
					spinner.setVisibility(View.VISIBLE);
					runScriptService("start");
					OpenWebViewConditionB = true;
					openWebViewIfMeetAllConditions();
            	} else {
            		// TODO: the path is not changed
            		OpenWebViewConditionB = true;
            		openWebViewIfMeetAllConditions();
            	}
            }
		}
	}

	/**
	 * Check if the path contains a data and a content folder
	 * @param path
	 * @return
	 */
	private boolean check_directory(String path){
		File data_file = new File(path + "/data");
		File content_file = new File(path + "/content");
		// if the directory doesn't contain data or content folder, alert
        if(!data_file.exists() || !content_file.exists()){
        	new AlertDialog.Builder(this)
                .setTitle("Invalid Directory")
                .setMessage("The selected directory doesn't contain the data or content folder")
                .setPositiveButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) { 
                    }
                 })
                .show();
        	return false;
        }
        else {
        	return true;
        }
	}
	
	private void sendmsg(String key, String value) {
	      Message message = installerHandler.obtainMessage();
	      Bundle bundle = new Bundle();
	      bundle.putString(key, value);
	      message.setData(bundle);
	      installerHandler.sendMessage(message);
	   }
	    
	   final Handler installerHandler = new Handler() {
	   @Override
	   public void handleMessage(Message message) {
		        Bundle bundle = message.getData();
		        
		        if (bundle.containsKey("showProgressDialog")) {
		 	       myProgressDialog = ProgressDialog.show(ScriptActivity.this, "Installing", "Loading", true); 
		        }
		        else if (bundle.containsKey("setMessageProgressDialog")) {
		        	if (myProgressDialog.isShowing()) {
			        	myProgressDialog.setMessage(bundle.getString("setMessageProgressDialog"));
		        	}
		        }
		        else if (bundle.containsKey("dismissProgressDialog")) {
		        	if (myProgressDialog.isShowing()) {
			        	myProgressDialog.dismiss();
		        	}
		        }
		        else if (bundle.containsKey("installSucceed")) {
		  		  Toast toast = Toast.makeText( getApplicationContext(), "Install Succeed", Toast.LENGTH_LONG);
				  toast.show();
		        }
		        else if (bundle.containsKey("installFailed")) {
			  		  Toast toast = Toast.makeText( getApplicationContext(), "Install Failed. Please check logs.", Toast.LENGTH_LONG);
					  toast.show();
			    }
	       }
	   };
	   
	  public class InstallAsyncTask extends AsyncTask<Void, Integer, Boolean> {
		   @Override
		   protected void onPreExecute() {
		   }
	
		   @Override
		   protected Boolean doInBackground(Void... params) {	    
	    	Log.i(GlobalConstants.LOG_TAG, "Installing...");

	    	// show progress dialog
	    	sendmsg("showProgressDialog", "");

	    	sendmsg("setMessageProgressDialog", "Please wait...");
	    	createOurExternalStorageRootDir();
	
			// Copy all resources
			copyResourcesToLocal();
	
			// TODO
		    return true;
		   }
	
		   @Override
		   protected void onProgressUpdate(Integer... values) {
		   }
	
		   @Override
		   protected void onPostExecute(Boolean installStatus) {
	    	sendmsg("dismissProgressDialog", "");
	    	
	    	if(installStatus) {
		    	sendmsg("installSucceed", "");
	    	}
	    	else {
		    	sendmsg("installFailed", "");
	    	}
  		  	mUtilities.generate_local_settings(path, getApplicationContext());

  		  	ServerStatusTextView.setText("No Content Available");
  		  	ServerStatusTextView.setTextColor(Color.parseColor("#FF9966"));
		   }
	   
	  }
	
  private void runScriptService(String kalite_command) {
	  if(GlobalConstants.IS_FOREGROUND_SERVICE) {
		  startService(new Intent(this, ScriptService.class));
	  }
	  else {
		  startService(new Intent(this, BackgroundScriptService.class).putExtra("kalite_command", kalite_command));
	  }
  }
  
	private void createOurExternalStorageRootDir() {
		Utils.createDirectoryOnExternalStorage( this.getPackageName() );
	}
	
	// quick and dirty: only test a file
	private boolean isInstallNeeded() {
		File testedFile = new File(this.getFilesDir().getAbsolutePath()+ "/" + GlobalConstants.PYTHON_MAIN_SCRIPT_NAME);
		if(!testedFile.exists()) {
			return true;
		}
		return false;
	}
	
	
	 private void copyResourcesToLocal() {
			String name, sFileName;
			InputStream content;
			
			R.raw a = new R.raw();
			java.lang.reflect.Field[] t = R.raw.class.getFields();
			Resources resources = getResources();
			
			boolean succeed = true;
			
			for (int i = 0; i < t.length; i++) {
				try {
					name = resources.getText(t[i].getInt(a)).toString();
					sFileName = name.substring(name.lastIndexOf('/') + 1, name.length());
					content = getResources().openRawResource(t[i].getInt(a));
					content.reset();

					// python project
					if(sFileName.endsWith(GlobalConstants.PYTHON_PROJECT_ZIP_NAME)) {
						succeed &= Utils.unzip(content, this.getFilesDir().getAbsolutePath()+ "/", true);
					}
					// python -> /data/data/com.android.python27/files/python
					else if (sFileName.endsWith(GlobalConstants.PYTHON_ZIP_NAME)) {
						succeed &= Utils.unzip(content, this.getFilesDir().getAbsolutePath()+ "/", true);
						FileUtils.chmod(new File(this.getFilesDir().getAbsolutePath()+ "/python/bin/python" ), 0755);
					}
					// python extras -> /sdcard/com.android.python27/extras/python
					else if (sFileName.endsWith(GlobalConstants.PYTHON_EXTRAS_ZIP_NAME)) {
						Utils.createDirectoryOnExternalStorage( this.getPackageName() + "/" + "extras");
						Utils.createDirectoryOnExternalStorage( this.getPackageName() + "/" + "extras" + "/" + "tmp");
						succeed &= Utils.unzip(content, Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + this.getPackageName() + "/extras/", true);
					}
					
				} catch (Exception e) {
					Log.e(GlobalConstants.LOG_TAG, "Failed to copyResourcesToLocal", e);
					succeed = false;
				}
			} // end for all files in res/raw
			
	 }

  @Override
  protected void onStart() {
	  super.onStart();
	
	  String s = "System infos:";
	  s += " OS Version: " + System.getProperty("os.version") + "(" + android.os.Build.VERSION.INCREMENTAL + ")";
	  s += " | OS API Level: " + android.os.Build.VERSION.SDK;
	  s += " | Device: " + android.os.Build.DEVICE;
	  s += " | Model (and Product): " + android.os.Build.MODEL + " ("+ android.os.Build.PRODUCT + ")";
	  
	  Log.i(GlobalConstants.LOG_TAG, s);

	  //finish();
  }

  	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.e(GlobalConstants.LOG_TAG, "main activity onDestroy is called elieli");
		runScriptService("stop");
	}
  
}
