package com.couchbase.todolite;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.couchbase.lite.replicator.Replication;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.net.MalformedURLException;
import java.net.URL;

public class GcmMessageHandler extends IntentService {

    private Handler handler;
    public GcmMessageHandler() {
        super("GcmMessageHandler");
    }
    private Intent mIntent;

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        handler = new Handler();
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        mIntent = intent;

        Bundle extras = intent.getExtras();

        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        showToast();
        Log.i("GCM", "Received : (" + messageType + ")  " + extras.getString("title"));

        /*
        pull, non continuous, with event listener
         */

        Application application = (Application) getApplication();

        try {
            URL url = new URL(BuildConfig.SYNC_URL_HTTP);
            Replication pull = application.getDatabase().createPullReplication(url);
            pull.addChangeListener(getReplicationListener());
            pull.start();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }

    private Replication.ChangeListener getReplicationListener() {
        return new Replication.ChangeListener() {
            @Override
            public void changed(Replication.ChangeEvent event) {
                Log.i("GCM", "replication status is : " + event.getSource().getStatus());
                if (event.getSource().getStatus() == Replication.ReplicationStatus.REPLICATION_STOPPED) {
                    GcmBroadcastReceiver.completeWakefulIntent(mIntent);
                }
            }
        };
    }

    public void showToast(){
        handler.post(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), "Server ping - sync up!", Toast.LENGTH_LONG).show();
            }
        });

    }
}