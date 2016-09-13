package org.amoradi.syncopoli;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ScrollView;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class BackupLogFragment extends Fragment {
    private BackupItem mBackupItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    public void setBackupItem(BackupItem b) {
        mBackupItem = b;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_backuplog, container, false);
        if (mBackupItem != null) {
            ((TextView) v.findViewById(R.id.backuplog_textview)).setText(getLogString(mBackupItem.logFileName));
        } else {
            ((TextView) v.findViewById(R.id.backuplog_textview)).setText("mBackupItem is null");
        }

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.findItem(R.id.action_done).setVisible(false);
        menu.findItem(R.id.action_refresh).setVisible(true);
        menu.findItem(R.id.action_run).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            ((TextView) getView().findViewById(R.id.backuplog_textview)).setText(getLogString(mBackupItem.logFileName));
            ((ScrollView) getView().findViewById(R.id.backuplog_scrollview)).fullScroll(View.FOCUS_DOWN);
        } else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }

    public String getLogString(String filename) {
        try {
            FileInputStream ins = getActivity().getApplicationContext().openFileInput(filename);

            BufferedReader reader = new BufferedReader(new InputStreamReader(ins));
            char[] buffer = new char[4096];

            StringBuilder output = new StringBuilder();
            while (reader.read(buffer) > 0) {
                output.append(new String(buffer));
            }
            reader.close();
            ins.close();

            return output.toString();
        } catch (FileNotFoundException e) {
            return "Log file not found.";
        } catch (IOException e) {
            e.printStackTrace();
            return "An error occurred while trying to read log file.";
        }
    }
}
