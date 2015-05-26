/**
 * Created by Pasin Suriyentrakorn <pasin@couchbase.com> on 2/26/14.
 */

package com.couchbase.todolite;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import com.couchbase.lite.Attachment;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.SavedRevision;
import com.couchbase.lite.UnsavedRevision;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.util.Log;
import com.couchbase.todolite.preferences.ToDoLitePreferences;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;

import org.apache.http.client.HttpResponseException;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Observable;

public class Application extends android.app.Application {

    public static final String TAG = "ToDoLite";
    private static final String DATABASE_NAME = "todos";
    private static final String GUEST_DATABASE_NAME = "guest";
    private static final String SYNC_URL_HTTP = BuildConfig.SYNC_URL_HTTP;
    private static final String SYNC_URL_HTTPS = BuildConfig.SYNC_URL_HTTPS;
    private static final String SYNC_URL = SYNC_URL_HTTP;

    private Manager manager;
    private Database database;
    private Synchronize sync;

    private int syncCompletedChangedCount;
    private int syncTotalChangedCount;
    private OnSyncProgressChangeObservable onSyncProgressChangeObservable;
    private OnSyncUnauthorizedObservable onSyncUnauthorizedObservable;

    public enum AuthenticationType { FACEBOOK, ALL }

    // By default, this should be set to FACEBOOK.  To test "custom cookie" auth,
    // or basic auth change it to ALL. And run the app against your local sync gateway
    // you have control over to create custom cookies and users via the admin port.
    public static AuthenticationType authenticationType = AuthenticationType.FACEBOOK;

    private ToDoLitePreferences preferences;

