package com.filebrowser;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.List;

/**
 * An ArrayAdapter for displaying File objects with a custom layout.
 */
public class FileListAdapter extends ArrayAdapter<File> {

    // The list of Files currently being displayed.
    private List<File> mFiles;

    public FileListAdapter(Context context, int resource, List<File> objects) {
        super(context, resource, objects);
        mFiles = objects;
    }

    /**
     * Create the View for a single item in the list.
     */
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.file_list_item, parent, false);
        }

        // Set the name of the file.
        File file = mFiles.get(position);
        TextView nameTextView = (TextView) convertView.findViewById(R.id.name_text);
        nameTextView.setText(file.getName());

        // Add a folder icon next to directories that can be opened.
        ImageView folderIcon = (ImageView) convertView.findViewById(R.id.icon_view);
        if (file.isDirectory()) {
            folderIcon.setImageResource(R.drawable.ic_folder_open_black_24dp);
            folderIcon.setVisibility(View.VISIBLE);
        } else
            folderIcon.setVisibility(View.INVISIBLE);

        return convertView;
    }
}
