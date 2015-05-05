package com.android.kalite27;

import com.android.kalite27.config.GlobalConstants;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import android.app.Activity;

public class KaliteUtilities {
	// Path is depending on the ka_lite.zip file
	private final String local_settings_path = "/kalite/local_settings.py";

	public String exitCodeTranslate(int server_status) {
		switch (server_status) {
		case -7:
			return "Please wait, server is starting up";
		case 0:
			return "Server is running";
		case 1:
			return "Server is stopped (1)";
		case 4:
			return "Server is starting up (4)";
		case 5:
			return "Not responding (5)";
		case 6:
			return "Failed to start (6)";
		case 7:
			return "Unclean shutdown (7)";
		case 8:
			return "Unknown KA Lite running on port (8)";
		case 9:
			return "KA Lite server configuration error (9)";
		case 99:
			return "Could not read PID file (99)";
		case 100:
			return "Invalid PID file (100)";
		case 101:
			return "Could not determine status (101)";
		}
		return "unknown python exit code";
	}
	
	private boolean isNetworkAvailable(Context context) {
	    ConnectivityManager connectivityManager 
	          = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}
	
	public boolean hasInternetAccess(Context context) {
	    if (isNetworkAvailable(context)) {
	        try {
	            HttpURLConnection urlc = (HttpURLConnection) 
	                (new URL("https://learningequality.org/give/")
	                .openConnection());
	            urlc.setRequestProperty("User-Agent", "Android");
	            urlc.setRequestProperty("Connection", "close");
	            // Waiting time
	            urlc.setConnectTimeout(15000); 
	            urlc.connect();
	            return (urlc.getResponseCode() == 200);
	        } catch (IOException e) {
	        	e.printStackTrace();
	        }
	    }
	    return false;
	}
	
