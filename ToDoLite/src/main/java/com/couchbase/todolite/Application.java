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
import com.couchbase.lite.auth.FacebookAuthorizer;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class Application extends android.app.Application {
    public static final String TAG = "ToDoLite";
    private static final String DATABASE_NAME = "todos";
    private static final String SYNC_URL = "http://10.17.26.89:4984/todos/";

    private static final String PREF_CURRENT_LIST_ID = "CurrentListId";
    private static final String PREF_CURRENT_USER_ID = "CurrentUserId";

    private Manager manager;
    private Database database;

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

    public void startReplicationSyncWithFacebookLogin(String accessToken, String email) {
        URL syncUrl;
        try {
            syncUrl = new URL(SYNC_URL);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Invalid Sync Url", e);
            throw new RuntimeException(e);
        }

        FacebookAuthorizer auth = new FacebookAuthorizer(email);
        auth.registerAccessToken(accessToken, email, syncUrl.toString());

        Replication pullRep = database.createPullReplication(syncUrl);
        pullRep.setContinuous(true);
        pullRep.setAuthorizer(auth);

        Replication pushRep = database.createPushReplication(syncUrl);
        pushRep.setContinuous(true);
        pushRep.setAuthorizer(auth);

        pullRep.start();
        pushRep.start();

        Log.v(TAG, "Start Replication Sync ...");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initDatabase();
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
        } else
            sp.edit().remove(PREF_CURRENT_LIST_ID);
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
        } else
            sp.edit().remove(PREF_CURRENT_USER_ID);
    }
}
