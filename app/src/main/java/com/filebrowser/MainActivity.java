package com.filebrowser;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 * The main Activity for the app File Browser, which uses a ListView
 * to allow the user to navigate through directories in storage.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "FileBrowser";

    // An ID used when requesting the READ_EXTERNAL_STORAGE Permission.
    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;

    // A reference to the current file to open if
    // the app has to wait for a permission dialog callback.
    private File mFileToOpen;

    // The list of currently displayed Files.
    private List<File> mDisplayedFiles;

    // The back stack of opened directories.
    private Stack<File> mDirectoryBackStack = new Stack<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Start by opening the main External Storage directory.
        String storagePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        final File storageDir = new File(storagePath);
        openDirectory(storageDir);

        // Add handling to the ListView for when the user selects a list item.
        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Ignore user taps if there are no files listed on the screen,
                // Unless the problem is due to permissions, then request the permission.
                if (mDisplayedFiles == null) {
                    // Check whether the app is lacking necessary permissions.
                    int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.READ_EXTERNAL_STORAGE);
                    if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                        requestStoragePermission(storageDir);
                    }
                    return;
                }

                // If there are files on the screen, process the one the user selected.
                File file = mDisplayedFiles.get(position);
                if (file != null && file.isDirectory()) {
                    // Retrieve the *Visible* files in the selected directory.
                    File[] visibleFiles = file.listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File pathname) {
                            return !pathname.isHidden();
                        }
                    });

                    // If the directory has files, open it.
                    // Otherwise, show a message that it's empty.
                    if (visibleFiles.length > 0)
                        openDirectory(file);
                    else {
                        // Inform the user that the selected directory is empty.
                        mDisplayedFiles = null;
                        mDirectoryBackStack.push(file);
                        displayListViewMessage(getString(R.string.directory_is_empty));
                        // Update the displayed file path.
                        TextView filePathView = (TextView) findViewById(R.id.file_path_view);
                        filePathView.setText(file.getAbsolutePath());
                    }
                } else {
                    // Inform the user that they selected a file (not a directory),
                    // so it can't be opened.
                    displayToast(getResources().getString(R.string.file_selected_error));
                }
            }
        });
    }

    /**
     * Display the contents of a directory in the ListView.
     *
     * @param directory The directory to display
     */
    private void openDirectory(final File directory) {

        // If the directory is null, add an error message to
        // the ListView (i.e. if the user declined the permission dialog).
        if (directory == null) {
            displayListViewMessage(getString(R.string.error_reading_storage));
            return;
        }

        // Check whether the app has the necessary permissions, that
        // the storage is mounted, and that the File is a directory.
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            // The app has the necessary permissions.

            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state) ||
                    Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {

                if (directory.isDirectory() && directory.listFiles() != null) {
                    // Retrieve the list of *visible* files, sort them, and
                    // display them in the ListView.
                    File[] visibleFiles = directory.listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File pathname) {
                            return !pathname.isHidden();
                        }
                    });
                    mDisplayedFiles = Arrays.asList(visibleFiles);
                    Collections.sort(mDisplayedFiles);
                    FileListAdapter adapter = new FileListAdapter(this,
                            R.layout.file_list_item,
                            mDisplayedFiles);
                    ListView listView = (ListView) findViewById(R.id.listView);
                    listView.setAdapter(adapter);
                    mDirectoryBackStack.push(directory);
                    // Update the displayed file path.
                    TextView filePathView = (TextView) findViewById(R.id.file_path_view);
                    filePathView.setText(directory.getAbsolutePath());
                }
            }
        } else {
            // Request the permission from the Permission framework.
            requestStoragePermission(directory);
        }
    }

    /**
     * Display a Toast to the user.
     */
    private void displayToast(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        int verticalOffset = getResources()
                .getDisplayMetrics().heightPixels / 4;
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, verticalOffset);
        toast.show();
    }

    /**
     * Add a message to the user as a single ListView item.
     */
    private void displayListViewMessage(String message) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                new String[] {message});
        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(adapter);
    }

    /**
     * Handle the Back button.
     * If the app is inside of a directory, go up one level.
     * If the app is at the top level, show a prompt to confirm exiting the app.
     */
    @Override
    public void onBackPressed() {
        if (mDirectoryBackStack.size() > 1) {
            // Pop off the current directory.
            mDirectoryBackStack.pop();

            // Open the previous directory in the stack.
            openDirectory(mDirectoryBackStack.pop());
        } else {
            // If the app is at the top level, confirm with the user
            // whether they want to exit the app.
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle(R.string.exit_app_dialog_title)
                    .setMessage(R.string.exit_app_dialog_message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Exit the app.
                            finish();
                        }
                    });
            alertDialogBuilder.create().show();
        }
    }

    /**
     * Request the Storage permission.
     *
     * @param file The file to open if the permission is granted
     */
    private void requestStoragePermission(final File file) {

        // Check with the Permission framework to see
        // if a "Rationale" dialog should be shown first.
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)) {
            // Show rationale for needing the Storage permission.
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle(R.string.permission_rationale_title)
                    .setMessage(R.string.permission_rationale_message)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            // Request the permission.
                            mFileToOpen = file;
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                    PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                        }
                    });
            alertDialogBuilder.create().show();

        } else {
            // Request the Storage permission without Rationale.
            mFileToOpen = file;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }
    }

    /**
     * Handle the permission dialog result from the user.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If the request was canceled, the grantResults array will be empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted.
                    openDirectory(mFileToOpen);
                } else {
                    // Permission Denied.
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                    alertDialogBuilder.setTitle(R.string.permission_error_title)
                            .setMessage(R.string.permission_error_message)
                            .setPositiveButton(android.R.string.ok, null);
                    alertDialogBuilder.create().show();
                    openDirectory(null);
                    // Update the displayed file path.
                    TextView filePathView = (TextView) findViewById(R.id.file_path_view);
                    filePathView.setText("");
                }
            }
        }
    }
}