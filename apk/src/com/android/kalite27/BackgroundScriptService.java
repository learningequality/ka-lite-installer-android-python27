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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.android.kalite27.config.GlobalConstants;
import com.android.kalite27.process.MyScriptProcess;
import com.googlecode.android_scripting.AndroidProxy;
import com.googlecode.android_scripting.jsonrpc.RpcReceiverManager;
import com.googlecode.android_scripting.interpreter.InterpreterConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class BackgroundScriptService extends Service {
	private final CountDownLatch mLatch = new CountDownLatch(1);
	private IBinder mBinder;
	private MyScriptProcess myScriptProcess;
	
	private static BackgroundScriptService instance;
	private boolean killMe;
	private boolean swipekill;
	  
	private InterpreterConfiguration mInterpreterConfiguration = null;
	private RpcReceiverManager mFacadeManager;
    private AndroidProxy mProxy;
    
    private static Context context = null;
    static {
      instance = null;
    }

    // ------------------------------------------------------------------------------------------------------
	@Override
	public void onTaskRemoved(Intent rootIntent) {
		Log.d(GlobalConstants.LOG_TAG, "onTaskRemoved elieli is called.");
		swipekill = true;
	}
    
    // ------------------------------------------------------------------------------------------------------

	public class LocalBinder extends Binder {
		public BackgroundScriptService getService() {
			return BackgroundScriptService.this;
		}
	}

    // ------------------------------------------------------------------------------------------------------

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

    // ------------------------------------------------------------------------------------------------------

//	public BackgroundScriptService() {
//		mBinder = new LocalBinder();
//	}

    // ------------------------------------------------------------------------------------------------------

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

    // ------------------------------------------------------------------------------------------------------

    public static Context getAppContext() {
        return BackgroundScriptService.context;
    }
    
    // ------------------------------------------------------------------------------------------------------

	@Override
	public void onCreate() {
		super.onCreate();
		BackgroundScriptService.context = getApplicationContext();
		mBinder = new LocalBinder();
		swipekill = false;
	}

    // ------------------------------------------------------------------------------------------------------

	private void killProcess() {
		this.killMe = true;
	    instance = null;
	    if (myScriptProcess != null) {
	    	myScriptProcess.kill();
	    }
	}

    // ------------------------------------------------------------------------------------------------------
	
	@Override
	public void onStart(Intent intent, final int startId) {
		super.onStart(intent, startId);
	}

	// ------------------------------------------------------------------------------------------------------
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		super.onStartCommand(intent, flags, startId);
		killProcess();
		instance = this;
		this.killMe = false;
		if(!swipekill){
			if (intent != null && intent.getExtras() != null) {
				String kalite_command = intent.getExtras().getString("kalite_command");
				new startMyAsyncTask(kalite_command).execute(startId);
			}
			return START_STICKY;
		}else{
			new startMyAsyncTask("stop").execute(startId);
			return START_REDELIVER_INTENT;
		}
	}

    // ------------------------------------------------------------------------------------------------------

	public class startMyAsyncTask extends AsyncTask<Integer, Integer, Boolean> {
		private String kalite_command;

		public startMyAsyncTask(String s) {
			super();
			kalite_command = s;
		}

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected Boolean doInBackground(Integer... params) {
			startMyMain(params[0], kalite_command);
			return true;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
		}

		@Override
		protected void onPostExecute(Boolean installStatus) {
		}	   
	}

	// ------------------------------------------------------------------------------------------------------

	private void startMyMain(final int startId, String kalite_command) {

		String scriptName = GlobalConstants.PYTHON_MAIN_SCRIPT_NAME;
		scriptName = this.getFilesDir().getAbsolutePath() + "/" + scriptName;
		File script = new File(scriptName);
		final boolean selfTerminated;
		if(kalite_command.equals("stop")){
			selfTerminated = true;
		}else{
			selfTerminated = false;
		}
		// arguments
		ArrayList<String> args = new ArrayList<String>();
		args.add(scriptName);
		args.add(kalite_command);
		// args.add("--foreground");

		File pythonBinary = new File(this.getFilesDir().getAbsolutePath() + "/python/bin/python");

		// env var
		Map<String, String> environmentVariables = null;	
		environmentVariables = new HashMap<String, String>();
		environmentVariables.put("PYTHONPATH", Environment.getExternalStorageDirectory().getAbsolutePath()+ "/" + this.getPackageName() + "/extras/python" + ":" + this.getFilesDir().getAbsolutePath() + "/python/lib/python2.7/lib-dynload" + ":" + this.getFilesDir().getAbsolutePath() + "/python/lib/python2.7");		
		environmentVariables.put("TEMP", Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + this.getPackageName() + "/extras/tmp");		
		environmentVariables.put("PYTHONHOME", this.getFilesDir().getAbsolutePath() + "/python");		
		environmentVariables.put("LD_LIBRARY_PATH", this.getFilesDir().getAbsolutePath() + "/python/lib" + ":" + this.getFilesDir().getAbsolutePath() + "/python/lib/python2.7/lib-dynload");		
		
		// launch script
		mProxy = new AndroidProxy(this, null, true);
		mProxy.startLocal();
		mLatch.countDown();
	      
		myScriptProcess = MyScriptProcess.launchScript(script, mInterpreterConfiguration, mProxy, new Runnable() {
					@Override
					public void run() {
						Log.d(GlobalConstants.LOG_TAG, "Process "+android.os.Process.myPid()+" BackgroundScriptService elieli.");
						if(selfTerminated){
							mProxy.shutdown();
							stopSelf(startId);
							killProcess();
						}
						// hard force restart
//				        if (!ScriptService.this.killMe) {
//				        	startMyMain();				        	
//				        }
					}
				}, script.getParent(),  Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + this.getPackageName(), args, environmentVariables, pythonBinary);		
	}
	
    // ------------------------------------------------------------------------------------------------------

	RpcReceiverManager getRpcReceiverManager() throws InterruptedException {
		mLatch.await();
		
		if (mFacadeManager==null) { // Facade manage may not be available on startup.
		mFacadeManager = mProxy.getRpcReceiverManagerFactory()
		.getRpcReceiverManagers().get(0);
		}
		return mFacadeManager;
	}

    // ------------------------------------------------------------------------------------------------------
}