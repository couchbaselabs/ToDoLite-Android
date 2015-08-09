/**
 * Created by Pasin Suriyentrakorn <pasin@couchbase.com> on 2/26/14.
 */

package com.couchbase.todolite;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.BasicAuthenticator;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.util.Log;
import com.couchbase.todolite.document.Profile;
import com.couchbase.todolite.preferences.ToDoLitePreferences;

import org.apache.http.client.HttpResponseException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class Application extends android.app.Application {

    public static final String TAG = "ToDoLite";
    private static final String DATABASE_NAME = "todos";
    private static final String SYNC_URL = "http://10.17.3.228:4984/todos";

    private Manager manager;
    private Database database;
    private Replication pullReplication;
    private Replication pushReplication;

    private ToDoLitePreferences preferences;

    private void initDatabase() {
        //TODO WORKSHOP STEP 1: initialize the database
        try {

            Manager.enableLogging(Application.TAG, Log.VERBOSE);

            manager = new Manager(new AndroidContext(getApplicationContext()), Manager.DEFAULT_OPTIONS);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            database = manager.getDatabase(DATABASE_NAME);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }

    public void setupReplication(){
        URL syncURL = null;
        try {
            syncURL = new URL(SYNC_URL);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        pullReplication = database.createPullReplication(syncURL);
        pushReplication = database.createPullReplication(syncURL);

        pullReplication.setContinuous(true);
        pushReplication.setContinuous(true);

        pullReplication.start();
        pushReplication.start();
    }

    public void setupReplicationWithName(String name, String password) {
        URL syncURL = null;
        try {
            syncURL = new URL(SYNC_URL);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        Authenticator authenticator = new BasicAuthenticator(name, password);
        pullReplication = database.createPullReplication(syncURL);
        pushReplication = database.createPushReplication(syncURL);

        pullReplication.setAuthenticator(authenticator);
        pushReplication.setAuthenticator(authenticator);

        pullReplication.setContinuous(true);
        pushReplication.setContinuous(true);

        pullReplication.start();
        pushReplication.start();
    }

    private Replication.ChangeListener getReplicationChangeListener() {
        return new Replication.ChangeListener() {

            @Override
            public void changed(Replication.ChangeEvent event) {
                Replication replication = event.getSource();
                if (event.getError() != null) {
                    Throwable lastError = event.getError();
                    if (lastError.getMessage().contains("existing change tracker")) {
                        Log.d(TAG, "Replication Event", String.format("Sync error: %s:", lastError.getMessage()));
                    }
                    if (lastError instanceof HttpResponseException) {
                        HttpResponseException responseException = (HttpResponseException) lastError;
                        if (responseException.getStatusCode() == 401) {
                            Log.d(TAG, "401 error: " + lastError.getMessage());
                        }
                    }
                }
                Log.d(TAG, event.toString());
            }
        };
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.preferences = new ToDoLitePreferences(getApplicationContext());

        Log.d(Application.TAG, "Application State: onCreate()");
        initDatabase();

        preferences.setCurrentUserId("oliver");
        try {
            Document document = Profile.createProfile(database, "oliver", "Oliver Smith");
            Log.d(Application.TAG, "Saved document with properties %s", document.getProperties().toString());
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }

        setupReplicationWithName("oliver", "letmein");
    }
    
    public Database getDatabase() {
        return this.database;
    }

}
