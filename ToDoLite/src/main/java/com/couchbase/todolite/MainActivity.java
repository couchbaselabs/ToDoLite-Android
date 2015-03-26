package com.couchbase.todolite;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.util.Log;
import com.couchbase.todolite.document.List;
import com.facebook.Session;

import java.util.Observable;
import java.util.Observer;

public class MainActivity extends BaseActivity {

    private static final String TAG = Application.TAG;
    private CharSequence mTitle;
    private DrawerLayout mDrawerLayout;

    private Database getDatabase() {
        Application application = (Application) getApplication();
        return application.getDatabase();
    }

    private String getCurrentListId() {
        Application application = (Application) getApplication();
        String currentListId = application.getCurrentListId();
        if (currentListId == null) {
            try {
                QueryEnumerator enumerator = List.getQuery(getDatabase()).run();
                if (enumerator.getCount() > 0) {
                    currentListId = enumerator.getRow(0).getDocument().getId();
                }
            } catch (CouchbaseLiteException e) { }
        }
        return currentListId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(Application.TAG, "MainActivity State: onCreate()");

        if (application.getCurrentUserId() != null && application.getCurrentUserPassword() != null) { // basic auth
            application.setDatabaseForName(application.getCurrentUserId());
            application.startReplicationSyncWithBasicAuth(application.getCurrentUserId(), application.getCurrentUserPassword());
        } else if (application.getLastReceivedFbAccessToken() != null) { // fb auth
            application.setDatabaseForName(application.getCurrentUserId());
            application.startReplicationSyncWithFacebookLogin(application.getLastReceivedFbAccessToken());
        } else if (application.getCurrentUserId() != null) { // cookie auth
            application.setDatabaseForName(application.getCurrentUserId());
            application.startReplicationSyncWithCustomCookie(application.getCurrentUserId());
        } else if (application.getGuestBoolean()) {
            application.setDatabaseForGuest();
        } else {
            Intent i = new Intent(MainActivity.this, LoginActivity.class);
            startActivityForResult(i, 0);
            finish();
        }

        setupTodoLists();
        setupDrawer();

        ((TextView) findViewById(R.id.name)).setText(application.getCurrentUserId());

        mTitle = getTitle();

        String currentListId = getCurrentListId();
        if (currentListId != null) {
            displayListContent(currentListId);
        }

        application.getOnSyncProgressChangeObservable().addObserver(new Observer() {
            @Override
            public void update(final Observable observable, final Object data) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Application.SyncProgress progress = (Application.SyncProgress) data;
                        Log.d(TAG, "Sync progress changed.  Completed: %d Total: %d Status: %s", progress.completedCount, progress.totalCount, progress.status);

                        if (progress.status == Replication.ReplicationStatus.REPLICATION_ACTIVE) {
                            Log.d(TAG, "Turn on progress spinny");
                            setProgressBarIndeterminateVisibility(true);
                        } else {
                            Log.d(TAG, "Turn off progress spinny");
                            setProgressBarIndeterminateVisibility(false);
                        }
                    }
                });
            }
        });

        application.getOnSyncUnauthorizedObservable().addObserver(new Observer() {
            @Override
            public void update(Observable observable, Object data) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Log.d(Application.TAG, "OnSyncUnauthorizedObservable called, show toast");

                        // clear the saved user id, since our session is no longer valid
                        // and we want to show the login button
                        Application application = (Application) getApplication();
                        application.setCurrentUserId(null);
                        application.setCurrentUserPassword(null);
                        invalidateOptionsMenu();

                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();

                        String msg = "Sync unable to continue due to invalid session/login";
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();

                    }
                });

            }
        });

    }

    void setupTodoLists() {
        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        LiveQuery liveQuery = List.getQuery(getDatabase()).toLiveQuery();
        RecyclerView.Adapter mAdapter = new LiveQueryRecyclerAdapter(this, liveQuery, new LiveQueryRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String listId) {
                displayListContent(listId);

                mDrawerLayout.closeDrawers();
            }
        }, application.getCurrentUserId());
        mRecyclerView.setAdapter(mAdapter);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
    }

    void setupDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        android.support.v7.app.ActionBarDrawerToggle mDrawerToggle = new android.support.v7.app.ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);

            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);

            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(Application.TAG, "MainActivity State: onStart()");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(Application.TAG, "MainActivity State: onRestart()");

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(Application.TAG, "MainActivity State: onResume()");

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Log.d(Application.TAG, "MainActivity State: onPostResume()");

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(Application.TAG, "MainActivity State: onPause()");

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(Application.TAG, "MainActivity State: onStop()");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(Application.TAG, "MainActivity State: onDestroy()");

    }


    private void displayListContent(String listDocId) {
        Document document = getDatabase().getDocument(listDocId);
        getSupportActionBar().setSubtitle((String)document.getProperty("title"));

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, TasksFragment.newInstance(listDocId))
                .commit();

        Application application = (Application)getApplication();
        application.setCurrentListId(listDocId);
    }

    public void restoreActionBar() {
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mDrawerLayout.isDrawerOpen(findViewById(R.id.drawer))) {
            getMenuInflater().inflate(R.menu.main, menu);

            // Add Login button if the user has not been logged in.
            Application application = (Application) getApplication();

            // Add Share button if the user has been logged in
            if (application.getCurrentUserId() != null && getCurrentListId() != null) {
                MenuItem shareMenuItem = menu.add(getResources().getString(R.string.action_share));
                shareMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                shareMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        Intent intent = new Intent(MainActivity.this, ShareActivity.class);
                        intent.putExtra(ShareActivity.SHARE_ACTIVITY_CURRENT_LIST_ID_EXTRA,
                                getCurrentListId());
                        MainActivity.this.startActivity(intent);
                        return true;
                    }
                });
                MenuItem logoutMenuItem = menu.add("Logout");
                logoutMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                logoutMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        Application application = (Application) getApplication();
                        application.logoutUser();
                        invalidateOptionsMenu();
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                        return true;
                    }
                });
            } else if (application.getGuestBoolean()) {
                MenuItem loginMenuItem = menu.add("Login");
                loginMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                loginMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        Intent i = new Intent(MainActivity.this, LoginActivity.class);
                        startActivityForResult(i, 0);
                        return true;
                    }
                });
            }
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    private void createNewList() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getResources().getString(R.string.title_dialog_new_list));

        final EditText input = new EditText(this);
        input.setMaxLines(1);
        input.setSingleLine(true);
        input.setHint(getResources().getString(R.string.hint_new_list));
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String title = input.getText().toString();
                if (title.length() == 0) {
                    // TODO: Show an error message.
                    return;
                }
                try {
                    String currentUserId = ((Application)getApplication()).getCurrentUserId();
                    Document document = List.createNewList(getDatabase(), title, currentUserId);
                    displayListContent(document.getId());
                    invalidateOptionsMenu();
                } catch (CouchbaseLiteException e) {
                    Log.e(Application.TAG, "Cannot create a new list", e);
                }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) { }
        });

        alert.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_new_list) {
            createNewList();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
    }

    private void startSyncWithCustomCookie(String cookieVal) {
        Application application = (Application) MainActivity.this.getApplication();
        application.startReplicationSyncWithCustomCookie(cookieVal);
    }

    private void startSyncWithBasicAuth(String username, String password) {
        Application application = (Application) MainActivity.this.getApplication();
        application.startReplicationSyncWithBasicAuth(username, password);
    }

}
