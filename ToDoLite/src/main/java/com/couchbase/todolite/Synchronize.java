package com.couchbase.todolite;

import android.app.Activity;

import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.AuthenticatorFactory;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.Database;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by jamesnocentini on 01/02/15.
 */
public class Synchronize {

    public Replication pullReplication;
    public Replication pushReplication;

    private boolean facebookAuth;
    private boolean basicAuth;
    private boolean cookieAuth;

    public static class Builder {
        public Replication pullReplication;
        public Replication pushReplication;

        private boolean facebookAuth;
        private boolean basicAuth;
        private boolean cookieAuth;

        public Builder(Database database, String url, Boolean continuousPull) {

            if (pullReplication == null && pushReplication == null) {

                URL syncUrl;
                try {
                    syncUrl = new URL(url);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }

                pullReplication = database.createPullReplication(syncUrl);
                if (continuousPull == true)
                    pullReplication.setContinuous(true);

                pushReplication = database.createPushReplication(syncUrl);
                pushReplication.setContinuous(true);
            }
        }

        public Builder facebookAuth(String token) {

            Authenticator facebookAuthenticator = AuthenticatorFactory.createFacebookAuthenticator(token);

            pullReplication.setAuthenticator(facebookAuthenticator);
            pushReplication.setAuthenticator(facebookAuthenticator);

            return this;
        }

        public Builder basicAuth(String username, String password) {

            Authenticator basicAuthenticator = AuthenticatorFactory.createBasicAuthenticator(username, password);

            pullReplication.setAuthenticator(basicAuthenticator);
            pushReplication.setAuthenticator(basicAuthenticator);

            return this;
        }

        public Builder cookieAuth(String cookieValue) {

            String cookieName = "SyncGatewaySession";
            boolean isSecure = false;
            boolean httpOnly = false;

            // expiration date - 1 day from now
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            int numDaysToAdd = 1;
            cal.add(Calendar.DATE, numDaysToAdd);
            Date expirationDate = cal.getTime();

            pullReplication.setCookie(cookieName, cookieValue, "/", expirationDate, isSecure, httpOnly);
            pushReplication.setCookie(cookieName, cookieValue, "/", expirationDate, isSecure, httpOnly);

            return this;
        }

        public Builder addChangeListener(Replication.ChangeListener changeListener) {
            pullReplication.addChangeListener(changeListener);
            pushReplication.addChangeListener(changeListener);

            return this;
        }

        public Synchronize build() {
            return new Synchronize(this);
        }

    }

    private Synchronize(Builder builder) {
        pullReplication = builder.pullReplication;
        pushReplication = builder.pushReplication;

        facebookAuth = builder.facebookAuth;
        basicAuth = builder.basicAuth;
        cookieAuth = builder.cookieAuth;
    }

    public void start() {
        pullReplication.start();
        pushReplication.start();
    }

    public void destroyReplications() {
        pullReplication.stop();
        pushReplication.stop();
        pullReplication.deleteCookie("SyncGatewaySession");
        pushReplication.deleteCookie("SyncGatewaySession");
        pullReplication = null;
        pushReplication = null;
    }

}
