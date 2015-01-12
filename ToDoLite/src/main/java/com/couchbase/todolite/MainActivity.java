package com.couchbase.todolite;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.widget.DrawerLayout;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.lite.Attachment;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.util.Log;
import com.couchbase.todolite.document.List;
import com.couchbase.todolite.document.Profile;
import com.couchbase.todolite.document.Task;
import com.couchbase.todolite.helper.ImageHelper;
import com.couchbase.todolite.helper.LiveQueryAdapter;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;

import org.codehaus.jackson.map.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.StringTokenizer;

public class MainActivity extends Activity
        implements ListDrawerFragment.ListSelectionCallback {
    private ListDrawerFragment mDrawerFragment;

    private CharSequence mTitle;

    private Session.StatusCallback statusCallback = new FacebookSessionStatusCallback();

    private static final String TAG = Application.TAG;

    private Database getDatabase() {
        Application application = (Application) getApplication();
        return application.getDatabase();
    }

    private String getCurrentListId() {
        Application application = (Application) getApplication();
        String currentListId = application.getCurrentListId();
        if (currentListId == null) {
            try {
                QueryEnumerator enumerator = List.getQuery(getDatabase()).run();
                if (enumerator.getCount() > 0) {
                    currentListId = enumerator.getRow(0).getDocument().getId();
                }
            } catch (CouchbaseLiteException e) { }
        }
        return currentListId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(Application.TAG, "MainActivity State: onCreate()");

        Application application = (Application) getApplication();

        if (application.getCurrentUserId() != null && application.getCurrentUserPassword() != null) { // basic auth
            application.setDatabaseForName(application.getCurrentUserId());
            application.startReplicationSyncWithBasicAuth(application.getCurrentUserId(), application.getCurrentUserPassword());
        } else if (application.getLastReceivedFbAccessToken() != null) { // fb auth
            application.setDatabaseForName(application.getCurrentUserId());
            application.startReplicationSyncWithFacebookLogin(application.getLastReceivedFbAccessToken());
        } else if (application.getCurrentUserId() != null) { // cookie auth
            application.setDatabaseForName(application.getCurrentUserId());
            application.startReplicationSyncWithCustomCookie(application.getCurrentUserId());
        } else if (application.getGuestBoolean()) {
            application.setDatabaseForGuest();
        } else {
            Intent i = new Intent(MainActivity.this, LoginActivity.class);
            startActivityForResult(i, 0);
            finish();
        }

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);

        mDrawerFragment = (ListDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        mDrawerFragment.setUp(R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        String currentListId = getCurrentListId();
        if (currentListId != null) {
//            displayListContent(currentListId);
        }

        application.getOnSyncProgressChangeObservable().addObserver(new Observer() {
            @Override
            public void update(final Observable observable, final Object data) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Application.SyncProgress progress = (Application.SyncProgress) data;
                        Log.d(TAG, "Sync progress changed.  Completed: %d Total: %d Status: %s", progress.completedCount, progress.totalCount, progress.status);

                        if (progress.status == Replication.ReplicationStatus.REPLICATION_ACTIVE) {
                            Log.d(TAG, "Turn on progress spinny");
                            setProgressBarIndeterminateVisibility(true);
                        } else {
                            Log.d(TAG, "Turn off progress spinny");
                            setProgressBarIndeterminateVisibility(false);
                        }
                    }
                });
            }
        });
        application.getOnSyncUnauthorizedObservable().addObserver(new Observer() {
            @Override
            public void update(Observable observable, Object data) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Log.d(Application.TAG, "OnSyncUnauthorizedObservable called, show toast");

                        // clear the saved user id, since our session is no longer valid
                        // and we want to show the login button
                        Application application = (Application) getApplication();
                        application.setCurrentUserId(null);
                        application.setCurrentUserPassword(null);
                        invalidateOptionsMenu();

                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();

                        String msg = "Sync unable to continue due to invalid session/login";
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();

                    }
                });

            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(Application.TAG, "MainActivity State: onStart()");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(Application.TAG, "MainActivity State: onRestart()");

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(Application.TAG, "MainActivity State: onResume()");

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Log.d(Application.TAG, "MainActivity State: onPostResume()");

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(Application.TAG, "MainActivity State: onPause()");

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(Application.TAG, "MainActivity State: onStop()");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(Application.TAG, "MainActivity State: onDestroy()");

    }


    private void displayListContent(String listDocId) {
        Document document = getDatabase().getDocument(listDocId);
        getActionBar().setSubtitle((String)document.getProperty("title"));

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, TasksFragment.newInstance(listDocId))
                .commit();

        Application application = (Application)getApplication();
        application.setCurrentListId(listDocId);
    }

    @Override
    public void onListSelected(String id) {
        displayListContent(id);
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mDrawerFragment.isDrawerOpen()) {
            getMenuInflater().inflate(R.menu.main, menu);

            // Add Login button if the user has not been logged in.
            Application application = (Application) getApplication();

            // Add Share button if the user has been logged in
            if (application.getCurrentUserId() != null && getCurrentListId() != null) {
                MenuItem shareMenuItem = menu.add(getResources().getString(R.string.action_share));
                shareMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                shareMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        Intent intent = new Intent(MainActivity.this, ShareActivity.class);
                        intent.putExtra(ShareActivity.SHARE_ACTIVITY_CURRENT_LIST_ID_EXTRA,
                                getCurrentListId());
                        MainActivity.this.startActivity(intent);
                        return true;
                    }
                });
                MenuItem logoutMenuItem = menu.add("Logout");
                logoutMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                logoutMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        Application application = (Application) getApplication();
                        application.logoutUser();
                        invalidateOptionsMenu();
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                        return true;
                    }
                });
            } else if (application.getGuestBoolean()) {
                MenuItem loginMenuItem = menu.add("Login");
                loginMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                loginMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        Intent i = new Intent(MainActivity.this, LoginActivity.class);
                        startActivityForResult(i, 0);
                        return true;
                    }
                });
            }
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    private void createNewList() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getResources().getString(R.string.title_dialog_new_list));

        final EditText input = new EditText(this);
        input.setMaxLines(1);
        input.setSingleLine(true);
        input.setHint(getResources().getString(R.string.hint_new_list));
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String title = input.getText().toString();
                if (title.length() == 0) {
                    // TODO: Show an error message.
                    return;
                }
                try {
                    String currentUserId = ((Application)getApplication()).getCurrentUserId();
                    Document document = List.createNewList(getDatabase(), title, currentUserId);
                    displayListContent(document.getId());
                    invalidateOptionsMenu();
                } catch (CouchbaseLiteException e) {
                    Log.e(Application.TAG, "Cannot create a new list", e);
                }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) { }
        });

        alert.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_new_list) {
            createNewList();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
    }

    private void startSyncWithCustomCookie(String cookieVal) {
        Application application = (Application) MainActivity.this.getApplication();
        application.startReplicationSyncWithCustomCookie(cookieVal);
    }

    private void startSyncWithStoredCustomCookie() {
        Application application = (Application) MainActivity.this.getApplication();
        application.startReplicationSyncWithStoredCustomCookie();
    }

    private void startSyncWithStoredBasicAuth() {
        Application application = (Application) MainActivity.this.getApplication();
        application.startReplicationSyncWithStoredBasicAuth();
    }

    private void startSyncWithBasicAuth(String username, String password) {
        Application application = (Application) MainActivity.this.getApplication();
        application.startReplicationSyncWithBasicAuth(username, password);
    }


    private class FacebookSessionStatusCallback implements Session.StatusCallback {
        @Override
        public void call(final Session session, SessionState state, Exception exception) {

            Log.d(TAG, "FacebookSessionStatusCallback called.  Session: %s State: %s Exception: %s", session, state, exception);

            if (exception != null) {
                String msg = String.format("Login failed!  Exception: %s", exception.getMessage());
                Log.e(TAG, msg);
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                return;
            }

            if (session == null || !session.isOpened()) {
                Log.d(TAG, "FacebookSessionStatusCallback called, but session is null");
                return;
            }

            if (!session.isOpened()) {
                Log.d(TAG, "FacebookSessionStatusCallback called, but session is not opened");
                return;
            }

            Request.newMeRequest(session, new Request.GraphUserCallback() {
                @Override
                public void onCompleted(GraphUser user, Response response) {
                    if (user != null) {
                        String userId = (String) user.getId();
                        String name = (String) user.getName();

                        Application application = (Application) MainActivity.this.getApplication();
                        String currentUserId = application.getCurrentUserId();
                        if (currentUserId != null && !currentUserId.equals(userId)) {
                            //TODO: Update Database and all UIs
                        }

                        Document profile = null;
                        try {
                            profile = Profile.getUserProfileById(getDatabase(), userId);
                            if (profile == null)
                                profile = Profile.createProfile(getDatabase(), userId, name);
                        } catch (CouchbaseLiteException e) { }

                        try {
                            List.assignOwnerToListsIfNeeded(getDatabase(), profile);
                        } catch (CouchbaseLiteException e) { }

                        application.setCurrentUserId(userId);
                        application.setLastReceivedFbAccessToken(session.getAccessToken());

                        Toast.makeText(MainActivity.this, "Login successful!  Starting sync.", Toast.LENGTH_LONG).show();

                        application.startReplicationSyncWithFacebookLogin(
                                session.getAccessToken());

                        invalidateOptionsMenu();
                    }
                }
            }).executeAsync();
        }
    }

    public static class TasksFragment extends Fragment {
        private static final String ARG_LIST_DOC_ID = "id";

        private static final int REQUEST_TAKE_PHOTO = 1;
        private static final int REQUEST_CHOOSE_PHOTO = 2;

        private static final int THUMBNAIL_SIZE_PX = 150;

        private TaskAdapter mAdapter;
        private String mImagePathToBeAttached;
        private Bitmap mImageToBeAttached;
        private Document mCurrentTaskToAttachImage;

        public static TasksFragment newInstance(String id) {
            TasksFragment fragment = new TasksFragment();

            Bundle args = new Bundle();
            args.putString(ARG_LIST_DOC_ID, id);
            fragment.setArguments(args);

            return fragment;
        }

        public TasksFragment() { }

        private Database getDatabase() {
            Application application = (Application) getActivity().getApplication();
            return application.getDatabase();
        }

        private File createImageFile() throws IOException {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "TODO_LITE_" + timeStamp + "_";
            File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File image = File.createTempFile(fileName, ".jpg", storageDir);
            mImagePathToBeAttached = image.getAbsolutePath();
            return image;
        }

        private void dispatchTakePhotoIntent() {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException e) {
                    Log.e(Application.TAG, "Cannot create a temp image file", e);
                }

                if (photoFile != null) {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                }
            }
        }

        private void dispatchChoosePhotoIntent() {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select File"),
                    REQUEST_CHOOSE_PHOTO);
        }

        private void deleteCurrentPhoto() {
            if (mImageToBeAttached != null) {
                mImageToBeAttached.recycle();
                mImageToBeAttached = null;

                ViewGroup createTaskPanel = (ViewGroup) getActivity().findViewById(
                        R.id.create_task);
                ImageView imageView = (ImageView) createTaskPanel.findViewById(R.id.image);
                imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_camera));
            }
        }

        private void attachImage(final Document task) {
            CharSequence[] items;
            if (mImageToBeAttached != null)
                items = new CharSequence[] { "Take photo", "Choose photo", "Delete photo" };
            else
                items = new CharSequence[] { "Take photo", "Choose photo" };

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Add picture");
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int item) {
                    if (item == 0) {
                        mCurrentTaskToAttachImage = task;
                        dispatchTakePhotoIntent();
                    } else if (item == 1) {
                        mCurrentTaskToAttachImage = task;
                        dispatchChoosePhotoIntent();
                    } else {
                        deleteCurrentPhoto();
                    }
                }
            });
            builder.show();
        }

        private void dispatchImageViewIntent(Bitmap image) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, 50, stream);
            byte[] byteArray = stream.toByteArray();

            long l = byteArray.length;

            Intent intent = new Intent(getActivity(), ImageViewActivity.class);
            intent.putExtra(ImageViewActivity.INTENT_IMAGE, byteArray);
            startActivity(intent);
        }

        private void deleteTask(int position) {
            Document task = (Document) mAdapter.getItem(position);
            try {
                Task.deleteTask(task);
            } catch (CouchbaseLiteException e) {
                Log.e(Application.TAG, "Cannot delete a task", e);
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final ListView listView = (ListView) inflater.inflate(
                    R.layout.fragment_main, container, false);

            final String listId = getArguments().getString(ARG_LIST_DOC_ID);

            ViewGroup header = (ViewGroup) inflater.inflate(
                    R.layout.view_task_create, listView, false);

            final ImageView imageView = (ImageView) header.findViewById(R.id.image);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    attachImage(null);
                }
            });

            final EditText text = (EditText) header.findViewById(R.id.text);
            text.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View view, int i, KeyEvent keyEvent) {
                    if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN) &&
                            (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                        String inputText = text.getText().toString();
                        if (inputText.length() > 0) {
                            try {
                                Task.createTask(getDatabase(), inputText, mImageToBeAttached, listId);
                            } catch (CouchbaseLiteException e) {
                                Log.e(Application.TAG, "Cannot create new task", e);
                            }
                        }

                        // Reset text and current selected photo if available.
                        text.setText("");
                        deleteCurrentPhoto();

                        return true;
                    }
                    return false;
                }
            });

            listView.addHeaderView(header);

            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, final int position,
                                               long id) {
                    PopupMenu popup = new PopupMenu(getActivity(), view);
                    popup.getMenu().add(getResources().getString(R.string.action_delete));
                    popup.getMenu().add(getResources().getString(R.string.action_show_document));
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if (item.getTitle().equals(getResources().getString(R.string.action_delete))) {
                                deleteTask(position - 1);
                            } else if (item.getTitle().equals(getResources().getString(R.string.action_show_document))) {
                                Document task = (Document) mAdapter.getItem(position - 1);
                                System.out.println("Doc id: " + task.getId());
                                Map<String, Object> documentMap = task.getProperties();
                                ObjectMapper objectMapper = new ObjectMapper();
                                try {
                                    String documentString = objectMapper.writeValueAsString(documentMap);
                                    Toast.makeText(getActivity(), documentString, Toast.LENGTH_LONG).show();
                                } catch (IOException e) {
                                    Toast.makeText(getActivity(), "Error showing document", Toast.LENGTH_LONG).show();
                                    Log.d(Application.TAG, "Error showing document", e);
                                }

                            }
                            return true;
                        }
                    });


                    popup.show();
                    return true;
                }
            });

            LiveQuery query = Task.getQuery(getDatabase(), listId).toLiveQuery();
            mAdapter = new TaskAdapter(getActivity(), query);
            listView.setAdapter(mAdapter);

            return listView;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (resultCode != RESULT_OK) {
                if (mCurrentTaskToAttachImage != null) {
                    mCurrentTaskToAttachImage = null;
                }
                return;
            }

            Bitmap thumbnail = null;
            if (requestCode == REQUEST_TAKE_PHOTO) {
                mImageToBeAttached = BitmapFactory.decodeFile(mImagePathToBeAttached);
                if (mCurrentTaskToAttachImage == null) {
                    thumbnail = ImageHelper.thumbmailFromFile(mImagePathToBeAttached,
                            THUMBNAIL_SIZE_PX, THUMBNAIL_SIZE_PX);
                }

                // Delete the temporary image file
                File file = new File(mImagePathToBeAttached);
                file.delete();
                mImagePathToBeAttached = null;
            } else if (requestCode == REQUEST_CHOOSE_PHOTO) {
                try {
                    Uri uri = data.getData();
                    mImageToBeAttached = MediaStore.Images.Media.getBitmap(
                            getActivity().getContentResolver(), uri);
                    if (mCurrentTaskToAttachImage == null) {
                        AssetFileDescriptor descriptor =
                                getActivity().getContentResolver().openAssetFileDescriptor(uri, "r");
                        thumbnail = ImageHelper.thumbmailFromDescriptor(descriptor.getFileDescriptor(),
                                THUMBNAIL_SIZE_PX, THUMBNAIL_SIZE_PX);
                    }
                } catch (IOException e) {
                    Log.e(Application.TAG, "Cannot get a selected photo from the gallery.", e);
                }
            }

            if (mImageToBeAttached != null) {
                if (mCurrentTaskToAttachImage != null) {
                    try {
                        Task.attachImage(mCurrentTaskToAttachImage, mImageToBeAttached);
                        mImageToBeAttached = null;
                    } catch (CouchbaseLiteException e) {
                        Log.e(Application.TAG, "Cannot attach an image to a task.", e);
                    }
                } else { // Attach an image for a new task
                    ImageView imageView = (ImageView) getActivity().findViewById(R.id.image);
                    imageView.setImageBitmap(thumbnail);
                }
            }

            // Ensure resetting the task to attach an image
            if (mCurrentTaskToAttachImage != null) {
                mCurrentTaskToAttachImage = null;
            }
        }

        private class TaskAdapter extends LiveQueryAdapter {
            public TaskAdapter(Context context, LiveQuery query) {
                super(context, query);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    LayoutInflater inflater = (LayoutInflater) parent.getContext().
                            getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    convertView = inflater.inflate(R.layout.view_task, null);
                }

                final Document task = (Document) getItem(position);

                if (task == null || task.getCurrentRevision() == null) {
                    return convertView;
                }

                Bitmap thumbnail = null;
                java.util.List<Attachment> attachments = task.getCurrentRevision().getAttachments();
                if (attachments != null && attachments.size() > 0) {
                    Attachment attachment = attachments.get(0);
                    try {
                        final BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeStream(attachment.getContent(), null, options);
                        options.inSampleSize = ImageHelper.calculateInSampleSize(
                                options, THUMBNAIL_SIZE_PX, THUMBNAIL_SIZE_PX);
                        attachment.getContent().close();

                        // Need to get a new attachment again as the FileInputStream
                        // doesn't support mark and reset.
                        attachments = task.getCurrentRevision().getAttachments();
                        attachment = attachments.get(0);
                        options.inJustDecodeBounds = false;
                        Bitmap bitmap = BitmapFactory.decodeStream(attachment.getContent(), null, options);
                        int w = bitmap.getWidth();
                        int h = bitmap.getHeight();
                        thumbnail = ThumbnailUtils.extractThumbnail(bitmap, THUMBNAIL_SIZE_PX, THUMBNAIL_SIZE_PX);
                        attachment.getContent().close();
                    } catch (Exception e) {
                        Log.e(Application.TAG, "Cannot decode the attached image", e);
                    }
                }

                ImageView imageView = (ImageView) convertView.findViewById(R.id.image);
                if (thumbnail != null) {
                    imageView.setImageBitmap(thumbnail);
                } else {
                    imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_camera_light));
                }

                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        java.util.List<Attachment> attachments =
                                task.getCurrentRevision().getAttachments();
                        if (attachments != null && attachments.size() > 0) {
                            Attachment attachment = attachments.get(0);
                            try {
                                Bitmap displayImage = BitmapFactory.decodeStream(attachment.getContent());
                                dispatchImageViewIntent(displayImage);
                                attachment.getContent().close();
                            } catch (Exception e) {
                                Log.e(Application.TAG, "Cannot decode the attached image", e);
                            }
                        } else {
                            attachImage(task);
                        }
                    }
                });

                TextView text = (TextView) convertView.findViewById(R.id.text);
                text.setText((String) task.getProperty("title"));

                final CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checked);
                Boolean checkedProperty = (Boolean) task.getProperty("checked");
                boolean checked = checkedProperty != null ? checkedProperty.booleanValue() : false;
                checkBox.setChecked(checked);
                checkBox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            Task.updateCheckedStatus(task, checkBox.isChecked());
                        } catch (CouchbaseLiteException e) {
                            Log.e(Application.TAG, "Cannot update checked status", e);
                        }
                    }
                });

                return convertView;
            }
        }
    }
}
