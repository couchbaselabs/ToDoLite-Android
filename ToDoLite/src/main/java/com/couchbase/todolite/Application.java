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
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.AuthenticatorFactory;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.util.Log;
import com.couchbase.todolite.document.Profile;
import com.couchbase.todolite.document.Task;
import com.facebook.Session;

import org.apache.http.auth.AUTH;
import org.apache.http.client.HttpResponseException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.StringTokenizer;

public class Application extends android.app.Application {

    public static final String TAG = "ToDoLite";
    private static final String DATABASE_NAME = "todos";
    private static final String GUEST_DATABASE_NAME = "guest";
    private static final String SYNC_URL_HTTP = BuildConfig.SYNC_URL_HTTP;
    private static final String SYNC_URL_HTTPS = BuildConfig.SYNC_URL_HTTPS;
    private static final String SYNC_URL = SYNC_URL_HTTP;

    private static final String PREF_GUEST_BOOLEAN = "GuestBoolean";
    private static final String PREF_CURRENT_LIST_ID = "CurrentListId";
    private static final String PREF_CURRENT_USER_ID = "CurrentUserId";
    private static final String PREF_CURRENT_USER_PASSWORD = "CurrentUserPassword";

    private static final String PREF_LAST_RCVD_FB_ACCESS_TOKEN = "LastReceivedFbAccessToken";
    private static final String PREF_VERSION_CODE = "VersionCode";

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

        sync = new Synchronize.Builder(getDatabase(), SYNC_URL)
                .cookieAuth(cookieValue)
                .addChangeListener(getReplicationChangeListener())
                .build();
        sync.start();

    }

    public void startReplicationSyncWithStoredCustomCookie() {

        sync = new Synchronize.Builder(getDatabase(), SYNC_URL)
                .addChangeListener(getReplicationChangeListener())
                .build();
        sync.start();

    }

    public void startReplicationSyncWithStoredBasicAuth() {

        sync = new Synchronize.Builder(getDatabase(), SYNC_URL)
                .basicAuth(getCurrentUserId(), getCurrentUserPassword())
                .addChangeListener(getReplicationChangeListener())
                .build();
        sync.start();

    }


    public void startReplicationSyncWithBasicAuth(String username, String password) {

        sync = new Synchronize.Builder(getDatabase(), SYNC_URL)
                .basicAuth(username, password)
                .addChangeListener(getReplicationChangeListener())
                .build();
        sync.start();

    }

    public void startReplicationSyncWithFacebookLogin(String accessToken) {

        sync = new Synchronize.Builder(getDatabase(), SYNC_URL)
                .facebookAuth(accessToken)
                .addChangeListener(getReplicationChangeListener())
                .build();
        sync.start();

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
                    UnsavedRevision rev = null;
                    try {
                        rev = newDoc.getCurrentRevision().createRevision();
                    } catch (CouchbaseLiteException e) {
                        Log.e(TAG, "Cannot create new revision", e);
                    }

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
        setCurrentUserId(null);
        setCurrentUserPassword(null);
        setLastReceivedFbAccessToken(null);
        setCurrentListId(null);
    }

    /**
     * Logout from Facebook to make sure the session and cache are cleared
     * See http://stackoverflow.com/a/18584885/1908348
     */
    public static void callFacebookLogout(Context context) {
        Session session = Session.getActiveSession();
        if (session != null) {

            if (!session.isClosed()) {
                session.closeAndClearTokenInformation();
                //clear your preferences if saved
            }
        } else {

            session = new Session(context);
            Session.setActiveSession(session);

            session.closeAndClearTokenInformation();
            //clear your preferences if saved

        }

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

        Log.d(Application.TAG, "Application State: onCreate()");

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
        if (getVersionCode() == 0) {
            callFacebookLogout(getApplicationContext());
            setCurrentUserId(null);
            setCurrentUserPassword(null);
            setLastReceivedFbAccessToken(null);
            setCurrentListId(null);

            setVersionCode(v);
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

    public Boolean getGuestBoolean() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        return sp.getBoolean(PREF_GUEST_BOOLEAN, false);
    }

    public void setGuestBoolean(Boolean bool) {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        sp.edit().putBoolean(PREF_GUEST_BOOLEAN, bool).apply();
    }

    public String getCurrentListId() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        return sp.getString(PREF_CURRENT_LIST_ID, null);
    }

    public void setCurrentListId(String id) {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        if (id != null) {
            sp.edit().putString(PREF_CURRENT_LIST_ID, id).apply();
        } else {
            sp.edit().remove(PREF_CURRENT_LIST_ID).apply();
        }
    }

    public String getCurrentUserId() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        String userId = sp.getString(PREF_CURRENT_USER_ID, null);
        return userId;
    }

    public void setCurrentUserId(String id) {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        if (id != null) {
            sp.edit().putString(PREF_CURRENT_USER_ID, id).apply();
        } else {
            sp.edit().remove(PREF_CURRENT_USER_ID).apply();
        }
    }

    public void setCurrentUserPassword(String userNamePass) {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        if (userNamePass != null) {
            sp.edit().putString(PREF_CURRENT_USER_PASSWORD, userNamePass).apply();
        } else {
            sp.edit().remove(PREF_CURRENT_USER_PASSWORD).apply();
        }
    }

    public String getCurrentUserPassword() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        String userId = sp.getString(PREF_CURRENT_USER_PASSWORD, null);
        return userId;
    }

    public void setLastReceivedFbAccessToken(String fbAccessToken) {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        if (fbAccessToken != null) {
            sp.edit().putString(PREF_LAST_RCVD_FB_ACCESS_TOKEN, fbAccessToken).apply();
        } else {
            sp.edit().remove(PREF_LAST_RCVD_FB_ACCESS_TOKEN).apply();
        }
    }

    public String getLastReceivedFbAccessToken() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        return sp.getString(PREF_LAST_RCVD_FB_ACCESS_TOKEN, null);
    }

    public void setVersionCode(int versionCode) {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        if (versionCode != 0) {
            sp.edit().putInt(PREF_VERSION_CODE, versionCode).apply();
        } else {
            sp.edit().remove(PREF_VERSION_CODE).apply();
        }
    }

    public int getVersionCode() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        return sp.getInt(PREF_VERSION_CODE, 0);
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
