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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import android.util.Base64;
import android.util.Log;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
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
	private String path;
	  
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
		
		// set the file path
		// first check if the user has setting saved
		File path_settings = new File(Environment.getExternalStorageDirectory().getPath() + 
				"/kalite_essential/content_settings.py");
        if(path_settings.exists()){
        	this.path = readCopyOfSettings(path_settings);
        	this.path = this.path.replaceAll("\n","");
        } else {
        	// if there is no setting saved, use the external storage
        	this.path = Environment.getExternalStorageDirectory().getPath();
        }
		TextView FileTextView = (TextView)findViewById(R.id.FileDirectory);
		FileTextView.setText(this.path);
		
		// install needed ?
    	boolean installNeeded = isInstallNeeded();
		
    	// first time running
    	if(installNeeded) {
    		// this will also call generate_local_settings after unzip library
  		  	new InstallAsyncTask().execute();
    	}
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
		if (check_directory(path)) {
			runScriptService();
		}
	}
	
	/**
	 * When user click file browser
	 * @param view
	 */
	public void openDirPicker(View view) {
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
					generate_local_settings();
					TextView FileTextView = (TextView)findViewById(R.id.FileDirectory);
					FileTextView.setText(path);
            	} else {
            		// TODO: the path is not changed
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
                .setMessage("The selected directory doesn't contain the data or content folder "+path+ "/data")
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
	
	/**
	 * Overwrite the local_settings based on the file pick
	 * @param path
	 */
	private void generate_local_settings(){
		try {
			// First check if there is RSA saved
			String RSA = "";
			File RSA_settings = new File(Environment.getExternalStorageDirectory().getPath() + "/kalite_essential/RSA_settings.py");
	        if(RSA_settings.exists()){
	        	RSA = readCopyOfSettings(RSA_settings);
	        } else {
	        	// if there is no RSA saved, generate new RSA
	        	RSA = generateRSA();
	        }
	        if (RSA.length() < 100) {
	        	RSA = generateRSA();
	        }
			
            String content_root = null;
            String content_data = null;
            
            // the location of local_settings.py
            String local_settings_destination = this.getFilesDir().getAbsolutePath() + local_settings_path;
            String database_path = "\nDATABASE_PATH = \"" + Environment.getExternalStorageDirectory().getPath() + "/kalite_essential/data.sqlite\"";
            
            content_root = "\nCONTENT_ROOT = \"" + path +"/content/\"";
            content_data = "\nCONTENT_DATA_PATH = \"" + path +"/data/\"";
            
            // setting info
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
            "\n" + RSA;
            
            // delete the old settings
            File old_local_settings = new File(local_settings_destination);
            if(old_local_settings.exists()){
                old_local_settings.delete();
            }
            // overwrite with new settings
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
                    makeCopyOfSettings(RSA,path);
                } catch(Exception e){
                	System.out.println("Failed to write file");
                }
            }
        } catch(Exception e) {
            System.out.println("Failed to write file");
        }
    }

	/**
	 * Generate RSA key pairs
	 * @return
	 */
	private String generateRSA() {
		String key = "";
		try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair RSA_key = keyGen.generateKeyPair();
            Key priavte_key = RSA_key.getPrivate();
            Key public_key = RSA_key.getPublic();
            
            byte[] publicKeyBytes = public_key.getEncoded();
            byte[] privateKeyBytes = priavte_key.getEncoded();
            
            key = "OWN_DEVICE_PUBLIC_KEY=" + "\"" + Base64.encodeToString(publicKeyBytes, 24, publicKeyBytes.length-24, Base64.DEFAULT).replace("\n", "\\n") + "\""
            + "\nOWN_DEVICE_PRIVATE_KEY=" +  "\"" + "-----BEGIN RSA PRIVATE KEY-----" + "\\n"
            + Base64.encodeToString(privateKeyBytes, 26, privateKeyBytes.length-26, Base64.DEFAULT).replace("\n", "\\n")
            + "-----END RSA PRIVATE KEY-----" + "\"";
		} catch(Exception e) {
            System.out.println("RSA generating error");
        }
		return key;
	}
	
	/**
	 * Read setting from local file
	 * @param file
	 * @return
	 */
	private String readCopyOfSettings(File file) {
		String settings = "";
		try {
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			StringBuffer stringBuffer = new StringBuffer();
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				stringBuffer.append(line);
				stringBuffer.append("\n");
			}
			fileReader.close();
			settings = stringBuffer.toString();
		} catch (IOException e) {
			System.out.println("Failed to read file");
		}
		return settings;
	}
	
	/**
	 * Make a local copy of the setting of RSA and path
	 * @param RSA
	 * @param content
	 */
	private void makeCopyOfSettings(String RSA, String content) {
		try {
			String externalStorage = Environment.getExternalStorageDirectory().getPath();
			String setting_folder = externalStorage + "/kalite_essential";
			File folder = new File(setting_folder);
			if (!folder.isDirectory()) {
				folder.mkdir();
			}
			String RSA_path = setting_folder + "/RSA_settings.py";
			String content_path = setting_folder + "/content_settings.py";
			File RSA_settings = new File(RSA_path);
			// only write RSA at first time
	        if (!RSA_settings.exists()){
	        	RSA_settings.createNewFile();
	            try
	           	{
	                FileOutputStream fOut = new FileOutputStream(RSA_settings);
	                OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
	                myOutWriter.append(RSA);
	                myOutWriter.close();
	                fOut.close();
	            } catch(Exception e){
	            	System.out.println("Failed to write file");
	            }
	        }
	        File content_settings = new File(content_path);
	        // overwrite path
	        if (content_settings.exists()){
	        	content_settings.delete();
	        } 
	        content_settings.createNewFile();
	        try
	        {
	        	FileOutputStream fOut = new FileOutputStream(content_settings);
	        	OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
	        	myOutWriter.append(content);
	        	myOutWriter.close();
	        	fOut.close();
	        } catch(Exception e){
	        	System.out.println("Failed to write file");
	        }
		} catch(Exception e) {
			System.out.println("Failed to create a local copy");
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
  		  	generate_local_settings();
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
