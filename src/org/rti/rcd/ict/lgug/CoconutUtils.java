package org.rti.rcd.ict.lgug;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.res.AssetManager;
import android.util.Log;

import com.daleharvey.mobilefuton.AndCouch;

public class CoconutUtils extends Activity {
	
	protected static final String TAG = "CouchAppActivity";

	/**
	 *  Will check for the existence of a design doc and if it does not exist,
	 *  upload the json found at dataPath to create it
	 *  
	 *  if (dbName.equals(fileName)), then it is a design document and it compares the couchapphash to see if it needs updating
	 *  For other documents, it simply puts the file.
	 *  
	 * @param dbName - CouchDB name
	 * @param hostPortUrl - e.g.: http://0.0.0.0:5985
	 * @param docName - doc _id; dbName if null.
	 * @param fileName
	 */
	public void ensureLoadDoc(String dbName, String hostPortUrl, String docName, String fileName) {

		try {

			//Boolean toUpdate = true;
			Boolean dDoc = false;
			String data = null;
			data = readAsset(getAssets(), fileName);
			File hashCache = null;
			String md5 = null;

//			if (dbName.equals(fileName)) {
//				dDoc = true;
//				Log.v(TAG, fileName + " is a design document: " + docName + " .");
//			} else {
//				Log.v(TAG, fileName + " is not a design document.");
//			}
//
//			if (dDoc == true) {
//				hashCache = new File(CouchbaseMobile.dataPath() + "/couchapps/" + dbName + ".couchapphash");				
//				md5 = md5(data);
//				String cachedHash;
//
//				try {
//					cachedHash = readFile(hashCache);
//					toUpdate = !md5.equals(cachedHash);
//				} catch (Exception e) {
//					e.printStackTrace();
//					toUpdate = true;
//				}
//			} else {
//				//TODO: compare to version on server.
//				toUpdate = true;
//			}
			
			//Log.v(TAG, docName + " toUpdate: " + toUpdate);

			//if (toUpdate == true) {
				String docUrl = null;
				if (docName != null) {
					docUrl = hostPortUrl + dbName + "/" + docName;
				} else {
					JSONObject json = new JSONObject(data);
					docName = json.getString("_id");
					//docUrl = url + dbName + "/_design/" + dbName;
					docUrl = hostPortUrl + dbName + "/" + docName;
					Log.v(TAG, fileName + " has the docName: " + docName);
					Log.v(TAG, "docUrl: " + docUrl);
				}
				
				URL urlObject = new URL(docUrl);
				String protocol = urlObject.getProtocol();
				String hostName = urlObject.getHost();
				int port = urlObject.getPort();
				String path = urlObject.getPath();
				String queryString = urlObject.getQuery();
				
				URI uri = null;
				try {
					uri = new URI(
							protocol, 
							null, // userinfo
							hostName, 
							port,
							path,
							queryString,
					        null);
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//String cleanUrlA = uri.toString();
				//Log.v(TAG, "URL toString: " + cleanUrlA + " path: " + path);
				
//				URI uri2 = null; 
//				try {
//					uri2 = new URI(docUrl.replace(" ", "%20"));
//					//Log.v(TAG, "uri2: " + uri2);
//				} catch (URISyntaxException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
				
				String cleanUrl = uri.toASCIIString();

				AndCouch req = AndCouch.get(cleanUrl);
				Log.v(TAG, "cleanUrl: " + cleanUrl + " req.status: " + req.status);

				if (req.status == 404) {
					Log.v(TAG, "Uploading " + cleanUrl);
					//AndCouch.put(hostPortUrl + dbName, null);
					AndCouch.put(cleanUrl, data);
				} else if (req.status == 200) {
					Log.v(TAG, cleanUrl + " Found, Updating");
					String rev = req.json.getString("_rev");
					JSONObject json = new JSONObject(data);
					json.put("_rev", rev);
					//AndCouch.put(hostPortUrl + dbName, null);
					AndCouch.put(cleanUrl, json.toString());
				}

				if (dDoc == true) {
					new File(hashCache.getParent()).mkdirs();
					writeFile(hashCache, md5);
				}
//			} else {
//				Log.v(TAG, fileName + " is up to date.");
//			}

		} catch (IOException e) {
			e.printStackTrace();
			// There is no design doc to load
		} catch (JSONException e) {
			e.printStackTrace();
		}
	};

	
	public static String readAsset(AssetManager assets, String path) throws IOException {
		InputStream is = assets.open(path);
		int size = is.available();
		byte[] buffer = new byte[size];
		is.read(buffer);
		is.close();
		return new String(buffer);
	}

    public static String readFile(File file) throws IOException {
        StringBuffer fileData = new StringBuffer(1000);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        char[] buf = new char[1024];
        int numRead=0;
        while((numRead=reader.read(buf)) != -1){
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
        return fileData.toString();
    }

    public static void writeFile(File file, String data) throws IOException {
    	FileWriter fstream = new FileWriter(file);
    	BufferedWriter out = new BufferedWriter(fstream);
    	out.write(data);
    	out.close();
    }
    
    public static String md5(String input){
        String res = "";
        try {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(input.getBytes());
            byte[] md5 = algorithm.digest();
            String tmp = "";
            for (int i = 0; i < md5.length; i++) {
                tmp = (Integer.toHexString(0xFF & md5[i]));
                if (tmp.length() == 1) {
                    res += "0" + tmp;
                } else {
                    res += tmp;
                }
            }
        } catch (NoSuchAlgorithmException ex) {}
        return res;
    }
    
}
