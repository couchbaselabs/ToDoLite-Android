package com.couchbase.todolite.util;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.couchbase.todolite.Application;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Moss
 */

public class FbUtility {

    public void fbCallback(LoginButton loginButton, CallbackManager mCallbackManager, final FacebookResults fbResults) {
        loginButton.setReadPermissions("email");
        loginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                fbGraphInfo(loginResult, fbResults);
            }

            @Override
            public void onCancel() {  }

            @Override
            public void onError(FacebookException error) {
                Log.e(Application.TAG, "Facebook login error", error);
            }
        });
    }

    private void fbGraphInfo(final LoginResult loginResult, final FacebookResults fbResults) {
        GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject object, GraphResponse response) {
                if (object == null) {
                    Log.e(Application.TAG, "Cannot get facebook user info after login");
                    return;
                }

                try {
                    AccessToken accessToken = loginResult.getAccessToken();
                    String token = accessToken.getToken();
                    String userId = accessToken.getUserId();
                    String name = object.getString("name");
                    fbResults.onFbLoginSuccess(token, userId, name);
                } catch (JSONException e) {
                    Log.e(Application.TAG, "Cannot get facebook user info after login", e);
                    return;
                }
            }
        });
        Bundle params = new Bundle();
        params.putString("fields", "name");
        request.setParameters(params);
        request.executeAsync();
    }
}
