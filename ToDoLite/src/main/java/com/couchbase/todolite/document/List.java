/**
 * Created by Pasin Suriyentrakorn <pasin@couchbase.com> on 3/4/14.
 */

package com.couchbase.todolite.document;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.Revision;
import com.couchbase.lite.UnsavedRevision;
import com.couchbase.lite.util.Log;
import com.couchbase.todolite.Application;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

public class List {
    private static final String VIEW_NAME = "lists";
    private static final String DOC_TYPE = "list";

    public static Query getQuery(Database database) {
        com.couchbase.lite.View view = database.getView(VIEW_NAME);
        if (view.getMap() == null) {
            Mapper mapper = new Mapper() {
                public void map(Map<String, Object> document, Emitter emitter) {
                    String type = (String)document.get("type");
                    if (DOC_TYPE.equals(type)) {
                        emitter.emit(document.get("title"), document);
                    }
                }
            };
            view.setMap(mapper, "1");
        }

        Query query = view.createQuery();
        return query;
    }

    public static Document createNewList(Database database, String title, String userId)
            throws CouchbaseLiteException {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Calendar calendar = GregorianCalendar.getInstance();
        String currentTimeString = dateFormatter.format(calendar.getTime());

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("type", "list");
        properties.put("title", title);
        properties.put("created_at", currentTimeString);
        properties.put("members", new ArrayList<String>());
        if (userId != null)
            properties.put("owner", "profile:" + userId);

        Document document = database.createDocument();
        document.putProperties(properties);

        return document;
    }

    public static void assignOwnerToListsIfNeeded(Database database, Document user)
            throws CouchbaseLiteException {
        QueryEnumerator enumerator = getQuery(database).run();

        if (enumerator == null)
            return;

        while (enumerator.hasNext()) {
            Document document = enumerator.next().getDocument();

            String owner = (String) document.getProperty("owner");
            if (owner != null) continue;

            Map<String, Object> properties = new HashMap<String, Object>();
            properties.putAll(document.getProperties());
            properties.put("owner", user.getId());
            document.putProperties(properties);
        }
    }

    public static void addMemberToList(Document list, Document user)
            throws CouchbaseLiteException {
        Map<String, Object> newProperties = new HashMap<String, Object>();
        newProperties.putAll(list.getProperties());

        java.util.List<String> members = (java.util.List<String>) newProperties.get("members");
        if (members == null) members = new ArrayList<String>();
        members.add(user.getId());
        newProperties.put("members", members);

        try {
            list.putProperties(newProperties);
        } catch (CouchbaseLiteException e) {
            com.couchbase.lite.util.Log.e(Application.TAG, "Cannot add member to the list", e);
        }
    }

    public static void removeMemberFromList(Document list, Document user)
            throws CouchbaseLiteException {
        Map<String, Object> newProperties = new HashMap<String, Object>();
        newProperties.putAll(list.getProperties());

        java.util.List<String> members = (java.util.List<String>) newProperties.get("members");
        if (members != null) members.remove(user.getId());
        newProperties.put("members", members);

        list.putProperties(newProperties);
    }
}
