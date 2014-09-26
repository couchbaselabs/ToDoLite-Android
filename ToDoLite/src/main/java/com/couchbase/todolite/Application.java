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
import android.preference.PreferenceManager;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.AuthenticatorFactory;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.util.Log;

import org.apache.http.client.HttpResponseException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Observable;

public class Application extends android.app.Application {

    public static final String TAG = "ToDoLite";
    private static final String DATABASE_NAME = "todos";
    private static final String SYNC_URL = "http://demo.mobile.couchbase.com/todolite/";

    private static final String PREF_CURRENT_LIST_ID = "CurrentListId";
    private static final String PREF_CURRENT_USER_ID = "CurrentUserId";
    private static final String PREF_LAST_RCVD_FB_ACCESS_TOKEN = "LastReceivedFbAccessToken";

    private Manager manager;
    private Database database;

    private int syncCompletedChangedCount;
    private int syncTotalChangedCount;
    private OnSyncProgressChangeObservable onSyncProgressChangeObservable;
    private OnSyncUnauthorizedObservable onSyncUnauthorizedObservable;

    private Replication pullReplication;
    private Replication pushReplication;

    public enum AuthenticationType { FACEBOOK, CUSTOM_COOKIE }

    // By default, this should be set to FACEBOOK.  To test "custom cookie" auth,
    // set this to CUSTOM_COOKIE.
    private AuthenticationType authenticationType = AuthenticationType.FACEBOOK;

    private void initDatabase() {
        try {
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

    private synchronized void updateSyncProgress(int completedCount, int totalCount) {
        onSyncProgressChangeObservable.notifyChanges(completedCount, totalCount);
    }

    public void startReplicationSyncWithCustomCookie(String name, String value, String path, Date expirationDate, boolean secure, boolean httpOnly) {

        if (pullReplication == null && pushReplication == null) {
            Replication[] replications = createReplications();
            pullReplication = replications[0];
            pushReplication = replications[1];

            pullReplication.setCookie(name, value, path, expirationDate, secure, httpOnly);
            pushReplication.setCookie(name, value, path, expirationDate, secure, httpOnly);

            pullReplication.start();
            pushReplication.start();

            Log.v(TAG, "startReplicationSyncWithCustomCookie(): Start Replication Sync ...");
        } else {
            Log.v(TAG, "startReplicationSyncWithCustomCookie(): doing nothing, already have existing replications");

        }

    }

    public void startReplicationSyncWithStoredCustomCookie() {

        if (pullReplication == null && pushReplication == null) {

            Replication[] replications = createReplications();
            pullReplication = replications[0];
            pushReplication = replications[1];

            pullReplication.start();
            pushReplication.start();

            Log.v(TAG, "startReplicationSyncWithStoredCustomCookie(): Start Replication Sync ...");

        } else {
            Log.v(TAG, "startReplicationSyncWithStoredCustomCookie(): doing nothing, already have existing replications");

        }

    }

    public void startReplicationSyncWithFacebookLogin(String accessToken) {

        if (pullReplication == null && pushReplication == null) {

            Authenticator facebookAuthenticator = AuthenticatorFactory.createFacebookAuthenticator(accessToken);

            Replication[] replications = createReplications();
            pullReplication = replications[0];
            pushReplication = replications[1];

            pullReplication.setAuthenticator(facebookAuthenticator);
            pushReplication.setAuthenticator(facebookAuthenticator);

            pullReplication.start();
            pushReplication.start();

            Log.v(TAG, "startReplicationSyncWithFacebookLogin(): Start Replication Sync ...");

        } else {
            Log.v(TAG, "startReplicationSyncWithFacebookLogin(): doing nothing, already have existing replications");

        }
    }

    public Replication[] createReplications() {

        URL syncUrl;
        try {
            syncUrl = new URL(SYNC_URL);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Invalid Sync Url", e);
            throw new RuntimeException(e);
        }

        Replication pullRep = database.createPullReplication(syncUrl);
        pullRep.setContinuous(true);
        pullRep.addChangeListener(getReplicationChangeListener());

        Replication pushRep = database.createPushReplication(syncUrl);
        pushRep.setContinuous(true);
        pushRep.addChangeListener(getReplicationChangeListener());

        return new Replication[]{pullRep, pushRep};

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
                updateSyncProgress(replication.getCompletedChangesCount(),
                        replication.getChangesCount());
            }
        };
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(Application.TAG, "Application State: onCreate()");

        initDatabase();
        initObservable();

    }


    public Database getDatabase() {
        return this.database;
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
            sp.edit().remove(PREF_CURRENT_LIST_ID);
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

    public OnSyncProgressChangeObservable getOnSyncProgressChangeObservable() {
        return onSyncProgressChangeObservable;
    }

    public OnSyncUnauthorizedObservable getOnSyncUnauthorizedObservable() {
        return onSyncUnauthorizedObservable;
    }

    public AuthenticationType getAuthenticationType() {
        return authenticationType;
    }

    static class OnSyncProgressChangeObservable extends Observable {
        private void notifyChanges(int completedCount, int totalCount) {
            SyncProgress progress = new SyncProgress();
            progress.completedCount = completedCount;
            progress.totalCount = totalCount;
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
    }



}
