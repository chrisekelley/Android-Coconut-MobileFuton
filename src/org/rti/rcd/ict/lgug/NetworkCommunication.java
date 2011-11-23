package org.rti.rcd.ict.lgug;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OperationCanceledException;
import android.accounts.AuthenticatorException;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.util.Log;
import org.apache.http.cookie.Cookie;
import org.apache.http.HttpEntity;    
import org.apache.http.HttpResponse;  
import org.apache.http.HttpStatus;    
import org.apache.http.Header;
import org.apache.http.NameValuePair; 
import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.entity.UrlEncodedFormEntity;  
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Code from example at http://mylifewithandroid.blogspot.com/2010/10/push-service-from-google.html
 */
public class NetworkCommunication {
    private static final int REGISTRATION_TIMEOUT = 30 * 1000; // ms
    private static final String TOKEN_URI =
    		CoconutActivity.PUSH_SERVER_URL + "/token";
    private static final String LOG_TAG = "Push_NetworkComm";

    private static DefaultHttpClient httpClient = null;

    public static boolean sendRegistrationId( Account account, Context context, String registrationId ) {
        String accountName = account.name;
        return sendToken( accountName, registrationId );
    }

    private static boolean sendToken( String accountName, String registrationId ) {
        try {
            maybeCreateHttpClient();
            HttpPost post = new HttpPost( TOKEN_URI );
            ArrayList<BasicNameValuePair> parms = new ArrayList<BasicNameValuePair>();
            parms.add( new BasicNameValuePair( "accountName", accountName ) );
            parms.add( new BasicNameValuePair( "registrationId", registrationId ) );
            post.setEntity( new UrlEncodedFormEntity( parms ) );
            HttpResponse resp = httpClient.execute( post );
// Execute the POST transaction and read the results
            return resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch( UnsupportedEncodingException ex ) {
            Log.e( LOG_TAG, "UnsupportedEncodingException", ex );
            return false;
        } catch( IOException ex ) {
            Log.e( LOG_TAG, "IOException", ex );
            return false;
        }
    }

    private static void maybeCreateHttpClient() {
        if ( httpClient == null) {
            httpClient = new DefaultHttpClient();
            HttpParams params = httpClient.getParams();
            HttpConnectionParams.setConnectionTimeout(params, REGISTRATION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(params, REGISTRATION_TIMEOUT);
            ConnManagerParams.setTimeout(params, REGISTRATION_TIMEOUT);
        }
    }

}

