package com.couchbase.todolite;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.SavedRevision;
import com.couchbase.lite.TransactionalTask;
import com.couchbase.lite.UnsavedRevision;
import com.couchbase.lite.util.Log;
import com.couchbase.todolite.Application;
import com.couchbase.todolite.ConflictsAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ListConflicts extends ListActivity {

    private ArrayList<SavedRevision> conflicts;
    private Document task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
        Get the task document with conflicting revisions
        and display them on the list view.
         */
        String id = (String) getIntent().getExtras().get("DOC_ID");
        final Application application = (Application) getApplication();
        task = application.getDatabase().getDocument(id);

        try {
            conflicts = new ArrayList<SavedRevision>(task.getConflictingRevisions());
        } catch (CouchbaseLiteException e) {
            Log.e(Application.TAG, "Cannot get the conflicting revisions");
        }

        ConflictsAdapter adapter = new ConflictsAdapter(this, conflicts);
        this.setListAdapter(adapter);

    }
}
