package org.rti.rcd.ict.lgug;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ListActivity;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SimpleAdapter;
import android.widget.Toast; 

import com.google.android.c2dm.C2DMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.Formatter;

/**
 * Code from example at http://mylifewithandroid.blogspot.com/2010/10/push-service-from-google.html
 *
 */
public class Push extends ListActivity
{
    private static final int ACTIVITY_ACCOUNTS = 1;
    private static final int MENU_ACCOUNTS = 2;    
    private static final String LOG_TAG = "Push";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        messages = new SimpleAdapter(    
                                this,
                                list,
                                R.layout.main_item_two_line_row,
                                new String[] { "time","message" },
                                new int[] { R.id.time, R.id.message }  );
        setListAdapter( messages );
        uiHandler = new Handler();
        pushRef = this;
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
                        "aexp.push",
                        "aexp.push.AccountSelector" );
                    startActivityForResult(i, ACTIVITY_ACCOUNTS );
                    return true;
                }
        }
        return false;
    }

    public static Push getRef() {
        return pushRef;
    }

    public void displayMessage( String message ) {
        uiHandler.post( new AddMessageTask( message ) );
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

    

    private ArrayList<HashMap<String,String>> list = new ArrayList<HashMap<String,String>>();
    private SimpleAdapter messages;
    private Handler uiHandler;
    private Account selectedAccount;
    private boolean registered;
    private static Push pushRef;

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


