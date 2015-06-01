package com.couchbase.todolite.helper;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.QueryEnumerator;

public class LiveQueryRecyclerAdapter<T extends RecyclerView.ViewHolder> extends RecyclerView.Adapter {

    public Context context;
    private LiveQuery liveQuery;
    private QueryEnumerator enumerator;

    public LiveQueryRecyclerAdapter(Context context, LiveQuery liveQuery) {
        this.context = context;
        this.liveQuery = liveQuery;

        this.liveQuery.addChangeListener(new LiveQuery.ChangeListener() {
            @Override
            public void changed(final LiveQuery.ChangeEvent event) {
                ((ActionBarActivity) LiveQueryRecyclerAdapter.this.context).runOnUiThread(new Runnable() {
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
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return enumerator != null ? enumerator.getCount() : 0;
    }

    public Object getItem(int i) {
        return enumerator != null ? enumerator.getRow(i).getDocument() : null;
    }
}
