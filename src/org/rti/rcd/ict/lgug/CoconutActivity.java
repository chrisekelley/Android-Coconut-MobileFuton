package org.rti.rcd.ict.lgug;

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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.rti.rcd.ict.lgug.Push.AddMessageTask;
import org.rti.rcd.ict.lgug.Push.ToastMessage;
import org.rti.rcd.ict.lgug.c2dm.Config;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.couchbase.android.CouchbaseMobile;
import com.couchbase.android.ICouchbaseDelegate;
import com.daleharvey.mobilefuton.AndCouch;
import com.google.android.c2dm.C2DMessaging;

public class CoconutActivity extends Activity {

	private final CoconutActivity self = this;
	protected static final String TAG = "CouchAppActivity";
    private static final String LOG_TAG = "CouchAppActivity";

	private static final int ACTIVITY_ACCOUNTS = 1;
    private static final int MENU_ACCOUNTS = 2;    

	private CouchbaseMobile couch;
	private ServiceConnection couchServiceConnection;
	private WebView webView;
	
    private ArrayList<HashMap<String,String>> list = new ArrayList<HashMap<String,String>>();
    private SimpleAdapter messages;
    private Handler uiHandler;
    private Account selectedAccount;
    private boolean registered;
    private static CoconutActivity coconutRef;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		startCouch();
		uiHandler = new Handler();
		coconutRef = this;
	}
	
	@Override
    protected void onActivityResult( int requestCode,
                                        int resultCode, 
                                        Intent extras ) {
        super.onActivityResult( requestCode, resultCode, extras);
        switch(requestCode) {
            case ACTIVITY_ACCOUNTS: {
                    String accountName = extras.getStringExtra( "account" );
                    selectedAccount = getAccountFromAccountName( accountName );
                    Toast.makeText(this, "Account selected: "+accountName, Toast.LENGTH_SHORT).show();
                    if( selectedAccount != null )
                        register();
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        menu.add( Menu.NONE, MENU_ACCOUNTS, Menu.NONE, R.string.menu_accounts );
        return result;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        switch ( item.getItemId() ) {
            case MENU_ACCOUNTS: {
                    Intent i = new Intent();
                    i.setClassName( 
                        "org.rti.rcd.ict.lgug",
                        "org.rti.rcd.ict.lgug.AccountSelector" );
                    startActivityForResult(i, ACTIVITY_ACCOUNTS );
                    return true;
                }
        }
        return false;
    }
    
    public static CoconutActivity getRef() {
        return coconutRef;
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
	
	 public void displayMessage( String message ) {
	        //uiHandler.post( new AddMessageTask( message ) );
	        uiHandler.post( new ToastMessage( this, message ) );
	    }

	    public Account getSelectedAccount() {
	        return selectedAccount;
	    }

	    public void onRegistered() {
	        Log.d( LOG_TAG, "onRegistered" );
	        registered = true; 
	        uiHandler.post( new ToastMessage( this,"Registered" ) );
	    }

	    public void onUnregistered() {
	        Log.d( LOG_TAG, "onUnregistered" );
	        registered = false; 
	        uiHandler.post( new ToastMessage( this, "Unregistered" ) );
	    }

	    private Account getAccountFromAccountName( String accountName ) {
	        AccountManager accountManager = AccountManager.get( this );
	        Account accounts[] = accountManager.getAccounts();
	        for( int i = 0 ; i < accounts.length ; ++i )
	            if( accountName.equals( accounts[i].name ) )
	                return accounts[i];
	        return null;
	    }

	    private void register() {
	        if( registered )
	            unregister();
	        else {
	            Log.d( LOG_TAG, "register()" );
	            C2DMessaging.register( this, Config.C2DM_SENDER );
	            Log.d( LOG_TAG, "register() done" );
	        }
	    }

	    private void unregister() {
	        if( registered ) {
	            Log.d( LOG_TAG, "unregister()" );
	            C2DMessaging.unregister( this );
	            Log.d( LOG_TAG, "unregister() done" );
	        }
	    }

	private final ICouchbaseDelegate mCallback = new ICouchbaseDelegate() {
		@Override
		public void couchbaseStarted(String host, int port) {

			String url = "http://" + host + ":" + Integer.toString(port) + "/";
		    String ip = getLocalIpAddress();
		    String param = (ip == null) ? "" : "?ip=" + ip;
		    
		    Log.v(TAG, "host: " + host + " ip: " + ip);
		    
		    try {
				couch.installDatabase("mobilefuton.couch");
			} catch (IOException e) {
				e.printStackTrace();
			}
		    
		    // Copy the couch db from /usr/local/var/lib/couchdb on dev instance.
		    try {
		    	String sourceDb = "coconut.couch.jpg";
		    	String destDb = "coconut.couch";
		    	File source = new File(CouchbaseMobile.externalPath() + "/db/" + sourceDb);
		    	File destination = new File(CouchbaseMobile.externalPath() + "/db/" + destDb);
		    	if (!destination.exists()) {
                    Log.d(TAG, "Installing the database at " + destination);
			    	couch.installDatabase(sourceDb);
			    	source.renameTo(destination);
		    	}
//		    	boolean autoSyncEnabled = !C2DMessaging.getRegistrationId(getBaseContext()).equals("");
//
//		    	if (!autoSyncEnabled) {
//		    		Log.i(TAG, "Registering with C2DMessaging");
//		    		C2DMessaging.register(getBaseContext(), Config.C2DM_SENDER);
//		    		String registrationId = C2DMessaging.getRegistrationId(getBaseContext());
//		    		Log.d(TAG, "registrationId: " + registrationId);
//		    	}
		    } catch (IOException e) {
		    	e.printStackTrace();
		    }
			String couchAppUrl = url + "coconut/_design/coconut/index.html";
			launchCouchApp(couchAppUrl);
		}

		@Override
		public void exit(String error) {
			Log.v(TAG, error);
			couchError();
		}
	};

	private void startCouch() {
		//CouchbaseMobile couch = new CouchbaseMobile(getBaseContext(), mCallback);
		couch = new CouchbaseMobile(getBaseContext(), mCallback);

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
		webView = new WebView(CoconutActivity.this);
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
 
    class ToastMessage implements Runnable {
        public ToastMessage( Context ctx, String msg ) {
            this.ctx = ctx;
            this.msg = msg;
        }

        public void run() {
            Toast.makeText( ctx, msg, Toast.LENGTH_SHORT).show();
        }

        Context ctx;
        String msg;
    }

    class AddMessageTask implements Runnable {
        AddMessageTask( String message ) {
            this.message = message;
        }

        public void run() {
            HashMap<String,String> entry = new HashMap<String,String>();
            Calendar c = new GregorianCalendar();
            StringBuffer b = new StringBuffer();
            Formatter f = new Formatter( b );
            f.format( "%04d/%02d/%02d %02d:%02d:%02d",
                        c.get( Calendar.YEAR ),
                        c.get( Calendar.MONTH ),
                        c.get( Calendar.DAY_OF_MONTH ),
                        c.get( Calendar.HOUR_OF_DAY ),
                        c.get( Calendar.MINUTE ),
                        c.get( Calendar.SECOND ) );
            entry.put( "time",new String( b ) );
            entry.put( "message", message );
            list.add( entry );
            messages.notifyDataSetChanged();
        }

        String message;
    }
}