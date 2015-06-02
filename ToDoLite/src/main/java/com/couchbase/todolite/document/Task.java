/**
 * Created by Pasin Suriyentrakorn <pasin@couchbase.com> on 3/4/14.
 */

package com.couchbase.todolite.document;

import android.graphics.Bitmap;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.UnsavedRevision;
import com.couchbase.lite.util.Log;
import com.couchbase.todolite.Application;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class Task {
    private static final String VIEW_NAME = "tasks";
    private static final String DOC_TYPE = "task";

    public static Query getQuery(Database database, String listDocId) {
        com.couchbase.lite.View view = database.getView(VIEW_NAME);
        if (view.getMap() == null) {
            Mapper map = new Mapper() {
                @Override
                public void map(Map<String, Object> document, Emitter emitter) {
                    if (DOC_TYPE.equals(document.get("type"))) {
                        java.util.List<Object> keys = new ArrayList<Object>();
                        keys.add(document.get("list_id"));
                        keys.add(document.get("created_at"));
                        emitter.emit(keys, document);
                    }
                }
            };
            view.setMap(map, "1");
        }

        Query query = view.createQuery();
        query.setDescending(true);

        java.util.List<Object> startKeys = new ArrayList<Object>();
        startKeys.add(listDocId);
        startKeys.add(new HashMap<String, Object>());

        java.util.List<Object> endKeys = new ArrayList<Object>();
        endKeys.add(listDocId);

        query.setStartKey(startKeys);
        query.setEndKey(endKeys);

        return query;
    }

    public static Document createTask(Database database, String title, Bitmap image, String listId) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Calendar calendar = GregorianCalendar.getInstance();
        String currentTimeString = dateFormatter.format(calendar.getTime());

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("type", DOC_TYPE);
        properties.put("title", title);
        properties.put("checked", Boolean.FALSE);
        properties.put("created_at", currentTimeString);
        properties.put("list_id", listId);

        Document document = database.createDocument();

        UnsavedRevision revision = document.createRevision();
        revision.setUserProperties(properties);

        //TODO WORKSHOP STEP 8: Add attachment to the current revision

        try {
            revision.save();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
            Log.e(Application.TAG, "Cannot create the revision", e);
        }

        Log.d(Application.TAG, "Created doc: %s", document.getId());

        return document;
    }

    //TODO WORKSHOP STEP 8: Add a static method to attach an image to a Task

    public static void updateCheckedStatus(Document task, boolean checked)
            throws CouchbaseLiteException {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.putAll(task.getProperties());
        properties.put("checked", checked);
        task.putProperties(properties);
    }

    public static void deleteTask(Document task) throws CouchbaseLiteException {
        task.delete();
        Log.d(Application.TAG, "Deleted doc: %s", task.getId());

    }
}
