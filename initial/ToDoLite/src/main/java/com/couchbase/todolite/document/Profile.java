/**
 * Created by Pasin Suriyentrakorn <pasin@couchbase.com> on 3/5/14.
 */

package com.couchbase.todolite.document;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

public class Profile {
    private static final String VIEW_NAME = "profiles";
    private static final String BY_ID_VIEW_NAME = "profiles_by_id";
    private static final String DOC_TYPE = "profile";

    public static Query getQuery(Database database, final String ignoreUserId) {
        com.couchbase.lite.View view = database.getView(VIEW_NAME);
        if (view.getMap() == null) {
            Mapper map = new Mapper() {
                @Override
                public void map(Map<String, Object> document, Emitter emitter) {
                    if (DOC_TYPE.equals(document.get("type"))) {
                        if (ignoreUserId == null ||
                                (ignoreUserId != null &&
                                        !ignoreUserId.equals(document.get("user_id")))) {
                            emitter.emit(document.get("name"), document);
                        }
                    }
                }
            };
            view.setMap(map, "1");
        }

        Query query = view.createQuery();
        return query;
    }

    public static Query getQueryById(Database database, String userId) {
        com.couchbase.lite.View view = database.getView(BY_ID_VIEW_NAME);
        if (view.getMap() == null) {
            Mapper map = new Mapper() {
                @Override
                public void map(Map<String, Object> document, Emitter emitter) {
                    if (DOC_TYPE.equals(document.get("type"))) {
                        emitter.emit(document.get("user_id"), document);
                    }
                }
            };
            view.setMap(map, "1");
        }

        Query query = view.createQuery();
        java.util.List<Object> keys = new ArrayList<Object>();
        keys.add(userId);
        query.setKeys(keys);

        return query;
    }

    public static Document getUserProfileById(Database database, String userId) {
        Document profile = null;
        try {
            QueryEnumerator enumerator = Profile.getQueryById(database, userId).run();
            profile = enumerator != null && enumerator.getCount() > 0 ?
                    enumerator.getRow(0).getDocument() : null;
        } catch (CouchbaseLiteException e) { }

        return profile;
    }

    public static Document createProfile(Database database, String userId, String name)
            throws CouchbaseLiteException {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Calendar calendar = GregorianCalendar.getInstance();
        String currentTimeString = dateFormatter.format(calendar.getTime());

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("type", DOC_TYPE);
        properties.put("user_id", userId);
        properties.put("name", name);

        Document document = database.getDocument("p:" + userId);
        document.putProperties(properties);

        return document;
    }
}