	public void quitDialog(Context context){
		final Context c = context;
		new AlertDialog.Builder(context)
			.setTitle("Do you want to exit Ka Lite ?")
			.setPositiveButton("Exit", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					((Activity) c).finish();
            	}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					//do nothing
				}
        	})
        	.show();
	}
	
	public String readFromAssets(Context c, String file_name){
		String text = null;
		AssetManager assetManager = c.getAssets();
	    try {
			InputStream ips = assetManager.open(file_name);
			int size = ips.available();
			byte[] buffer = new byte[size];
			ips.read(buffer);
			ips.close();
			text = new String(buffer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return text;
	}
	
	/**
	 * Overwrite the local_settings based on the file pick
	 * @param path
	 */
	 void generate_local_settings(Context context){
		try {
			String externalStorage = Environment.getExternalStorageDirectory().getPath();
			String setting_folder = externalStorage + "/kalite_essential";
			File folder = new File(setting_folder);
			if(!folder.isDirectory()){
				folder.mkdirs();
			}
			String dataSqlite = externalStorage + "/kalite_essential/data.sqlite";
			File sqlite = new File(dataSqlite);
			if(!sqlite.exists()){
				sqlite.createNewFile();
			}
					
			// First check if there is RSA saved
			String RSA = "";
			File copy_settings = new File(Environment.getExternalStorageDirectory().getPath() + "/kalite_essential/local_settings.py");
	        if(copy_settings.exists()){
	        	RSA = readRSA(copy_settings);
	        } else {
	        	// if there is no RSA saved, generate new RSA
	        	RSA = generateRSA();
	        }
	        if (RSA.length() < 30) {
	        	RSA = generateRSA();
	        }
	        			
	        String content_root_khan = null;
            String content_root = null;
            String content_data = null;
            
            // the location of local_settings.py
            String local_settings_destination = context.getFilesDir().getAbsolutePath() + local_settings_path;
            String database_path = "\nDATABASE_PATH = \"" + Environment.getExternalStorageDirectory().getPath() + "/kalite_essential/data.sqlite\"";
            
            content_root_khan = "\nCONTENT_ROOT_KHAN = \"" + Environment.getExternalStorageDirectory().getPath()
            		+ "/com.android.kalite27/extras/khan\"";
            content_root = "\nCONTENT_ROOT = \"content root not specified yet\"";
            content_data = "\nCONTENT_DATA_PATH = \"content data not specified yet\"";
            
            // setting info
            String gut ="CHANNEL = \"khan\"" +
            "\nDO_NOT_RELOAD_CONTENT_CACHE_AT_STARTUP = True" +
            "\nLOAD_KHAN_RESOURCES = True" +
//            "\nLOCKDOWN = True" +   // current develop branch does not work with this setting, yet
//            "\nSESSION_IDLE_TIMEOUT = 0" + //jamie ask to add it, need to test
            "\nPDFJS = False" +
            database_path +
            content_root_khan +
            content_root +
            content_data +
            "\nDEBUG = True" +
//            "\nTARGET_OS = \"Android\"" +
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
					if(!copy_settings.exists()){
						makeCopyOfSettings(newFile);
					}
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
	
	public void setContentPath(String newPath, Context context){
		BufferedReader br = null;
	    BufferedWriter bw = null;
		String local_settings_old = context.getFilesDir().getAbsolutePath() + local_settings_path;
		String local_settings_temp = context.getFilesDir().getAbsolutePath() + "/kalite/local_settings_temp.py";
		try {
			br = new BufferedReader(new FileReader(local_settings_old));
	        bw = new BufferedWriter(new FileWriter(local_settings_temp));
	        String line;
	        while ((line = br.readLine()) != null) {
	            if (line.contains("CONTENT_ROOT =")){
	               line = "CONTENT_ROOT = \"" + newPath +"/content/\"";
	            } else if (line.contains("CONTENT_DATA_PATH =")){
	            	line = "CONTENT_DATA_PATH = \"" + newPath +"/data/\"";
	            }
	            bw.write(line+"\n");
	         }
		} catch (Exception e) {
	        System.out.println("Problem reading file.");
	    } finally {
	    	try {
	    		if(br != null){
	    			br.close();
	    		}
			} catch (IOException e) {}
	    	try {
	    		if(bw != null){
	    			bw.close();
	    		}
			} catch (IOException e) {}
	    }
		// Once everything is complete, delete old file..
		File oldFile = new File(local_settings_old);
		oldFile.delete();

		// And rename tmp file's name to old file name
		File newFile = new File(local_settings_temp);
		newFile.renameTo(oldFile);
	}
	
	String readContentPath(Context context) {
		String local_settings_destination = context.getFilesDir().getAbsolutePath() + local_settings_path;
		File internal_local_settings = new File(local_settings_destination);
		String path = "";
		String setting = readSetting(internal_local_settings);
//		if(setting.contains("not specified yet")){
//			return "Content not specified yet";
//		}
		String startStr = "CONTENT_ROOT = \"";
		int start = setting.indexOf(startStr);
		if (start != -1) {
			int end = setting.indexOf("\n",start);
			String content = "/content/\"";
			path = setting.substring(start+startStr.length(), end-content.length());
		}
		return path;
	}
	
	String readRSA(File file) {
		String RSA = "";
		String setting = readSetting(file);
		int start = setting.indexOf("OWN_DEVICE_PUBLIC_KEY");
		if (start != -1) {
			String endStr = "-----END RSA PRIVATE KEY-----\"";
			int end = setting.indexOf(endStr);
			if (end > start) {
				end += endStr.length();
				RSA = setting.substring(start, end);
			}
		}
		return RSA;
	}
	
	private String readSetting(File file) {
		String setting = "";
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
			setting = stringBuffer.toString();
		} catch (IOException e) {
			System.out.println("Failed to read file");
		}
		return setting;
	}
	/**
	 * Read setting from local file
	 * @param file
	 * @return
	 
	String readCopyOfSettings(File file) {
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
	}*/
	
	/**
	 * Make a local copy of the setting of RSA and path
	 * @param RSA
	 * @param content
	 */
	private void makeCopyOfSettings(File file) {
		try {
			String externalStorage = Environment.getExternalStorageDirectory().getPath();
			String setting_folder = externalStorage + "/kalite_essential";
			File folder = new File(setting_folder);
			if (!folder.isDirectory()) {
				folder.mkdirs();
			}
			String copy_path = setting_folder + "/local_settings.py";
			File copy_settings = new File(copy_path);

	        copy_settings.createNewFile();
	        String settings = readSetting(file);
	        try
	        {
	        	FileOutputStream fOut = new FileOutputStream(copy_settings);
	        	OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
	        	myOutWriter.append(settings);
	        	myOutWriter.close();
	        	fOut.close();
	        } catch(Exception e){
	        	System.out.println("Failed to write file");
	        }
		} catch(Exception e) {
			System.out.println("Failed to create a local copy");
		}
	}
}

class MyWebChromeClient extends WebChromeClient{
	ProgressBar progressBar;
	public MyWebChromeClient(ProgressBar pb){
		progressBar = pb;
	}
	@Override
	public void onProgressChanged(WebView view, int progress) {
		progressBar.setVisibility(View.VISIBLE);
    	progressBar.setProgress(progress);

		if(progress == 100){
			progressBar.setVisibility(View.GONE);
        }
	}
}
