package com.daleharvey.mobilefuton;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.couchbase.android.CouchbaseMobile;
import com.couchbase.android.ICouchbaseDelegate;

public class MobileFutonActivity extends Activity {

	private final MobileFutonActivity self = this;
	protected static final String TAG = "CouchAppActivity";

	private ServiceConnection couchServiceConnection;
	private ProgressDialog installProgress;
	private WebView webView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		startCouch();
	}

	@Override
	public void onRestart() {
		super.onRestart();
		startCouch();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			unbindService(couchServiceConnection);
		} catch (IllegalArgumentException e) {
		}
	}

	private final ICouchbaseDelegate mCallback = new ICouchbaseDelegate() {
		@Override
		public void couchbaseStarted(String host, int port) {

			if (installProgress != null) {
				installProgress.dismiss();
			}

			String url = "http://" + host + ":" + Integer.toString(port) + "/";
		    String ip = getLocalIpAddress();
		    String param = (ip == null) ? "" : "?ip=" + ip;
		    
		    Log.v(TAG, "host: " + host + " ip: " + ip);
		    AndCouch dbTestResults = null;

		    // Load MobileFuton
		    try {
		    	dbTestResults = AndCouch.put(url + "mobilefuton", null);

		    } catch (JSONException e1) {
		    	// TODO Auto-generated catch block
		    	e1.printStackTrace();
		    }

		    if (dbTestResults.status != 412) {
		    	ensureLoadDoc("mobilefuton", url, "_design/mobilefuton", "mobilefuton.json");

		    	// Load coconut
		    	try {
		    		AndCouch.put(url + "coconut", null);
		    	} catch (JSONException e1) {
		    		// TODO Auto-generated catch block
		    		e1.printStackTrace();
		    	}
		    	try {
					ensureLoadDoc("coconut", url, "_design/coconut", "coconut.json.jpg");
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

		    	// Load contents of docs (formerly _docs)
		    	Log.v(TAG, "Getting a list of docs.");
		    	AssetManager assets = getAssets();
		    	String[] fileNames = null;
		    	try {
		    		fileNames = assets.list("docs");
		    		for (String fileName : fileNames) {
		    			Log.v(TAG, "File: " + fileName);
		    			ensureLoadDoc("coconut", url, null, "docs/" + fileName);
		    		}
		    		Log.v(TAG, "Launchng app at url: " + url);
		    	} catch (IOException e) {
		    		Log.v(TAG, "Error getting docs.");
		    		e.printStackTrace();
		    	}
		    }

			String couchAppUrl = url + "coconut/_design/coconut/index.html";
			//launchCouchApp(url + "coconut/_design/app/index.html" + param);
			launchCouchApp(couchAppUrl);
		}

//		public void installing(int completed, int total) {
//			ensureProgressDialog();
//			installProgress.setTitle("Installing");
//			installProgress.setProgress(completed);
//			installProgress.setMax(total);
//		}

		@Override
		public void exit(String error) {
			Log.v(TAG, error);
			couchError();
		}
	};

	private void ensureProgressDialog() {
		if (installProgress == null) {
			installProgress = new ProgressDialog(MobileFutonActivity.this);
			installProgress.setTitle(" ");
			installProgress.setCancelable(false);
			installProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			installProgress.show();
		}
	}

	private void startCouch() {
		CouchbaseMobile couch = new CouchbaseMobile(getBaseContext(), mCallback);

		try {
			couch.copyIniFile("mobilefuton.ini");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		couchServiceConnection = couch.startCouchbase();
	}

	private void couchError() {
		AlertDialog.Builder builder = new AlertDialog.Builder(self);
		builder.setMessage("Unknown Error")
				.setPositiveButton("Try Again?",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								startCouch();
							}
						})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								self.moveTaskToBack(true);
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void launchCouchApp(String url) {
		webView = new WebView(MobileFutonActivity.this);
		webView.setWebChromeClient(new WebChromeClient());
		webView.setWebViewClient(new CustomWebViewClient());
		webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		webView.getSettings().setDomStorageEnabled(true);

		webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

		webView.requestFocus(View.FOCUS_DOWN);
	    webView.setOnTouchListener(new View.OnTouchListener() {
	        @Override
	        public boolean onTouch(View v, MotionEvent event) {
	            switch (event.getAction()) {
	                case MotionEvent.ACTION_DOWN:
	                case MotionEvent.ACTION_UP:
	                    if (!v.hasFocus()) {
	                        v.requestFocus();
	                    }
	                    break;
	            }
	            return false;
	        }
	    });

		setContentView(webView);
		webView.loadUrl(url);
	};

	private class CustomWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
	    	webView.goBack();
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}

	public String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException ex) {
			ex.printStackTrace();
		}
		return null;
	}

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
	private void ensureLoadDoc(String dbName, String hostPortUrl, String docName, String fileName) {

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
}