    private void initDatabase() {
        try {

            Manager.enableLogging(TAG, Log.VERBOSE);
            Manager.enableLogging(Log.TAG, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_SYNC_ASYNC_TASK, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_SYNC, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_QUERY, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_VIEW, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_DATABASE, Log.VERBOSE);

            manager = new Manager(new AndroidContext(getApplicationContext()), Manager.DEFAULT_OPTIONS);
        } catch (IOException e) {
            Log.e(TAG, "Cannot create Manager object", e);
            return;
        }

        try {
            database = manager.getDatabase(DATABASE_NAME);
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "Cannot get Database", e);
            return;
        }
    }

    private void initObservable() {
        onSyncProgressChangeObservable = new OnSyncProgressChangeObservable();
        onSyncUnauthorizedObservable = new OnSyncUnauthorizedObservable();
    }

    private synchronized void updateSyncProgress(int completedCount, int totalCount, Replication.ReplicationStatus status) {
        onSyncProgressChangeObservable.notifyChanges(completedCount, totalCount, status);
    }

    public void startReplicationSyncWithCustomCookie(String cookieValue) {

        sync = new Synchronize.Builder(getDatabase(), SYNC_URL, true)
                .cookieAuth(cookieValue)
                .addChangeListener(getReplicationChangeListener())
                .build();
        sync.start();

    }

    public void startReplicationSyncWithStoredCustomCookie() {

        sync = new Synchronize.Builder(getDatabase(), SYNC_URL, true)
                .addChangeListener(getReplicationChangeListener())
                .build();
        sync.start();

    }

    public void startReplicationSyncWithStoredBasicAuth() {

        sync = new Synchronize.Builder(getDatabase(), SYNC_URL, true)
                .basicAuth(preferences.getCurrentUserId(), preferences.getCurrentUserPassword())
                .addChangeListener(getReplicationChangeListener())
                .build();
        sync.start();

    }


    public void startReplicationSyncWithBasicAuth(String username, String password) {

        sync = new Synchronize.Builder(getDatabase(), SYNC_URL, true)
                .basicAuth(username, password)
                .addChangeListener(getReplicationChangeListener())
                .build();
        sync.start();

    }

    public void startReplicationSyncWithFacebookLogin(String accessToken) {

        if (sync != null) {
            sync.destroyReplications();
        }

        sync = new Synchronize.Builder(getDatabase(), SYNC_URL, true)
                .facebookAuth(accessToken)
                .addChangeListener(getReplicationChangeListener())
                .build();
        sync.start();

    }

    public void startContinuousPushAndOneShotPull(String accessToken) {

        if (sync != null) {
            sync.destroyReplications();
        }

        sync = new Synchronize.Builder(getDatabase(), SYNC_URL, false)
                .facebookAuth(accessToken)
                .addChangeListener(getReplicationChangeListener())
                .build();


       sync.start();

    }

    public void stopSync() {
        sync.destroyReplications();
        sync = null;
    }

    public void migrateGuestDataToUser(Document profile) {
        Database guestDb = getDatabaseForGuest();
        if (guestDb.getLastSequenceNumber() > 0) {

            QueryEnumerator rows = null;
            try {
                rows = guestDb.createAllDocumentsQuery().run();
            } catch (CouchbaseLiteException e) {
                Log.e(TAG, "Cannot run all docs query", e);
            }

            if (rows.getCount() == 0) {
                return;
            }

            Database userDB = database;
            for (QueryRow row : rows) {
                Document doc = row.getDocument();

                Document newDoc = userDB.getDocument(doc.getId());
                try {
                    newDoc.putProperties(doc.getUserProperties());
                } catch (CouchbaseLiteException e) {
                    Log.e(TAG, "Cannot save userProperties in newDoc", e);
                }

                List<Attachment> attachments = doc.getCurrentRevision().getAttachments();
                if (attachments.size() > 0) {
                    UnsavedRevision rev = newDoc.getCurrentRevision().createRevision();
                    for (Attachment attachment : attachments) {
                        try {
                            rev.setAttachment(attachment.getName(), attachment.getContentType(), attachment.getContent());
                        } catch (CouchbaseLiteException e) {
                            Log.e(TAG, "Cannot set attachment on new revision", e);
                        }
                    }

                    try {
                        SavedRevision saved = rev.save();
                    } catch (CouchbaseLiteException e) {
                        Log.e(TAG, "Cannot save new revision", e);
                    }
                }
            }

            try {
                guestDb.delete();
            } catch (CouchbaseLiteException e) {
                Log.e(TAG, "Cannot delete guest database");
            }

        }
    }

    public void logoutUser() {
        callFacebookLogout(getApplicationContext());
        sync.destroyReplications();
        sync = null;
        preferences.setCurrentUserId(null);
        preferences.setCurrentUserPassword(null);
        preferences.setLastReceivedFbAccessToken(null);
        preferences.setCurrentListId(null);
    }

    /**
     * Logout from Facebook to make sure the session and cache are cleared
     * See http://stackoverflow.com/a/18584885/1908348
     */
    public static void callFacebookLogout(Context context) {
        LoginManager.getInstance().logOut();
    }

    public void pushLocalNotification(String title, String notificationText){

        NotificationManager notificationManager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        //use the flag FLAG_UPDATE_CURRENT to override any notification already there
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new Notification(R.drawable.ic_launcher, notificationText, System.currentTimeMillis());
        notification.flags = Notification.FLAG_AUTO_CANCEL | Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND;

        notification.setLatestEventInfo(this, title, notificationText, contentIntent);

        //10 is a random number I chose to act as the id for this notification
        notificationManager.notify(10, notification);

    }

    private Replication.ChangeListener getReplicationChangeListener() {
        return new Replication.ChangeListener() {

            @Override
            public void changed(Replication.ChangeEvent event) {
                Replication replication = event.getSource();
                if (event.getError() != null) {
                    Throwable lastError = event.getError();
                    if (lastError.getMessage().contains("existing change tracker")) {
                        pushLocalNotification("Replication Event", String.format("Sync error: %s:", lastError.getMessage()));
                    }
                    if (lastError instanceof HttpResponseException) {
                        HttpResponseException responseException = (HttpResponseException) lastError;
                        if (responseException.getStatusCode() == 401) {
                            onSyncUnauthorizedObservable.notifyChanges();
                        }
                    }
                }
                Log.d(TAG, event.toString());
                updateSyncProgress(
                        replication.getCompletedChangesCount(),
                        replication.getChangesCount(),
                        replication.getStatus()
                );
            }
        };
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.preferences = new ToDoLitePreferences(getApplicationContext());

        Log.d(Application.TAG, "Application State: onCreate()");

        /*
        We need to initialized the Facebook SDK before we can use it
        (https://developers.facebook.com/docs/android/getting-started)
         */
        FacebookSdk.sdkInitialize(getApplicationContext());

        migrateOldVersion();

        initDatabase();
        initObservable();

    }

    private void migrateOldVersion() {
        int v = 0;
        try {
            v = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        /*
        The versionCode is defined in the build.gradle file of the ToDoLite module.
        Version 104 adds the guest account and one db per user, instead of doing a
        migration, we ask the user to log in and all docs will be pulled in the new
        db from sync gateway. A better user experience would be to perform
        the migration here.
         */
        if (preferences.getVersionCode() == 0) {
            callFacebookLogout(getApplicationContext());
            preferences.setCurrentUserId(null);
            preferences.setCurrentUserPassword(null);
            preferences.setLastReceivedFbAccessToken(null);
            preferences.setCurrentListId(null);

            preferences.setVersionCode(v);
        }
    }

    public Database getDatabase() {
        return this.database;
    }

    public Database setDatabaseForName(String name) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            byte[] inputBytes = name.getBytes();
            byte[] hashBytes = digest.digest(inputBytes);
            database = manager.getDatabase("db" + byteArrayToHex(hashBytes));
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "Cannot get Database", e);
        }
        return database;
    }

    public void setDatabaseForGuest() {
        try {
            database = manager.getDatabase(GUEST_DATABASE_NAME);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }

    public Database getDatabaseForGuest() {
        Database db = null;
        try {
            db = manager.getDatabase(GUEST_DATABASE_NAME);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        return db;
    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    public OnSyncProgressChangeObservable getOnSyncProgressChangeObservable() {
        return onSyncProgressChangeObservable;
    }

    public OnSyncUnauthorizedObservable getOnSyncUnauthorizedObservable() {
        return onSyncUnauthorizedObservable;
    }

    static class OnSyncProgressChangeObservable extends Observable {
        private void notifyChanges(int completedCount, int totalCount, Replication.ReplicationStatus status) {
            SyncProgress progress = new SyncProgress();
            progress.completedCount = completedCount;
            progress.totalCount = totalCount;
            progress.status = status;
            setChanged();
            notifyObservers(progress);
        }
    }

    static class OnSyncUnauthorizedObservable extends Observable {
        private void notifyChanges() {
            setChanged();
            notifyObservers();
        }
    }

    static class SyncProgress {
        public int completedCount;
        public int totalCount;
        public Replication.ReplicationStatus status;
    }

}
