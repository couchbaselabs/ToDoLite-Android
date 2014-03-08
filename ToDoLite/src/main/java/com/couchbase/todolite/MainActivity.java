package com.couchbase.todolite;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.util.Log;
import com.couchbase.todolite.document.List;
import com.couchbase.todolite.document.Profile;
import com.couchbase.todolite.document.Task;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;

import java.util.Arrays;

public class MainActivity extends Activity
        implements ListDrawerFragment.ListSelectionCallback {
    private ListDrawerFragment mDrawerFragment;

    private CharSequence mTitle;

    private Session.StatusCallback statusCallback = new FacebookSessionStatusCallback();

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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDrawerFragment = (ListDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        mDrawerFragment.setUp(R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        String currentListId = getCurrentListId();
        if (currentListId != null) {
            displayListContent(currentListId);
        }

        // Log the current user in and start replication sync
        Application application = (Application) getApplication();
        if (application.getCurrentUserId() != null) {
            loginWithFacebookAndStartSync();
        }
    }

    private void displayListContent(String listDocId) {
        Document document = getDatabase().getDocument(listDocId);
        getActionBar().setSubtitle((String)document.getProperty("title"));

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, TasksFragment.newInstance(listDocId))
                .commit();

        Application application = (Application)getApplication();
        application.setCurrentListId(listDocId);
    }

    @Override
    public void onListSelected(String id) {
        displayListContent(id);
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mDrawerFragment.isDrawerOpen()) {
            getMenuInflater().inflate(R.menu.main, menu);

            // Add Login button if the user has not been logged in.
            Application application = (Application) getApplication();
            if (application.getCurrentUserId() == null) {
                MenuItem shareMenuItem = menu.add(getResources().getString(R.string.action_login));
                shareMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                shareMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        loginWithFacebookAndStartSync();
                        invalidateOptionsMenu();
                        return true;
                    }
                });
            }

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

    private Session.OpenRequest getFacebookOpenRequest() {
        Session.OpenRequest request = new Session.OpenRequest(this)
                .setPermissions(Arrays.asList("email"))
                .setCallback(statusCallback);
        return request;
    }

    private void loginWithFacebookAndStartSync() {
        Session session = Session.getActiveSession();

        if (session == null) {
            session = new Session(this);
            Session.setActiveSession(session);
        }

        if (!session.isOpened() && !session.isClosed()) {
            session.openForRead(getFacebookOpenRequest());
        } else {
            Session.openActiveSession(this, true, statusCallback);
        }
    }

    private class FacebookSessionStatusCallback implements Session.StatusCallback {
        @Override
        public void call(final Session session, SessionState state, Exception exception) {
            if (session == null || !session.isOpened())
                return;

            Request.newMeRequest(session, new Request.GraphUserCallback() {
                @Override
                public void onCompleted(GraphUser user, Response response) {
                    if (user != null) {
                        String userId = (String) user.getProperty("email");
                        String name = (String) user.getName();

                        Application application = (Application) MainActivity.this.getApplication();
                        String currentUserId = application.getCurrentUserId();
                        if (currentUserId != null && !currentUserId.equals(userId)) {
                            //TODO: Update Database and all UIs
                        }

                        Document profile = null;
                        try {
                            profile = Profile.getUserProfileById(getDatabase(), userId);
                            if (profile == null)
                                profile = Profile.createProfile(getDatabase(), userId, name);
                        } catch (CouchbaseLiteException e) { }

                        try {
                            List.assignOwnerToListsIfNeeded(getDatabase(), profile);
                        } catch (CouchbaseLiteException e) { }

                        application.setCurrentUserId(userId);
                        application.startReplicationSyncWithFacebookLogin(
                                session.getAccessToken(), userId);

                        invalidateOptionsMenu();
                    }
                }
            }).executeAsync();
        }
    }

    public static class TasksFragment extends Fragment {
        private static final String ARG_LIST_DOC_ID = "id";

        public static TasksFragment newInstance(String id) {
            TasksFragment fragment = new TasksFragment();

            Bundle args = new Bundle();
            args.putString(ARG_LIST_DOC_ID, id);
            fragment.setArguments(args);

            return fragment;
        }

        public TasksFragment() { }

        private Database getDatabase() {
            Application application = (Application) getActivity().getApplication();
            return application.getDatabase();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final ListView listView = (ListView) inflater.inflate(
                    R.layout.fragment_main, container, false);

            final String listId = getArguments().getString(ARG_LIST_DOC_ID);
            LiveQuery query = Task.getQuery(getDatabase(), listId).toLiveQuery();
            final TaskAdapter adapter = new TaskAdapter(getActivity(), query);
            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
                    Document task = (Document) adapter.getItemAtPosition(position);
                    boolean checked = ((Boolean) task.getProperty("checked")).booleanValue();
                    try {
                        Task.updateCheckedStatus(task, checked);
                    } catch (CouchbaseLiteException e) {
                        Log.e(Application.TAG, "Cannot update checked status", e);
                        e.printStackTrace();
                    }
                }
            });

            ViewGroup header = (ViewGroup) inflater.inflate(
                    R.layout.view_task_create, listView, false);
            final EditText text = (EditText) header.findViewById(R.id.text);
            text.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View view, int i, KeyEvent keyEvent) {
                    if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN) &&
                            (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                        String inputText = text.getText().toString();
                        if (inputText.length() > 0) {
                            try {
                                Task.createTask(getDatabase(), inputText, listId);
                            } catch (CouchbaseLiteException e) {
                                Log.e(Application.TAG, "Cannot create new task", e);
                            }
                        }
                        text.setText("");
                        return true;
                    }

                    return false;
                }
            });

            listView.addHeaderView(header);

            return listView;
        }

        private class TaskAdapter extends LiveQueryAdapter {
            public TaskAdapter(Context context, LiveQuery query) {
                super(context, query);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    LayoutInflater inflater = (LayoutInflater) parent.getContext().
                            getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    convertView = inflater.inflate(R.layout.view_task, null);
                }

                final Document task = (Document) getItem(position);

                TextView text = (TextView) convertView.findViewById(R.id.text);
                text.setText((String) task.getProperty("title"));

                final CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checked);
                Boolean checkedPropertry = (Boolean) task.getProperty("checked");
                boolean checked = checkedPropertry != null ? checkedPropertry.booleanValue() : false;
                checkBox.setChecked(checked);
                checkBox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            Task.updateCheckedStatus(task, checkBox.isChecked());
                        } catch (CouchbaseLiteException e) {
                            Log.e(Application.TAG, "Cannot update checked status", e);
                        }
                    }
                });

                return convertView;
            }
        }
    }
}
