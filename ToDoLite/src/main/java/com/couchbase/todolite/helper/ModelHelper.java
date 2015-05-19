package com.couchbase.todolite.helper;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class ModelHelper {

    public static Document save(Database database, Object object) {
        ObjectMapper m = new ObjectMapper();
        Map<String, Object> props = m.convertValue(object, Map.class);
        String id = (String) props.get("_id");

        Document document;
        if (id == null) {
            document = database.createDocument();
        } else {
            document = database.getExistingDocument(id);
            if (document == null) {
                document = database.getDocument(id);
            } else {
                props.put("_rev", document.getProperty("_rev"));
            }
        }

        try {
            document.putProperties(props);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        return document;
    }

    public static <T> T modelForDocument(Document document, Class<T> aClass) {
        ObjectMapper m = new ObjectMapper();
        return m.convertValue(document.getProperties(), aClass);
    }

}
