package com.android.kalite27;

import android.content.Context;
import android.os.Environment;
import android.util.Base64;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

public class KaliteUtilities {
	// Path is depending on the ka_lite.zip file
	private final String local_settings_path = "/kalite/local_settings.py";

	public String exitCodeMatch(int server_status) {
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
	
	/**
	 * Overwrite the local_settings based on the file pick
	 * @param path
	 */
	void generate_local_settings(String path, Context context){
		try {
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
			
            String content_root = null;
            String content_data = null;
            
            // the location of local_settings.py
            String local_settings_destination = context.getFilesDir().getAbsolutePath() + local_settings_path;
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
                    makeCopyOfSettings(newFile);
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
	
	String readPath(File file) {
		String path = "";
		String setting = readSetting(file);
		String startStr = "CONTENT_ROOT = \"";
		int start = setting.indexOf(startStr);
		if (start != -1) {
			int end = setting.indexOf("/content/\"");
			path = setting.substring(start+startStr.length(), end);
		}
		return path;
	}
	
	String readRSA(File file) {
		String RSA = "";
		String setting = readSetting(file);
		int start = setting.indexOf("OWN_DEVICE_PUBLIC_KEY");
		if (start != -1) {
			String endStr = "-----END RSA PRIVATE KEY-----";
			int end = setting.indexOf(endStr) + endStr.length();
			RSA = setting.substring(start, end);
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
				folder.mkdir();
			}
			String copy_path = setting_folder + "/local_settings.py";
			File copy_settings = new File(copy_path);
	        // overwrite copy
	        if (copy_settings.exists()){
	        	copy_settings.delete();
	        } 
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
