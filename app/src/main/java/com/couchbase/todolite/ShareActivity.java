package com.couchbase.todolite;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.util.Log;
import com.couchbase.todolite.util.LiveQueryAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShareActivity extends AppCompatActivity {
    public static final String INTENT_LIST_ID = "list_id";

    private Database mDatabase = null;
    private UserAdapter mAdapter = null;
    private Document mList = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String listId;
        if (savedInstanceState != null)
            listId = savedInstanceState.getString(INTENT_LIST_ID);
        else
            listId = getIntent().getStringExtra(INTENT_LIST_ID);

        Application application = (Application) getApplication();
        mDatabase = application.getDatabase();
        mList = mDatabase.getDocument(listId);

        mAdapter = new UserAdapter(this, getQuery().toLiveQuery());
        ListView listView = (ListView) findViewById(R.id.list);
        listView.setAdapter(mAdapter);
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString(INTENT_LIST_ID, mList.getId());
        super.onSaveInstanceState(savedInstanceState);
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

    private Query getQuery() {
        Application application = (Application) getApplication();
        Query query = application.getUserProfilesView().createQuery();
        return query;
    }

    private void addMember(Document user) {
        Map<String, Object> properties = new HashMap<>();
        properties.putAll(mList.getProperties());

        List<String> members = (List<String>) properties.get("members");
        if (members == null) {
            members = new ArrayList<String>();
            properties.put("members", members);
        }
        members.add(user.getId());

        try {
            mList.putProperties(properties);
        } catch (CouchbaseLiteException e) {
            Log.e(Application.TAG, "Cannot add member to the list", e);
        }
    }

    private void removeMember(Document user) {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.putAll(mList.getProperties());

        List<String> members = (List<String>) properties.get("members");
        if (members != null) {
            members.remove(user.getId());
            properties.put("members", members);
            try {
                mList.putProperties(properties);
            } catch (CouchbaseLiteException e) {
                Log.e(Application.TAG, "Cannot add member to the list", e);
            }
        }
    }

    private class UserAdapter extends LiveQueryAdapter {
        public UserAdapter(Context context, LiveQuery query) {
            super(context, query);
        }

        private boolean isListOwner(Document user) {
            Application application = (Application) getApplication();
            return user.getId().equals("p:" + application.getCurrentUserId());
        }

        private boolean isMemberOfTheCurrentList(Document user) {
            if (isListOwner(user))
                return true;

            List<String> members = (List<String>) mList.getProperty("members");
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
            checkBox.setEnabled(!isListOwner(user));
            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Application application = (Application) getApplication();

                    if (checkBox.isChecked())
                        addMember(user);
                    else
                        removeMember(user);
                }
            });
            return convertView;
        }
    }
}
