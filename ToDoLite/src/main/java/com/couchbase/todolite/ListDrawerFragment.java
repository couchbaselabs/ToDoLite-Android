package com.couchbase.todolite;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.todolite.document.List;

public class ListDrawerFragment extends Fragment {
    private static final String TAG = "ToDoLite";

    private static final String LISTS_VIEW = "lists_view";
    private static final String STATE_SELECTED_LIST_ID = "selected_list_id";

    private ListSelectionCallback mCallbacks;

    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;
    private View mFragmentContainerView;

    private int mCurrentSelectedPosition = -1;

    private ListsAdapter mListsAdapter;
    private LiveQuery mListsLiveQuery;

    public ListDrawerFragment() {

    }

    private Database getDatabase() {
        Application application = (Application) getActivity().getApplication();
        return application.getDatabase();
    }

    private int getCurrentSelectedPosition(QueryEnumerator enumerator) {
        if (enumerator == null)
            return  -1;

        Application application = (Application) getActivity().getApplication();
        String currentListId = application.getCurrentListId();

        if (currentListId == null)
            return enumerator.getCount() > 0 ? 0 : -1;

        int position = 0;
        while (enumerator.hasNext()) {
            if (currentListId.equals(enumerator.next().getDocument().getId()))
                break;
            ++position;
        }
        return position;
    }

    private LiveQuery getLiveQuery() {
        LiveQuery query = List.getQuery(getDatabase()).toLiveQuery();
        query.addChangeListener(new LiveQuery.ChangeListener() {
            @Override
            public void changed(final LiveQuery.ChangeEvent event) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        QueryEnumerator changedEnumerator = event.getRows();
                        mListsAdapter.update(changedEnumerator);
                        int position = getCurrentSelectedPosition(changedEnumerator);
                        if (position != -1 && position != mCurrentSelectedPosition) {
                            selectListItem(position, false);
                        }
                    }
                });
            }
        });

        return query;
    }

    private void restartLiveQuery() {
        if (mListsLiveQuery != null)
            mListsLiveQuery.stop();

        mListsLiveQuery = getLiveQuery();
        mListsLiveQuery.start();
    }

    public void refreshLists() {
        restartLiveQuery();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mListsAdapter = new ListsAdapter(getActivity(), null);
        restartLiveQuery();
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mDrawerListView = (ListView) inflater.inflate(
                R.layout.fragment_list_drawer, container, false);

        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
                selectListItem(position, true);
            }
        });

        mDrawerListView.setAdapter(mListsAdapter);
        if (mCurrentSelectedPosition > mListsAdapter.getCount()) {
            mDrawerListView.setItemChecked(mCurrentSelectedPosition, true);
        }

        return mDrawerListView;
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param fragmentId   The android:id of this fragment in its activity's layout.
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(int fragmentId, DrawerLayout drawerLayout) {
        mFragmentContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),
                mDrawerLayout,
                R.drawable.ic_drawer,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!isAdded()) return;

                getActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) return;

                getActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    private void selectListItem(int position, boolean closeDrawer) {
        mCurrentSelectedPosition = position;

        if (mDrawerListView != null)
            mDrawerListView.setItemChecked(position, true);

        if (mDrawerLayout != null && closeDrawer)
            mDrawerLayout.closeDrawer(mFragmentContainerView);

        if (mListsAdapter.getCount() > position) {
            Document document = (Document)mListsAdapter.getItem(position);
            Application application = (Application) getActivity().getApplication();
            application.setCurrentListId(document.getId());

            if (mCallbacks != null) {
                mCallbacks.onListSelected(document.getId());
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (ListSelectionCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_LIST_ID, mCurrentSelectedPosition);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // If the drawer is open, show the global app actions in the action bar. See also
        // showGlobalContextActionBar, which controls the top-left area of the action bar.
        if (mDrawerLayout != null && isDrawerOpen()) {
            inflater.inflate(R.menu.global, menu);
            showGlobalContextActionBar();
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Per the navigation drawer design guidelines, updates the action bar to show the global app
     * 'context', rather than just what's in the current screen.
     */
    private void showGlobalContextActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setTitle(R.string.app_name);
    }

    private ActionBar getActionBar() {
        return getActivity().getActionBar();
    }

    /**
     * Callbacks interface that all activities using this fragment must implement.
     */
    public static interface ListSelectionCallback {
        void onListSelected(String id);
    }

    private class ListsAdapter extends BaseAdapter {
        Context context;
        QueryEnumerator enumerator;

        public ListsAdapter(Context context, QueryEnumerator enumerator) {
            this.context = context;
            this.enumerator = enumerator;
        }

        @Override
        public int getCount() {
            return enumerator != null ? enumerator.getCount() : 0;
        }

        @Override
        public Object getItem(int i) {
            return enumerator.getRow(i).getDocument();
        }

        @Override
        public long getItemId(int i) {
            return enumerator.getRow(i).getSequenceNumber();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) parent.getContext().
                        getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.view_list_drawer, null);
            }

            Document document = (Document)getItem(position);
            TextView textView = (TextView) convertView;
            textView.setText((CharSequence) document.getProperty("title"));

            return convertView;
        }

        public void update(final QueryEnumerator enumerator) {
            ((Activity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ListsAdapter.this.enumerator = enumerator;
                    notifyDataSetChanged();
                }
            });
        }
    }
}
