package com.couchbase.todolite;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.couchbase.lite.Document;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.QueryEnumerator;

import java.util.HashMap;

public class LiveQueryRecyclerAdapter extends RecyclerView.Adapter<LiveQueryRecyclerAdapter.ViewHolder> implements View.OnClickListener {

    private Context context;
    private LiveQuery query;
    private QueryEnumerator enumerator;

    private OnItemClickListener mListener;

    public LiveQueryRecyclerAdapter(Context context, LiveQuery query, OnItemClickListener listener, String name) {
        this.context = context;
        this.query = query;
        this.mListener = listener;

        query.addChangeListener(new LiveQuery.ChangeListener() {
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
        query.start();
    }

    @Override
    public LiveQueryRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        final View view = LayoutInflater.from(context).inflate(R.layout.recycler_view_item_row, viewGroup, false);
        ViewHolder holder = new ViewHolder(view, i);
        holder.textView.setOnClickListener(this);
        return holder;
    }

    @Override
    public void onBindViewHolder(LiveQueryRecyclerAdapter.ViewHolder viewHolder, int i) {
        final Document task = (Document) getItem(i);
        viewHolder.textView.setText((String) task.getProperty("title"));
        viewHolder.textView.setTag(viewHolder);
    }

    @Override
    public int getItemCount() {
        return enumerator != null ? enumerator.getCount() : 0;
    }

    public Object getItem(int i) {
        return enumerator != null ? enumerator.getRow(i).getDocument() : null;
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.listRowText) {
            ViewHolder holder = (ViewHolder) v.getTag();
            Document document = (Document) getItem(holder.getPosition());
            mListener.onItemClick((String) document.getProperty("_id"));
        }
    }

    public static interface OnItemClickListener {
        public void onItemClick(String listId);
    }

    // Creating a ViewHolder class which extends the RecyclerView.ViewHolder
    // ViewHolder are used to store the inflated views in order to recycle them
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(View itemView, int ViewType) {
            super(itemView);
            textView = (TextView) itemView.findViewById(R.id.listRowText);
        }

    }
}
