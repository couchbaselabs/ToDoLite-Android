package com.couchbase.todolite;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.util.Log;
import com.couchbase.todolite.document.List;
import com.couchbase.todolite.document.Profile;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;

import java.util.StringTokenizer;

public class LoginActivity extends FragmentActivity {

    private static final String TAG = "LoginActivity";

    private UiLifecycleHelper uiHelper;
    private Button mLoginButton;
    private LoginFragment loginFragment;

    private Button mBasicAuthButton;
    private Button mCookieAuthButton;
    private Button mGuestLoginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (savedInstanceState == null) {
            loginFragment = new LoginFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragmentContainer, loginFragment)
                    .commit();
        } else {
            loginFragment = (LoginFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.fragmentContainer);
        }

        uiHelper = new UiLifecycleHelper(this, callback);
        uiHelper.onCreate(savedInstanceState);

        final Application application = (Application) getApplication();

        mGuestLoginButton = (Button)findViewById(R.id.guestLoginButton);
        mGuestLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.setDatabaseForGuest();
                application.setGuestBoolean(true);

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

    @Override
    protected void onStart() {
        super.onStart();

        final Application application = (Application) getApplication();
        if (application.getGuestBoolean()) {
            loginFragment.mLoginButton.performClick();
        }
    }

    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };

    private void onSessionStateChange(final Session session, SessionState state, Exception exception) {
        if (state.isOpened()) {

            final Application application = (Application) getApplication();

            if (application.getCurrentUserId() == null) {

                Request.newMeRequest(session, new Request.GraphUserCallback() {
                    @Override
                    public void onCompleted(GraphUser user, Response response) {
                        String userId = (String) user.getId();
                        String name = (String) user.getName();

                        application.setDatabaseForName(userId);

                        Document profile = null;
                        try {
                            profile = Profile.getUserProfileById(application.getDatabase(), userId);
                            if (profile == null)
                                profile = Profile.createProfile(application.getDatabase(), userId, name);
                                application.migrateGuestDataToUser(profile);
                                application.setGuestBoolean(false);
                        } catch (CouchbaseLiteException e) { }

                        try {
                            List.assignOwnerToListsIfNeeded(application.getDatabase(), profile);
                        } catch (CouchbaseLiteException e) { }

                        Toast.makeText(LoginActivity.this, "Login successful!  Starting sync.", Toast.LENGTH_LONG).show();

                        application.setCurrentUserId(userId);
                        application.setLastReceivedFbAccessToken(session.getAccessToken());

                        Intent i = new Intent(LoginActivity.this, MainActivity.class);
                        startActivityForResult(i, 0);
                        finish(); // call finish to remove this activity from history stack (for Back button to work as expected)
                    }
                }).executeAsync();

            } else {

                Intent i = new Intent(LoginActivity.this, MainActivity.class);
                startActivityForResult(i, 0);
                finish(); // call finish to remove this activity from history stack (convenience for Back button to work)

            }

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

                    Application application = (Application) getApplication();
                    application.setCurrentUserId(username);
                    application.setCurrentUserPassword(password);

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

                Application application = (Application) getApplication();
                application.setCurrentUserId(value);

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
    public void onResume() {
        super.onResume();
        uiHelper.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }
}
