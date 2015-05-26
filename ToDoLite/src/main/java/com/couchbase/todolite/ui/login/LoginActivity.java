package com.couchbase.todolite.ui.login;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.util.Log;
import com.couchbase.todolite.Application;
import com.couchbase.todolite.BaseActivity;
import com.couchbase.todolite.MainActivity;
import com.couchbase.todolite.R;
import com.couchbase.todolite.document.List;
import com.couchbase.todolite.document.Profile;
import com.couchbase.todolite.preferences.ToDoLitePreferences;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginFragment;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.StringTokenizer;

public class LoginActivity extends BaseActivity {

    private static final String TAG = "LoginActivity";

    private LoginFragment loginFragment;
    private LoginButton loginButton;

    private Button mBasicAuthButton;
    private Button mCookieAuthButton;
    private Button mGuestLoginButton;

    private ToDoLitePreferences preferences;
    private Application application;

    private CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.preferences = new ToDoLitePreferences(getApplication());
        this.application = (Application) getApplication();

        setContentView(R.layout.activity_login);

        loginButton = (LoginButton) findViewById(R.id.authButton);
        loginButton.setReadPermissions(Arrays.asList("email"));

        callbackManager = CallbackManager.Factory.create();
        loginButton.registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        // App code
                        loginUser(loginResult);
                    }

                    @Override
                    public void onCancel() {
                        // App code
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        // App code
                    }
                });

        mGuestLoginButton = (Button)findViewById(R.id.guestLoginButton);
        mGuestLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.setDatabaseForGuest();
                preferences.setGuestBoolean(true);

                Intent i = new Intent(LoginActivity.this, MainActivity.class);
                startActivityForResult(i, 0);
                finish();
            }
        });

        if (Application.authenticationType == Application.AuthenticationType.ALL) {

            mBasicAuthButton = (Button)findViewById(R.id.basicAuthLoginButton);
            mBasicAuthButton.setVisibility(View.VISIBLE);
            mBasicAuthButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    promptUserForBasicAuthAndStartSync();
                }
            });

            mCookieAuthButton = (Button)findViewById(R.id.cookieAuthLoginButton);
            mCookieAuthButton.setVisibility(View.VISIBLE);
            mCookieAuthButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    loginWithCustomCookieAndStartSync();
                }
            });

        }

    }

    private void loginUser(final LoginResult loginResult) {
        if (preferences.getCurrentUserId() == null) {
            GraphRequest.newMeRequest(
                    loginResult.getAccessToken(),
                    new GraphRequest.GraphJSONObjectCallback() {
                        @Override
                        public void onCompleted(JSONObject jsonObject, GraphResponse graphResponse) {

                            if (preferences.getCurrentUserId() == null) {
                                String userId = null;
                                String name = null;
                                try {
                                    userId = jsonObject.getString("id");
                                    name = jsonObject.getString("name");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                application.setDatabaseForName(userId);

                                try {
                                    Document profile = Profile.getUserProfileById(application.getDatabase(), userId);
                                    if (profile == null)
                                        profile = Profile.createProfile(application.getDatabase(), userId, name);
                                    application.migrateGuestDataToUser(profile);
                                    preferences.setGuestBoolean(false);

                                    List.assignOwnerToListsIfNeeded(application.getDatabase(), profile);
                                } catch (CouchbaseLiteException e) {
                                    e.printStackTrace();
                                }

                                Toast.makeText(LoginActivity.this, "Login successful!  Starting sync.", Toast.LENGTH_LONG).show();

                                preferences.setCurrentUserId(userId);
                                preferences.setLastReceivedFbAccessToken(loginResult.getAccessToken().getToken());

                                Intent i = new Intent(LoginActivity.this, MainActivity.class);
                                startActivityForResult(i, 0);
                                finish(); // call finish to remove this activity from history stack (for Back button to work as expected)
                            } else {
                                Intent i = new Intent(LoginActivity.this, MainActivity.class);
                                startActivityForResult(i, 0);
                                finish(); // call finish to remove this activity from history stack (convenience for Back button to work)
                            }

                        }
                    }).executeAsync();

        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (preferences.getGuestBoolean()) {
//            loginFragment.loginButton.performClick();
        }
    }

    /**
     * Allows user to enter basic auth username/password combo and start
     * sync.
     *
     * Before this will work, you must create the user on sync gateway
     *
     * curl -X POST http://localhost:4985/${db}/_user/ -d '{"name":"foo", "password":"bar"}'
     *
     */
    private void promptUserForBasicAuthAndStartSync() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        // if it's too much typing in the emulator, just replace hardcoded fake cookie here.
        String hardcodedUsernamePassCombo = "foo:bar";

        alert.setTitle("Enter username:pass combo for basic auth");
        alert.setMessage("This user must have been already created on sync gateway.");

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        input.setText(hardcodedUsernamePassCombo);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                try {
                    String value = input.getText().toString();

                    StringTokenizer st = new StringTokenizer(value, ":");
                    String username = st.nextToken();
                    String password = st.nextToken();

                    preferences.setCurrentUserId(username);
                    preferences.setCurrentUserPassword(password);

                    Intent i = new Intent(LoginActivity.this, MainActivity.class);
                    startActivityForResult(i, 0);
                    finish();
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing username/pass or starting sync", e);
                    Toast.makeText(LoginActivity.this, "Invalid username/pass", Toast.LENGTH_LONG).show();
                }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    /**
     * This is allows the user to enter a "fake" cookie, that would have to been
     * obtained manually.  In a real app, this would look like:
     *
     * - Your app prompts user for credentials
     * - Your app directly contacts your app server with these credentials
     * - Your app server creates a session on the Sync Gateway, which returns a cookie
     * - Your app server returns this cookie to your app
     *
     * Having obtained the cookie in the manner above, you would then call
     * startSyncWithCustomCookie() with this cookie.
     *
     */
    private void loginWithCustomCookieAndStartSync() {

        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        // if it's too much typing in the emulator, just replace hardcoded fake cookie here.
        String hardcodedFakeCookie = "376b9c707158a381a143060f1937935ede7cf75d";

        alert.setTitle("Enter fake cookie");
        alert.setMessage("See loginWithCustomCookieAndStartSync() for explanation.");

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        input.setText(hardcodedFakeCookie);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();

                preferences.setCurrentUserId(value);

                Intent i = new Intent(LoginActivity.this, MainActivity.class);
                startActivityForResult(i, 0);
                finish();
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

}
