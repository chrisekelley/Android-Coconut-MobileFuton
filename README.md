## Welcome!

This is a demonstration of using Dale Harvey's [Mobile Futon](https://github.com/daleharvey/Android-MobileFuton) as a platform for other apps. 

## Packaging the app

My app is targeting Android 2.1-update 1 (API level 7) which is used on the Samsung Galaxy Tab and the Nexus One. 
This version has a limit of 1MB for files in the assets directory. 
The workaround is to rename any files over 1 MB with an extension that is one of the formats that is not uncompressed by default Android. 
See [Dealing with Asset Compression in Android Apps](http://ponystyle.com/blog/2010/03/26/dealing-with-asset-compression-in-android-apps/) for more info.
Note - this is no longer an issue w/ Android 2.3.

To export the database, there are a couple of options: copy the couchdb or use couchapp push. I'm currently copying the couchdb - 
this seems to work better with recent versions of MobileCouchbase:

* Method 1: Copy the couch (preferred method):
```
cd /usr/local/var/lib/couchdb
```
```
cp coconut.couch ~/source/Android-Coconut-MobileFuton/assets/coconut.couch.jpg
```
* Method 2: Use couchapp push (no longer recommended):
```
    couchapp push --export > ../Android-Coconut-MobileFuton/assets/coconut.json.jpg
```
## Configuration

After cloning the project, download the most recent version of [Mobile Couchbase](http://www.couchbase.com/wiki/display/couchbase/Android) and install in the project.

If you would like to use synchronization with a master CouchDB or C2DM notification, 
un-comment and complete the values in res/war/coconut.properties.

### Logging

Change the log level in mobilefuton.ini. It is currently set to error. Setting log level to debug creates huge log files due to continuous replication.

## Testing the app and replication

You may download the app from [ictedge.org](http://ictedge.org/files/coconut/demo/Android-Coconut-MobileFuton.apk).

Continuous replication is already enabled in coconut.properties - it points to a test couch on iriscouch.com. 
Visit [vetula.iriscouch.com/coconut](http://vetula.iriscouch.com/coconut/_design/coconut/index.html#home) to view/edit records. 

Create a record by clicking the Incident button. 
In a few moments, your new record should be replicated to the app on your local instance. Click the refresh button to see the new data.
The app works best in landscape orientation if you are using a smartphone - the refresh button may be cut off in vertical orientation.
    
## Installing the app

Here is the code from [CoconutActivity](https://github.com/vetula/Android-Coconut-MobileFuton/blob/master/src/org/rti/rcd/ict/lgug/CoconutActivity.java) 
that installs the couchapps and launches my app instead of Mobile Futon:

    String sourceDb = "coconut.couch.jpg";
	String destDb = "coconut.couch";
	File source = new File(CouchbaseMobile.externalPath() + "/db/" + sourceDb);
	File destination = new File(CouchbaseMobile.externalPath() + "/db/" + destDb);
	if (!destination.exists()) {
        Log.d(TAG, "Installing the database at " + destination);
    	couch.installDatabase(sourceDb);
    	source.renameTo(destination);
	}
	String couchAppUrl = url + "coconut/_design/coconut/index.html";
	launchCouchApp(couchAppUrl);
    
I made some minor changes to Dale Harvey's code to load non-designDocs.

## Notifications

Push notifications are enabled. Register with the Google [C2DM](http://code.google.com/android/c2dm/) service.

Major kudos to [My Life with Android blogs' Push service from Google](http://mylifewithandroid.blogspot.com/2010/10/push-service-from-google.html) and the Google [Jumpnote](http://code.google.com/p/jumpnote/) example.
