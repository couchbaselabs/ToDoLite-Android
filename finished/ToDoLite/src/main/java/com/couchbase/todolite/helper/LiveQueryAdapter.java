/**
 * Created by Pasin Suriyentrakorn <pasin@couchbase.com> on 2/27/14.
 */

package com.couchbase.todolite.helper;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.couchbase.lite.Database;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.QueryEnumerator;

public class LiveQueryAdapter extends BaseAdapter {
    private LiveQuery liveQuery;
    private QueryEnumerator enumerator;
    private Context context;

    public LiveQueryAdapter(Context context, LiveQuery liveQuery) {
        this.context = context;
        this.liveQuery = liveQuery;

        this.liveQuery.addChangeListener(new LiveQuery.ChangeListener() {
            @Override
            public void changed(final LiveQuery.ChangeEvent event) {
                ((Activity) LiveQueryAdapter.this.context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        enumerator = event.getRows();
                        notifyDataSetChanged();
                    }
                });
            }
        });

        this.liveQuery.start();
    }

    @Override
    public int getCount() {
        return enumerator != null ? enumerator.getCount() : 0;
    }

    @Override
    public Object getItem(int i) {
        return enumerator != null ? enumerator.getRow(i).getDocument() : null;
    }

    @Override
    public long getItemId(int i) {
        return enumerator.getRow(i).getSequenceNumber();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return null;
    }

    public void invalidate() {
        if (liveQuery != null)
            liveQuery.stop();
    }

    /*
    Method called in the database change listener when a new change is detected.
    Because live queries do not trigger a change event when non current revisions are saved
    or pulled from a remote database.
     */
    public void updateQueryToShowConflictingRevisions(final Database.ChangeEvent event) {

        ((Activity) LiveQueryAdapter.this.context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                liveQuery.stop();
                enumerator = liveQuery.getRows();
                notifyDataSetChanged();
            }
        });

    }
}
