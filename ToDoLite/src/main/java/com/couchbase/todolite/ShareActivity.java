package com.couchbase.todolite;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.Query;
import com.couchbase.lite.util.Log;
import com.couchbase.todolite.document.List;
import com.couchbase.todolite.document.Profile;
import com.couchbase.todolite.helper.LiveQueryAdapter;
import com.couchbase.todolite.preferences.ToDoLitePreferences;

public class ShareActivity extends BaseActivity {
    public static final String SHARE_ACTIVITY_CURRENT_LIST_ID_EXTRA = "current_list_id";
    public static final String STATE_CURRENT_LIST_ID = "current_list_id";

    private UserAdapter mAdapter = null;
    private String mCurrentListId = null;
    private Document mCurrentList = null;

    private ToDoLitePreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.preferences = new ToDoLitePreferences(getApplication());
        setContentView(R.layout.activity_share);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState != null) {
            mCurrentListId = savedInstanceState.getString(STATE_CURRENT_LIST_ID);
        } else {
            Intent intent = getIntent();
            mCurrentListId = intent.getStringExtra(SHARE_ACTIVITY_CURRENT_LIST_ID_EXTRA);
        }

        mCurrentList = application.getDatabase().getDocument(mCurrentListId);

        Query query = Profile.getQuery(application.getDatabase(), preferences.getCurrentUserId());
        mAdapter = new UserAdapter(this, query.toLiveQuery());

        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(mAdapter);
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString(STATE_CURRENT_LIST_ID, mCurrentListId);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.share, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        mAdapter.invalidate();
        super.onDestroy();
    }

    private class UserAdapter extends LiveQueryAdapter {
        public UserAdapter(Context context, LiveQuery query) {
            super(context, query);
        }

        private boolean isMemberOfTheCurrentList(Document user) {
            java.util.List<String> members =
                    (java.util.List<String>) mCurrentList.getProperty("members");
            return members != null ? members.contains(user.getId()) : false;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) parent.getContext().
                        getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.view_user, null);
            }

            final Document task = (Document) getItem(position);

            TextView text = (TextView) convertView.findViewById(R.id.text);
            text.setText((String) task.getProperty("name"));

            final Document user = (Document) getItem(position);
            final CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checked);
            boolean checked = isMemberOfTheCurrentList(user);
            checkBox.setChecked(checked);
            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        if (checkBox.isChecked()) {
                            List.addMemberToList(mCurrentList, user);
                        } else {
                            List.removeMemberFromList(mCurrentList, user);
                        }
                    } catch (CouchbaseLiteException e) {
                        Log.e(Application.TAG, "Cannot update a member to a list", e);
                    }
                }
            });

            return convertView;
        }
    }
}
