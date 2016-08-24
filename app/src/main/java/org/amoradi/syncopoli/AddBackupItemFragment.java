package org.amoradi.syncopoli;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;

public class AddBackupItemFragment extends Fragment {
    IBackupHandler mHandler;
    BackupItem mBackup = null;

    @Override
    public void onAttach(Activity acc) {
        super.onAttach(acc);
        mHandler = (IBackupHandler) acc;
    }

    public void setBackupContent(BackupItem b) {
        mBackup = b;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_addbackupitem, container, false);

        if (mBackup == null) {
            return v;
        }

        Spinner v_dir = (Spinner) v.findViewById(R.id.addbackupitem_direction);
        if (mBackup.direction == BackupItem.Direction.OUTGOING) {
            v_dir.setSelection(1);
        } else {
            v_dir.setSelection(0);
        }

        TextInputEditText v_name = (TextInputEditText) v.findViewById(R.id.addbackupitem_name);
        v_name.setText(mBackup.name);

        TextInputEditText v_src = (TextInputEditText) v.findViewById(R.id.addbackupitem_source);
        v_src.setText(mBackup.source);

        TextInputEditText v_dst = (TextInputEditText) v.findViewById(R.id.addbackupitem_destination);
        v_dst.setText(mBackup.destination);

        TextInputEditText v_opts = (TextInputEditText) v.findViewById(R.id.addbackupitem_rsync_options);
        v_opts.setText(mBackup.rsync_options);

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.findItem(R.id.action_done).setVisible(true);
        menu.findItem(R.id.action_refresh).setVisible(false);
        menu.findItem(R.id.action_run).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_done) {
            BackupItem i = new BackupItem();

            View v = getView();

            EditText t = (EditText) v.findViewById(R.id.addbackupitem_source);
            i.source = t.getText().toString();

            t = (EditText) v.findViewById(R.id.addbackupitem_destination);
            i.destination = t.getText().toString();

            t = (EditText) v.findViewById(R.id.addbackupitem_name);
            i.name = t.getText().toString();

            t = (EditText) v.findViewById(R.id.addbackupitem_rsync_options);
            i.rsync_options = t.getText().toString();

            Spinner s = (Spinner) v.findViewById(R.id.addbackupitem_direction);
            String dir = s.getSelectedItem().toString();

            if (dir.equals("Remote to local")) {
                i.direction = BackupItem.Direction.INCOMING;
            } else {
                i.direction = BackupItem.Direction.OUTGOING;
            }

            if (mBackup == null) {
                mHandler.addBackup(i);
            } else {
                mHandler.updateBackup(i);
            }
        } else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }
}