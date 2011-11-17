/*
 */
package com.google.android.c2dm;

import org.rti.rcd.ict.lgug.C2DMReceiver;
import org.rti.rcd.ict.lgug.Config;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Helper class to handle BroadcastReciver behavior.
 * - can only run for a limited amount of time - it must start a real service 
 * for longer activity
 * - must get the power lock, must make sure it's released when all done.
 * 
 */
public class C2DMBroadcastReceiver extends BroadcastReceiver {
	static final String TAG = Config.makeLogTag(C2DMReceiver.class);
	
    @Override
    public final void onReceive(Context context, Intent intent) {
        // To keep things in one place.
        C2DMBaseReceiver.runIntentInService(context, intent);
        setResult(Activity.RESULT_OK, null /* data */, null /* extra */); 
        String message = intent.getExtras().getString(Config.C2DM_MESSAGE_EXTRA);
        Log.d(TAG, "Message from BroadcastReceiver: " + message);
    }
}
