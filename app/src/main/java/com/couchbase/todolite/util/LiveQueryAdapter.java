package com.couchbase.todolite.util;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.QueryEnumerator;

public class LiveQueryAdapter extends BaseAdapter {
    private LiveQuery query;
    private QueryEnumerator enumerator;
    private Context context;

    public LiveQueryAdapter(Context context, LiveQuery query) {
        this.context = context;
        this.query = query;

        query.addChangeListener(new LiveQuery.ChangeListener() {
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
        query.start();
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
        if (query != null)
            query.stop();
    }
}
