## Welcome!

This is a demonstration of using Dale Harvey's [Mobile Futon](https://github.com/daleharvey/Android-MobileFuton) as a platform for other apps. 

## Packaging the app

My app is targeting Android 2.1-update 1 (API level 7) which is used on the Samsung Galaxy Tab and the Nexus One. 
This version has a limit of 1MB for files in the assets directory. 
The workaround is to rename any files over 1 MB with an extention that is one of the formats that is not uncompressed by default Android. 
See [Dealing with Asset Compression in Android Apps](http://ponystyle.com/blog/2010/03/26/dealing-with-asset-compression-in-android-apps/) for more info.
Note - this is no longer an issue w/ Android 2.3.

To export the database, there are a couple of options: copy the couchdb or use couchapp push. I'm currently copying the couchdb - 
this seems to work better with recent versions of MobileCouchbase:

* Copy the couch:
```
cd /usr/local/var/lib/couchdb
cp coconut.couch ~/source/Android-Coconut-MobileFuton/assets/coconut.couch.jpg
```
* Use couchapp push:
```
    couchapp push --export > ../Android-Coconut-MobileFuton/assets/coconut.json.jpg
```
## Configuration

If you would like to use synchronization with a master CouchDB or C2DM notification, 
un-comment and complete the values in res/war/coconut.properties.
    
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
