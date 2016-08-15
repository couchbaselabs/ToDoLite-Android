package com.couchbase.todolite;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.couchbase.todolite.util.FacebookResults;
import com.couchbase.todolite.util.FbUtility;
import com.facebook.*;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity implements FacebookResults {
    public static final String ACTION_LOGOUT = "logout";

    private CallbackManager mCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_login);

        if (ACTION_LOGOUT.equals(getIntent().getAction())) {
            logout();
        } else {
            AccessToken accessToken = AccessToken.getCurrentAccessToken();
            if (accessToken != null && !accessToken.isExpired()) {
                onFbLoginSuccess(accessToken.getToken(), accessToken.getUserId(), null);
                return;
            }

            if (isLoggedAsGuest()) {
                loginAsGuest();
                return;
            }
        }

        LoginButton facebookLoginButton = (LoginButton) findViewById(R.id.facebook_login_button);
        facebookLoginButton.setReadPermissions("public_profile");

        mCallbackManager = CallbackManager.Factory.create();
        new FbUtility().fbCallback(facebookLoginButton, mCallbackManager, this);

        Button guestLoginButton = (Button) findViewById(R.id.guest_login_button);
        guestLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginAsGuest();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onFbLoginSuccess(String token, String userId, String name) {
        Application application = (Application) getApplication();
        application.loginAsFacebookUser(this, token, userId, name);
    }

    private void loginAsGuest() {
        SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("guest", true);
        editor.commit();
        Application application = (Application) getApplication();
        application.loginAsGuest(this);
    }

    private boolean isLoggedAsGuest() {
        SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
        return pref.getBoolean("guest", false);
    }

    private void logout() {
        SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.remove("guest");
        editor.commit();
        LoginManager.getInstance().logOut();
    }
}
