/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.jumpnote.android;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;

import org.rti.rcd.ict.lgug.c2dm.Config;
import org.rti.rcd.ict.lgug.c2dm.SyncAdapter;

/**
 * Provides access to a database of notes. Each note has a title, the note
 * itself, a creation date and a modified data.
 */
public class JumpNoteProvider extends ContentProvider {
    static final String TAG = Config.makeLogTag(JumpNoteProvider.class);

    private static final String DATABASE_NAME = "jumpnote.db";
    private static final int DATABASE_VERSION = 5;
    private static final String NOTES_TABLE_NAME = "notes";

    private static HashMap<String, String> sNotesProjectionMap;
    private static final int NOTES = 1;
    private static final int NOTE_ID = 2;

    private static final UriMatcher sUriMatcher;

    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {
        private Context mContext;
        
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + NOTES_TABLE_NAME + " ("
                    + JumpNoteContract.Notes._ID + " INTEGER PRIMARY KEY,"
                    + JumpNoteContract.Notes.SERVER_ID + " TEXT,"
                    + JumpNoteContract.Notes.ACCOUNT_NAME + " TEXT,"
                    + JumpNoteContract.Notes.TITLE + " TEXT,"
                    + JumpNoteContract.Notes.BODY + " TEXT NOT NULL DEFAULT '',"
                    + JumpNoteContract.Notes.CREATED_DATE + " INTEGER NOT NULL DEFAULT 0,"
                    + JumpNoteContract.Notes.PENDING_DELETE + " BOOLEAN NOT NULL DEFAULT 0,"
                    + JumpNoteContract.Notes.MODIFIED_DATE + " INTEGER NOT NULL DEFAULT 0"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            SyncAdapter.clearSyncData(mContext);
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
                    + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + NOTES_TABLE_NAME);
            onCreate(db);
        }
    }

    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(NOTES_TABLE_NAME);

        switch (sUriMatcher.match(uri)) {
            case NOTES:
                qb.setProjectionMap(sNotesProjectionMap);
                qb.appendWhere(JumpNoteContract.Notes.ACCOUNT_NAME + "=");
                qb.appendWhereEscapeString(uri.getPathSegments().get(1));
                break;

            case NOTE_ID:
                qb.setProjectionMap(sNotesProjectionMap);
                qb.appendWhere(JumpNoteContract.Notes.ACCOUNT_NAME + "=");
                qb.appendWhereEscapeString(uri.getPathSegments().get(1));
                qb.appendWhere(" AND ");
                qb.appendWhere(JumpNoteContract.Notes._ID + "=" + uri.getPathSegments().get(2));
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = JumpNoteContract.Notes.DEFAULT_SORT_ORDER;
        } else {
            orderBy = sortOrder;
        }

        // Get the database and run the query
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data
        // changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case NOTES:
                return JumpNoteContract.Notes.CONTENT_TYPE;

            case NOTE_ID:
                return JumpNoteContract.Notes.CONTENT_ITEM_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        // Validate the requested uri
        if (sUriMatcher.match(uri) != NOTES) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        String accountName = uri.getPathSegments().get(1);
        values.put(JumpNoteContract.Notes.ACCOUNT_NAME, accountName);

        long now = System.currentTimeMillis();

        // Make sure that the fields are all set
        if (values.getAsLong(JumpNoteContract.Notes.CREATED_DATE) == null) {
            values.put(JumpNoteContract.Notes.CREATED_DATE, now);
        }

        if (values.getAsLong(JumpNoteContract.Notes.MODIFIED_DATE) == null) {
            values.put(JumpNoteContract.Notes.MODIFIED_DATE, now);
        }

        if (!values.containsKey(JumpNoteContract.Notes.TITLE)) {
            Resources r = Resources.getSystem();
            values.put(JumpNoteContract.Notes.TITLE, r.getString(android.R.string.untitled));
        }

        if (values.containsKey(JumpNoteContract.Notes.PENDING_DELETE) &&
                values.getAsBoolean(JumpNoteContract.Notes.PENDING_DELETE) == true) {
            throw new SQLException("Cannot insert a note that is pending delete.");
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(NOTES_TABLE_NAME, JumpNoteContract.Notes.BODY, values);
        if (rowId > 0) {
            Uri noteUri = JumpNoteContract.buildNoteUri(accountName, rowId);
            boolean syncToNetwork = !hasCallerIsSyncAdapterParameter(uri);
            getContext().getContentResolver().notifyChange(noteUri, null, syncToNetwork);
            return noteUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case NOTES:
                count = db.delete(NOTES_TABLE_NAME, where, whereArgs);
                break;

            case NOTE_ID:
                String noteId = uri.getPathSegments().get(2);
                count = db.delete(NOTES_TABLE_NAME, JumpNoteContract.Notes._ID + "=" + noteId
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        boolean syncToNetwork = !hasCallerIsSyncAdapterParameter(uri);
        getContext().getContentResolver().notifyChange(uri, null, syncToNetwork);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case NOTES:
                count = db.update(NOTES_TABLE_NAME, values, where, whereArgs);
                break;

            case NOTE_ID:
                String noteId = uri.getPathSegments().get(2);
                count = db.update(NOTES_TABLE_NAME, values, JumpNoteContract.Notes._ID + "="
                        + noteId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
                        whereArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        boolean syncToNetwork = !hasCallerIsSyncAdapterParameter(uri);
        getContext().getContentResolver().notifyChange(uri, null, syncToNetwork);
        return count;
    }

    private static boolean hasCallerIsSyncAdapterParameter(Uri uri) {
        return Boolean.parseBoolean(uri.getQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER));
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        // Match .../notes/foo@gmail.com
        sUriMatcher.addURI(JumpNoteContract.AUTHORITY, "notes/*", NOTES);

        // Match .../notes/foo@gmail.com/123 (note ID)
        sUriMatcher.addURI(JumpNoteContract.AUTHORITY, "notes/*/#", NOTE_ID);

        sNotesProjectionMap = new HashMap<String, String>();
        sNotesProjectionMap.put(JumpNoteContract.Notes._ID,
                JumpNoteContract.Notes._ID);
        sNotesProjectionMap.put(JumpNoteContract.Notes.SERVER_ID,
                JumpNoteContract.Notes.SERVER_ID);
        sNotesProjectionMap.put(JumpNoteContract.Notes.ACCOUNT_NAME,
                JumpNoteContract.Notes.ACCOUNT_NAME);
        sNotesProjectionMap.put(JumpNoteContract.Notes.TITLE,
                JumpNoteContract.Notes.TITLE);
        sNotesProjectionMap.put(JumpNoteContract.Notes.BODY,
                JumpNoteContract.Notes.BODY);
        sNotesProjectionMap.put(JumpNoteContract.Notes.CREATED_DATE,
                JumpNoteContract.Notes.CREATED_DATE);
        sNotesProjectionMap.put(JumpNoteContract.Notes.MODIFIED_DATE,
                JumpNoteContract.Notes.MODIFIED_DATE);
        sNotesProjectionMap.put(JumpNoteContract.Notes.PENDING_DELETE,
                JumpNoteContract.Notes.PENDING_DELETE);
    }
}
