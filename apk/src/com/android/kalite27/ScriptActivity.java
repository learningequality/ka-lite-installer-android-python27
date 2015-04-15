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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import android.util.Base64;
import android.util.Log;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ScriptActivity extends Activity {
	ProgressDialog myProgressDialog; 
	
	// Path is depending on the ka_lite.zip file
	private final String local_settings_path = "/kalite/local_settings.py";
	
	private SharedPreferences prefs;
	private SharedPreferences.OnSharedPreferenceChangeListener prefs_listener;
	private TextView ServerStatusTextView;
	private WebView wv;
	private Context context;
	  
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
	  
		
		ServerStatusTextView = (TextView)findViewById(R.id.ServerStatus);
		Log.d(GlobalConstants.LOG_TAG, "Process " + android.os.Process.myPid() + "LaunchingActivity is checked elieli.");
		
		// set the lauching ui
		setContentView(R.layout.activity_launching);
		
		// set the default file path
		TextView FileTextView = (TextView)findViewById(R.id.FileDirectory);
		FileTextView.setText(Environment.getExternalStorageDirectory().getPath());
		
		// install needed ?
    	boolean installNeeded = isInstallNeeded();
		
    	if(installNeeded) {
    	  //setContentView(R.layout.install);	
  		  new InstallAsyncTask().execute();
    	}
    	else {
    	    //finish();
    	}

		//onStart();
  }
	
	@Override
	protected void onStop() {
	    super.onStop();
//	    prefs.unregisterOnSharedPreferenceChangeListener(prefs_listener);
	}
	
	/**
	 * When user click start 
	 * @param view
	 */
	public void startServer(View view) {
		runScriptService();
	}
	
	/**
	 * When user click file browser
	 * @param view
	 */
	public void openDirPicker(View view) {
		Intent intent = new Intent(this, DirectoryPicker.class); 
		// optionally set options here 
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
			generate_local_settings(path);
			TextView FileTextView = (TextView)findViewById(R.id.FileDirectory);
			FileTextView.setText(path);
			// do stuff with path
		}
	}

	/**
	 * Overwrite the local_settings based on the file pick
	 * @param path
	 */
	private void generate_local_settings(String path){
		try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair RSA_key = keyGen.generateKeyPair();
            Key priavte_key = RSA_key.getPrivate();
            Key public_key = RSA_key.getPublic();
            
            byte[] publicKeyBytes = public_key.getEncoded();
            byte[] privateKeyBytes = priavte_key.getEncoded();
            
            String content_root = null;
            String content_data = null;
            
            String local_settings_destination = this.getFilesDir().getAbsolutePath() + local_settings_path;
            String database_path = "\nDATABASE_PATH = \"" + Environment.getExternalStorageDirectory().getPath() + "/kalite_essential/data.sqlite\"";
            
            content_root = "\nCONTENT_ROOT = \"" + path +"/content/\"";
            content_data = "\nCONTENT_DATA_PATH = \"" + path +"/data/\"";
            
            
            String gut ="CHANNEL = \"connectteaching\"" +
            "\nLOAD_KHAN_RESOURCES = False" +
            "\nLOCKDOWN = True" +   //jamie ask to add it, need to test
            "\nSESSION_IDLE_TIMEOUT = 0" + //jamie ask to add it, need to test
            "\nPDFJS = False" +
            database_path +
            content_root +
            content_data +
            "\nDEBUG = True" +
            "\nUSE_I18N = False" +
            "\nUSE_L10N = False" +
            "\nOWN_DEVICE_PUBLIC_KEY=" + "\"" + Base64.encodeToString(publicKeyBytes, 24, publicKeyBytes.length-24, Base64.DEFAULT).replace("\n", "\\n") + "\""
            + "\nOWN_DEVICE_PRIVATE_KEY=" +  "\"" + "-----BEGIN RSA PRIVATE KEY-----" + "\\n"
            + Base64.encodeToString(privateKeyBytes, 26, privateKeyBytes.length-26, Base64.DEFAULT).replace("\n", "\\n")
            + "-----END RSA PRIVATE KEY-----" + "\"";
            
            File old_local_settings = new File(local_settings_destination);
            if(old_local_settings.exists()){
                old_local_settings.delete();
            }
            
            File newFile = new File(local_settings_destination);
            if(!newFile.exists())
            {
                newFile.createNewFile();
                try
               	{
                    FileOutputStream fOut = new FileOutputStream(newFile);
                    OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                    myOutWriter.append(gut);
                    myOutWriter.close();
                    fOut.close();
                } catch(Exception e){}
            }
        } catch(Exception e) {
            System.out.println("RSA generating error");
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
	    	
		    //runScriptService();
		    //finish();
		   }
	   
	  }
	
  private void runScriptService() {
	  if(GlobalConstants.IS_FOREGROUND_SERVICE) {
		  startService(new Intent(this, ScriptService.class));
	  }
	  else {
		  startService(new Intent(this, BackgroundScriptService.class)); 
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
  
}
