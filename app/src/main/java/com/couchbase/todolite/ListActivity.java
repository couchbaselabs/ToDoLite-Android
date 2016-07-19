package com.couchbase.todolite;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

public class ListActivity extends AppCompatActivity {
    private Database mDatabase = null;
    private ListAdapter mAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        Application application = (Application) getApplication();
        mDatabase = application.getDatabase();

        Query query = getQuery();
        mAdapter = new ListAdapter(this, query.toLiveQuery());

        ListView listView = (ListView) findViewById(R.id.list);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Document list = (Document) mAdapter.getItem(i);
                showTasks(list);
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int pos, long id) {
                PopupMenu popup = new PopupMenu(ListActivity.this, view);
                popup.inflate(R.menu.list_item);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        Document list = (Document) mAdapter.getItem(pos);
                        String owner = (String) list.getProperties().get("owner");
                        Application application = (Application) getApplication();
                        if (owner == null || owner.equals("p:" + application.getCurrentUserId()))
                            deleteList(list);
                        else
                            application.showErrorMessage("Only owner can delete the list", null);
                        return true;
                    }
                });
                popup.show();
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.create:
                displayCreateDialog();
                return true;
            case R.id.logout:
                logout();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        mAdapter.invalidate();
        super.onDestroy();
    }

    private void displayCreateDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getResources().getString(R.string.title_dialog_new_list));

        LayoutInflater inflater = this.getLayoutInflater();
        final View view = inflater.inflate(R.layout.view_dialog_input, null);
        final EditText input = (EditText) view.findViewById(R.id.text);
        alert.setView(view);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                try {
                    String title = input.getText().toString();
                    if (title.length() == 0)
                        return;
                    create(title);
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

    private Query getQuery() {
        com.couchbase.lite.View view = mDatabase.getView("list");
        if (view.getMap() == null) {
            Mapper mapper = new Mapper() {
                public void map(Map<String, Object> document, Emitter emitter) {
                    String type = (String)document.get("type");
                    if ("list".equals(type))
                        emitter.emit(document.get("title"), null);
                }
            };
            view.setMap(mapper, "1.0");
        }

        Query query = view.createQuery();
        return query;
    }

    private Document create(String title) throws CouchbaseLiteException {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Calendar calendar = GregorianCalendar.getInstance();
        String currentTimeString = dateFormatter.format(calendar.getTime());

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("type", "list");
        properties.put("title", title);
        properties.put("created_at", currentTimeString);
        properties.put("members", new ArrayList<String>());

        Application application = (Application) getApplication();
        String userId = application.getCurrentUserId();
        if (userId != null)
            properties.put("owner", "p:" + userId);

        Document document = mDatabase.createDocument();
        document.putProperties(properties);

        return document;
    }

    private void deleteList(Document list) {
        try {
            list.delete();
        } catch (CouchbaseLiteException e) {
            Log.e(Application.TAG, "Cannot delete list", e);
        }
    }

    private void showTasks(Document list) {
        Intent intent = new Intent(this, TaskActivity.class);
        intent.putExtra(TaskActivity.INTENT_LIST_ID, list.getId());
        startActivity(intent);
    }

    private void logout() {
        Application application = (Application) getApplication();
        application.logout();
    }

    private class ListAdapter extends LiveQueryAdapter {
        public ListAdapter(Context context, LiveQuery query) {
            super(context, query);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) parent.getContext().
                        getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.view_list, null);
            }

            final Document list = (Document) getItem(position);
            TextView text = (TextView) convertView.findViewById(R.id.text);
            text.setText((String) list.getProperty("title"));
            return convertView;
        }
    }
}
