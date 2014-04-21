/**
 * Created by Pasin Suriyentrakorn <pasin@couchbase.com> on 2/26/14.
 */

package com.couchbase.todolite;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.AuthenticatorFactory;
import com.couchbase.lite.auth.FacebookAuthorizer;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Observable;

public class Application extends android.app.Application {
    public static final String TAG = "ToDoLite";
    private static final String DATABASE_NAME = "todos";
    private static final String SYNC_URL = "http://sync.couchbasecloud.com:4984/todos4/";

    private static final String PREF_CURRENT_LIST_ID = "CurrentListId";
    private static final String PREF_CURRENT_USER_ID = "CurrentUserId";

    private Manager manager;
    private Database database;

    private int syncCompletedChangedCount;
    private int syncTotalChangedCount;
    private OnSyncProgressChangeObservable onSyncProgressChangeObservable;

    private void initDatabase() {
        try {
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
    }

    private synchronized void updateSyncProgress(int completedCount, int totalCount) {
        onSyncProgressChangeObservable.notifyChanges(completedCount, totalCount);
    }

    public void startReplicationSyncWithFacebookLogin(String accessToken, String email) {
        URL syncUrl;
        try {
            syncUrl = new URL(SYNC_URL);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Invalid Sync Url", e);
            throw new RuntimeException(e);
        }

        Replication pullRep = database.createPullReplication(syncUrl);
        pullRep.setContinuous(true);
        Authenticator facebookAuthenticator = AuthenticatorFactory.createFacebookAuthenticator(accessToken);
        pullRep.setAuthenticator(facebookAuthenticator);
        pullRep.addChangeListener(new Replication.ChangeListener() {
            @Override
            public void changed(Replication.ChangeEvent event) {
                Replication replication = event.getSource();
                updateSyncProgress(replication.getCompletedChangesCount(),
                        replication.getChangesCount());
            }
        });

        Replication pushRep = database.createPushReplication(syncUrl);
        pushRep.setContinuous(true);
        pullRep.setAuthenticator(facebookAuthenticator);
        pushRep.addChangeListener(new Replication.ChangeListener() {
            @Override
            public void changed(Replication.ChangeEvent event) {
                Replication replication = event.getSource();
                updateSyncProgress(replication.getCompletedChangesCount(),
                        replication.getChangesCount());
            }
        });

        pullRep.start();
        pushRep.start();

        Log.v(TAG, "Start Replication Sync ...");
    }

    @Override
    public void onCreate() {
        super.onCreate();

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
            sp.edit().remove(PREF_CURRENT_USER_ID);
        }
    }

    public OnSyncProgressChangeObservable getOnSyncProgressChangeObservable() {
        return onSyncProgressChangeObservable;
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

    static class SyncProgress {
        public int completedCount;
        public int totalCount;
    }
}
