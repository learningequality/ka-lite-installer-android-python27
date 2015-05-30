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

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;

import com.android.kalite27.config.GlobalConstants;
import com.android.kalite27.support.Utils;
import com.googlecode.android_scripting.FileUtils;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.NoSuchMethodException;
import java.lang.IllegalAccessException;
import java.lang.reflect.InvocationTargetException;

import android.util.Log;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import org.xwalk.core.XWalkNavigationHistory;
import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkView;
import org.xwalk.core.XWalkPreferences;
import org.xwalk.core.internal.XWalkSettings;
import org.xwalk.core.internal.XWalkViewBridge;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ScriptActivity extends Activity {
	ProgressDialog myProgressDialog; 
	
	private SharedPreferences prefs;
	private SharedPreferences.OnSharedPreferenceChangeListener prefs_listener;
	private Editor editor;
	private RelativeLayout startView;
	private TextView ServerStatusTextView;
	private TextView FileTextView;
	private XWalkView wv;
	private String contentPath;
	private KaliteUtilities mUtilities;
	private Button retryButton;
	private ProgressBar spinner;
	private ProgressBar webProgressBar;
	private boolean isServerRunning = false;
	private boolean isFileBrowserClosed = true;
	private boolean isHeartViewClosed = true;
	private boolean isHomePageFirstTime = true;
	private ViewPager mViewPager;
	private static final int MAX_VIEWS = 5;
	private boolean isGuideClosed = false;
	private String installMessage = "";
	GlobalValues gv;
	  
	@SuppressLint("NewApi") @Override
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
		
		startView = (RelativeLayout) findViewById(R.id.startView);
		retryButton = (Button) findViewById(R.id.buttonStart);
		spinner = (ProgressBar)findViewById(R.id.progressBar);
		webProgressBar = (ProgressBar)findViewById(R.id.webProgressBar);
		ServerStatusTextView = (TextView)findViewById(R.id.ServerStatus);
		FileTextView = (TextView)findViewById(R.id.FileDirectory);
		
		// check internet
		new InternetCheckAsyncTask().execute();
				
		retryButton.setVisibility(View.INVISIBLE);
		
		// install needed ?
    	boolean installNeeded = isInstallNeeded();
    	
    	// first time running
    	if(installNeeded) {
    		// this will also call generate_local_settings after unzip library
    		spinner.setVisibility(View.INVISIBLE);
    		mViewPager = (ViewPager) findViewById(R.id.view_pager);
    		mViewPager.setVisibility(View.VISIBLE);
    		isGuideClosed = false;
            mViewPager.setAdapter(new GuidePagerAdapter());
            mViewPager.setOnPageChangeListener(new GuidePageChangeListener());
  		  	new InstallAsyncTask().execute();
    	}else{
    		contentPath = mUtilities.readContentPath(this);
        	contentPath = contentPath.replaceAll("\n","");
    		File contentFiles = new File(contentPath);
    		if(contentFiles.exists()){
    			FileTextView.setText("Content Location: " + contentPath);
    			FileTextView.setBackgroundColor(Color.parseColor("#A3CC7A"));
    			runScriptService("start");
    		}else{
    			spinner.setVisibility(View.INVISIBLE);
    			ServerStatusTextView.setText("Content does not exist");
      		  	ServerStatusTextView.setTextColor(Color.parseColor("#FF9966"));
    		}
		}

    	wv = (XWalkView)findViewById(R.id.webview);
    	wv.setVisibility(View.INVISIBLE);
    	wv.setLayerType(View.LAYER_TYPE_HARDWARE, null);
		wv.setResourceClient(new XWalkResourceClient(wv){
			@Override
			public void onLoadStarted(XWalkView view, String url){
				super.onLoadStarted(view, url);
				if(!isHeartViewClosed){
					wv.getNavigationHistory().clear();
				}
			}
			
			@Override
			public void onLoadFinished(XWalkView view, String url){
				super.onLoadFinished(view, url);
				webProgressBar.setVisibility(View.INVISIBLE);
				if(url.equals("http://0.0.0.0:8008/") && isHomePageFirstTime){
					isHomePageFirstTime = false;
					startView.setVisibility(View.GONE);
					view.setVisibility(View.VISIBLE);
					view.getNavigationHistory().clear();
				}
			}
			
			@Override
			public void onProgressChanged(XWalkView view, int progress) {
				webProgressBar.setProgress(progress);
				if(progress > 99){
					webProgressBar.setProgress(0);
		        }
			}
			
			@Override
			public boolean shouldOverrideUrlLoading(XWalkView view, String url){
				if(!isHomePageFirstTime) {
					webProgressBar.setVisibility(View.VISIBLE);
				}
				if(!isHeartViewClosed){
					webProgressBar.setVisibility(View.VISIBLE);
					wv.getNavigationHistory().clear();
				}
				return false;
			}
		});
		
//		XWalkPreferences.setValue("enable-javascript", true);
//		XWalkPreferences.setValue(XWalkPreferences.JAVASCRIPT_CAN_OPEN_WINDOW, true);
		XWalkPreferences.setValue(XWalkPreferences.SUPPORT_MULTIPLE_WINDOWS, false);
		/*
		 * !!! remember to disable REMOTE_DEBUGGING in production, it causes error message overflow
		 */
		if(GlobalConstants.IS_REMOTE_DEBUGGING){
			XWalkPreferences.setValue(XWalkPreferences.REMOTE_DEBUGGING, true);
		}
		
//		new PreCacheAsyncTask().execute();
		prefs = getSharedPreferences("MyPrefs", MODE_MULTI_PROCESS);
		editor = prefs.edit();
		//clean kalite_command from before, we are not using SharedPreferences in conventional way.
		editor.clear();
		editor.commit();

		prefs_listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			public void onSharedPreferenceChanged(SharedPreferences prefs,
					String key) {
				if(prefs.getBoolean("from_process", false)){
					editor.putBoolean("from_process", false);
					int server_status = prefs.getInt("python_exit_code", -7);
					String kalite_command = prefs.getString("kalite_command", "no command yet");
					
					if (server_status == 0) {  // 0 means the server is running
						isServerRunning = true;
						openWebViewIfAllConditionsMeet();
					}else if(server_status != 0 && kalite_command.equals("start") || kalite_command.equals("restart")){
						runScriptService("status");
					}else if(kalite_command.equals("status")){
						ServerStatusTextView.setText(mUtilities.exitCodeTranslate(server_status));
						ServerStatusTextView.setTextColor(Color.parseColor("#FF9966"));
						spinner.setVisibility(View.INVISIBLE);
						retryButton.setVisibility(View.VISIBLE);
					}
				}
			}
		};
		prefs.registerOnSharedPreferenceChangeListener(prefs_listener);
  }
	
	/*
	 * To make Stripe checkout work properly, we need to change the User Agent for our webview, because
	 * Stripe use it to define its behaviors.
	 * Crosswalk 10 don't support setUserAgentString, so we use Reflection to modify the User Agent.
	 */
	private void setWebViewUserAgent(XWalkView webView, String userAgent){
	    try{
	        Method ___getBridge = XWalkView.class.getDeclaredMethod("getBridge");
	        ___getBridge.setAccessible(true);
	        XWalkViewBridge xWalkViewBridge = null;
	        xWalkViewBridge = (XWalkViewBridge)___getBridge.invoke(webView);
	        XWalkSettings xWalkSettings = xWalkViewBridge.getSettings();
	        xWalkSettings.setUserAgentString(userAgent);
	    }catch (NoSuchMethodException e){
	        // Could not set user agent
	        e.printStackTrace();
	    }catch (IllegalAccessException e){
	        e.printStackTrace();
	    }catch (InvocationTargetException e){
	    	e.printStackTrace();
	    }
	}
	
	@Override
	public void onBackPressed() {
	    if (wv.getNavigationHistory().canGoBack()) {
	        wv.getNavigationHistory().navigate(XWalkNavigationHistory.Direction.BACKWARD, 1);
	    } else {
	    	if (!isHeartViewClosed) {
	    		isHeartViewClosed = true;
	    		wv.load("about:blank", null);
	    		webProgressBar.setVisibility(View.INVISIBLE);
	    		wv.setVisibility(View.INVISIBLE);
	    		openWebViewIfAllConditionsMeet();
	    	} else {
	    		mUtilities.quitDialog(this);
	    	}
	    }
	}
	/***
	public class PreCacheAsyncTask extends AsyncTask<Void, Void, Boolean> {
		String pre_cache;
		@Override
		protected Boolean doInBackground(Void... params) {
			pre_cache = "<script type='text/javascript'>\n" +
					mUtilities.readFromAssets(getApplicationContext(), "jquery-ui.min.js") +
					"\n</script>";
			return true;
		}
		
		@Override
		   protected void onPostExecute(Boolean installStatus) {
		   Log.e(GlobalConstants.LOG_TAG, "elieli Caching: "+pre_cache);
		   wv.loadDataWithBaseURL("http://0.0.0.0:8008/", pre_cache,"text/html","utf-8",null);
		   wv.clearHistory();
		}
	}
	***/
	private void openWebViewIfAllConditionsMeet(){
		if(isServerRunning && isFileBrowserClosed && isHeartViewClosed){
			wv.load("http://0.0.0.0:8008/", null);
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
	 * When user click heart
	 * @param view
	 */
	public void clickOnHeart(View view) {
		/*
		 * here we pretend our webview is a desktop Chrome
		 */
		String userAgent = "Chrome/42.0.2311.90";
		setWebViewUserAgent(wv, userAgent);
		
		isHeartViewClosed = false;
		wv.setVisibility(View.VISIBLE);
		wv.load("https://learningequality.org/give/", null);
	}
	
	/**
	 * When user click file browser
	 * @param view
	 */
	public void openDirPicker(View view) {
		Log.e(GlobalConstants.LOG_TAG, "elieli fileBrowser opend");
		isFileBrowserClosed = false;
		Intent intent = new Intent(this, DirectoryPicker.class); 
		// set options here 
		intent.putExtra(DirectoryPicker.START_DIR,Environment.getExternalStorageDirectory().getParentFile().getPath());
		intent.putExtra(DirectoryPicker.ONLY_DIRS,false);
		startActivityForResult(intent, DirectoryPicker.PICK_DIRECTORY);
	}
	
	/**
	 * When the file pick is finished
	 */
	@Override 
	protected void onActivityResult(int requestCode, int resultCode, Intent data) { 
		if(requestCode == DirectoryPicker.PICK_DIRECTORY){
			if(resultCode == RESULT_OK) { 
				Bundle extras = data.getExtras(); 
				String path = (String) extras.get(DirectoryPicker.CHOSEN_DIRECTORY); 
				// do stuff with path
	            if(check_directory(path)){
	            	// if the path is changed
	            	if (contentPath != path) {
	            		// set the local settings
	            		mUtilities.setContentPath(path, this);
						FileTextView.setText("Content location: " + path);
						FileTextView.setBackgroundColor(Color.parseColor("#A3CC7A"));
						ServerStatusTextView.setText("Starting server ... ");
						ServerStatusTextView.setTextColor(Color.parseColor("#005987"));
						spinner.setVisibility(View.VISIBLE);
						runScriptService("restart");
						isFileBrowserClosed = true;
	            	} else {
	            		// TODO: the path is not changed
	            		isFileBrowserClosed = true;
	            		openWebViewIfAllConditionsMeet();
	            	}
	            }
			}else{
				//exit file browser by pressing back buttom
				isFileBrowserClosed = true;
				openWebViewIfAllConditionsMeet();
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
                    	isFileBrowserClosed = true;
                    	openWebViewIfAllConditionsMeet();
                    }
                 })
                 .setOnCancelListener(new DialogInterface.OnCancelListener() {         
                	 @Override
                	 public void onCancel(DialogInterface dialog) {
                		 isFileBrowserClosed = true;
                		 openWebViewIfAllConditionsMeet();
                	 }
                 })
                .setOnKeyListener(new DialogInterface.OnKeyListener(){
					@Override
					public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
						if (keyCode == KeyEvent.KEYCODE_BACK && 
			                event.getAction() == KeyEvent.ACTION_UP && 
			                !event.isCanceled()) {
							Log.e(GlobalConstants.LOG_TAG, "elieli OnKeyListener dialog");
			                dialog.cancel();
			                isFileBrowserClosed = true;
			                openWebViewIfAllConditionsMeet();
			                return true;
			            }
						return false;
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
	   
	   public class InternetCheckAsyncTask extends AsyncTask<Void, Integer, Boolean> {

			@Override
			protected Boolean doInBackground(Void... arg0) {
				return mUtilities.hasInternetAccess(getApplicationContext());
			}
			
			@Override
			protected void onPostExecute(Boolean internetStatus) {
				ImageView heart = (ImageView)findViewById(R.id.heart);
				if (internetStatus) {
					heart.setVisibility(View.VISIBLE);
				} else {
					heart.setVisibility(View.GONE);
				}
			}
		  }
	   
	  public class InstallAsyncTask extends AsyncTask<Void, Integer, Boolean> {
		   @Override
		   protected void onPreExecute() {
		   }
	
		   @Override
		   protected Boolean doInBackground(Void... params) {	    
	    	Log.i(GlobalConstants.LOG_TAG, "Unpacking...");
	    	
	    	// show progress dialog
	    	if (isGuideClosed){
	    		sendmsg("showProgressDialog", "");

	    		sendmsg("setMessageProgressDialog", "Please wait...");
	    	} else {
	    		installMessage = "setMessageProgressDialog";
	    	}
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
			if (isGuideClosed) {
		    	sendmsg("dismissProgressDialog", "");
		    	
		    	if(installStatus) {
			    	sendmsg("installSucceed", "");
		    	}
		    	else {
			    	sendmsg("installFailed", "");
		    	}
			} else {
				if(installStatus) {
			    	installMessage = "installSucceed";
		    	}
		    	else {
		    		installMessage = "installFailed";
		    	} 
			}
  		  	mUtilities.generate_local_settings(getApplicationContext());
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
  protected void onPause() {
      super.onPause();
      if (wv != null) {
          wv.pauseTimers();
          wv.onHide();
      }
  }

  @Override
  protected void onResume() {
      super.onResume();
      if (wv != null) {
          wv.resumeTimers();
          wv.onShow();
      }
  }

  	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (wv != null) {
            wv.onDestroy();
        }
		Log.e(GlobalConstants.LOG_TAG, "main activity onDestroy is called elieli");
		runScriptService("stop");
	}
  
  	class GuidePagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return MAX_VIEWS+1;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == (View) object;
        }

        @Override
        public Object instantiateItem(View container, int position) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View imageViewContainer = inflater.inflate(R.layout.guide_single_view, null);
            ImageView imageView = (ImageView) imageViewContainer.findViewById(R.id.image_view);

            switch(position) {
            case 0:
                imageView.setImageResource(R.drawable.image1);
                break;

            case 1:
                imageView.setImageResource(R.drawable.image2);
                break;

            case 2:
                imageView.setImageResource(R.drawable.image3);
                break;

            case 3:
                imageView.setImageResource(R.drawable.image4);
                break;

            case 4:
                imageView.setImageResource(R.drawable.image5);
                break;
            case 5:
            	mViewPager.setVisibility(View.GONE);
            	isGuideClosed = true;
            	if (installMessage.equals("setMessageProgressDialog")) {
            		sendmsg("showProgressDialog", "");
    	    		sendmsg("setMessageProgressDialog", "Please wait...");
            	} else if (installMessage.equals("installFailed")){
            		sendmsg("installFailed", "");
            	}
                break;
            }

            ((ViewPager) container).addView(imageViewContainer, 0);
            return imageViewContainer;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            ((ViewPager)container).removeView((View)object);
        }
    }


    class GuidePageChangeListener implements ViewPager.OnPageChangeListener {

        @Override
        public void onPageScrollStateChanged(int arg0) {

        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        @Override
        public void onPageSelected(int position) {
            // Here is where you should show change the view of page indicator
            switch(position) {
            case MAX_VIEWS:
                break;
            default:
            }
        }
    }
}
