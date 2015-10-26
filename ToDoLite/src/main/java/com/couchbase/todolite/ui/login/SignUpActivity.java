package com.couchbase.todolite.ui.login;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.todolite.Application;
import com.couchbase.todolite.MainActivity;
import com.couchbase.todolite.R;
import com.couchbase.todolite.document.List;
import com.couchbase.todolite.document.Profile;
import com.couchbase.todolite.helper.NetworkHelper;
import com.couchbase.todolite.preferences.ToDoLitePreferences;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

public class SignUpActivity extends ActionBarActivity {

    NetworkHelper networkHelper = new NetworkHelper();

    EditText nameInput;
    EditText passwordInput;
    EditText confirmPasswordInput;

    private ToDoLitePreferences preferences;
    private Application application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        this.preferences = new ToDoLitePreferences(getApplication());
        this.application = (Application) getApplication();

        nameInput = (EditText) findViewById(R.id.nameInput);
        passwordInput = (EditText) findViewById(R.id.passwordInput);
        confirmPasswordInput = (EditText) findViewById(R.id.confirmPasswordInput);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sign_up, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void signup(View view) {
        if (!passwordInput.getText().toString().equals(confirmPasswordInput.getText().toString())) {
            Toast.makeText(getApplicationContext(), "The passwords do not match", Toast.LENGTH_LONG).show();
        } else {
            String json = "{\"name\": \"" + nameInput.getText() + "\", \"password\":\"" + passwordInput.getText() + "\"}";
            networkHelper.post(Application.SYNC_HOSTNAME + "/signup", json, new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {

                }

                @Override
                public void onResponse(Response response) throws IOException {
                    if (response.code() == 201) {
                        String userId = String.valueOf(nameInput.getText());
                        String name = userId;
                        String password = String.valueOf(passwordInput.getText());

                        application.setDatabaseForName(name);

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

//        Toast.makeText(LoginActivity.this, "Login successful!  Starting sync.", Toast.LENGTH_LONG).show();

                        preferences.setCurrentUserId(userId);
                        preferences.setCurrentUserPassword(password);

                        Intent i = new Intent(SignUpActivity.this, MainActivity.class);
                        startActivityForResult(i, 0);
                        finish(); // call finish to remove this activity from history stack (for Back button to work as expected)
                    }
                    String responseStr = response.body().string();
                    final String messageText = "Status code : " + response.code() +
                            "\n" +
                            "Response body : " + responseStr;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), messageText, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        }
    }
}