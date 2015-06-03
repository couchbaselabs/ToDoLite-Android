package com.couchbase.todolite;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.couchbase.lite.Document;
import com.couchbase.lite.SavedRevision;
import com.couchbase.lite.util.Log;
import com.couchbase.todolite.R;

import org.w3c.dom.Text;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class ConflictsAdapter extends ArrayAdapter<SavedRevision> {


    public ConflictsAdapter(Context context, ArrayList<SavedRevision> conflicts) {
        super(context, 0, conflicts);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SavedRevision task = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
        }

        /*
        Convert the updated_at property string to a more readable
        date string
         */
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String dateString = null;
        try {
            dateString = dateFormat.parse((String) task.getProperty("updated_at")).toString();
        } catch (ParseException e) {
            Log.e(Application.TAG, "Cannot parse date", e);
        }


        TextView tvTitle = (TextView) convertView.findViewById(R.id.titleLabel);
        TextView tvDate = (TextView) convertView.findViewById(R.id.updatedDateLabel);
        TextView tvUserId = (TextView) convertView.findViewById(R.id.userIdLabel);

        tvTitle.setText((String) task.getProperty("title"));
        tvDate.setText("Last edited: " + dateString);

        String userId = (String) task.getProperty("user_id");
        Application application = (Application) getContext().getApplicationContext();
        Document profile = (Document) application.getDatabase().getDocument("p:" + userId);

        tvUserId.setText("Edited by: " + ((String) profile.getProperty("name")));

        return convertView;
    }
}
