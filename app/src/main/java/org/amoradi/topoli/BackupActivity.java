package org.amoradi.topoli;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


class BackupItem {
    public String source;
    public String destination;
}

interface IBackupHandler {
    void addBackup(BackupItem bi);
}

public class BackupActivity extends BaseActivity implements IBackupHandler {
    private static final String TAG = "BackupActivity";

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;

    List<BackupItem> mBackupItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);
        setNavDrawerSelected(R.id.nav_backup);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.e(TAG, "replacing fragment");
                    AddBackupItemFragment f = new AddBackupItemFragment();
                    getFragmentManager().beginTransaction().replace(R.id.content_container, f).commit();
                }
            });
        }

        mBackupItems = new ArrayList<>();



        BackupListFragment f = new BackupListFragment();
        f.setBackupItems(mBackupItems);
        getFragmentManager().beginTransaction().replace(R.id.content_container, f).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.backup, menu);
        menu.findItem(R.id.action_done).setVisible(false);
        menu.findItem(R.id.action_refresh).setVisible(true);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            Toast.makeText(getApplicationContext(), "Running all backups", Toast.LENGTH_SHORT).show();
        }

        return super.onOptionsItemSelected(item);
    }

    public void addBackup(BackupItem item) {
        Toast.makeText(getApplicationContext(), "Adding new backup '" + item.source + "'", Toast.LENGTH_SHORT).show();
        mBackupItems.add(item);
        BackupListFragment f = new BackupListFragment();
        f.setBackupItems(mBackupItems);
        getFragmentManager().beginTransaction().replace(R.id.content_container, f).commit();
    }

    public void runBackup(String origin, String dest) {

            // Executes the command.
            File f = new File(rsyncPath);
            if (!f.exists()) {
                Log.v(TAG, "WTH?");
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String rsync_username = prefs.getString(SettingsActivity.KEY_RSYNC_USERNAME, "");
            String rsync_options = prefs.getString(SettingsActivity.KEY_RSYNC_OPTIONS, "");
            String server_address = prefs.getString(SettingsActivity.KEY_SERVER_ADDRESS, "");
            String protocol = prefs.getString(SettingsActivity.KEY_PROTOCOL, "Rsync");
            String private_key = prefs.getString(SettingsActivity.KEY_PRIVATE_KEY, "");
            Integer port = prefs.getInt(SettingsActivity.KEY_PORT, 22);

            List<String> args = new ArrayList<>();
            args.add(f.getAbsolutePath());
            args.add(rsync_options);

            if (protocol.equals("SSH")) {
                args.add("-e \"ssh -y -p " + port + " '" + private_key + "'\"");
            }

            args.add(origin);
            args.add(rsync_username + "@" + server_address + ":" + dest);

            // DEBUG
            StringBuilder sb = new StringBuilder();
            for (String x : args) {
                sb.append(x);
                sb.append(" ");
            }

            Log.e(TAG, sb.toString());

            /*
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.directory(getFilesDir());
            Process process = pb.start();


            // Reads stdout.
            // NOTE: You can write to stdin of the command using
            //       process.getOutputStream().

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            while ((read = reader.read(buffer)) > 0) {
                StringBuffer output = new StringBuffer();
                output.append(buffer, 0, read);
                Log.e(TAG, output.toString());
            }
            reader.close();


            // Waits for the command to finish.
            process.waitFor();

            if (process.exitValue() != 0) {
                Toast.makeText(getApplicationContext(), "Error: rsync returned " + process.exitValue(), Toast.LENGTH_SHORT).show();
            }
            */

    }

    public static class AddBackupItemFragment extends Fragment {
        IBackupHandler mHandler;

        @Override
        public void onAttach(Activity acc) {
            super.onAttach(acc);
            mHandler = (IBackupHandler) acc;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_addbackupitem, container, false);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            super.onCreateOptionsMenu(menu, inflater);
            menu.findItem(R.id.action_done).setVisible(true);
            menu.findItem(R.id.action_refresh).setVisible(false);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();

            if (id == R.id.action_done) {
                BackupItem i = new BackupItem();

                EditText t = (EditText) getView().findViewById(R.id.addbackupitem_source);
                i.source = t.getText().toString();

                t = (EditText) getView().findViewById(R.id.addbackupitem_destination);
                i.destination = t.getText().toString();

                mHandler.addBackup(i);
            }

            return super.onOptionsItemSelected(item);
        }
    }


    public static class BackupAdapter extends RecyclerView.Adapter<BackupAdapter.ViewHolder> {
        List<BackupItem> mBackups;

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public TextView mTextView;
            public ViewHolder(View v) {
                super(v);
                mTextView = (TextView) v.findViewById(R.id.backup_text);
            }
        }

        public BackupAdapter(List<BackupItem> backups) {
            mBackups = backups;
        }

        @Override
        public BackupAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.backup_item, parent, false);
            ViewHolder vh = new ViewHolder(v.findViewById(R.id.backup_item));
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int pos) {
            holder.mTextView.setText(mBackups.get(pos).source);
        }

        @Override
        public int getItemCount() {
            return mBackups.size();
        }
    }

    public static class BackupListFragment extends Fragment {
        private List<BackupItem> mBackupItems;

        public void setBackupItems(List<BackupItem> b) {
            mBackupItems = b;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_backuplist, container, false);
            RecyclerView mRecyclerView = (RecyclerView) v.findViewById(R.id.recyclerview_backup);
            mRecyclerView.setHasFixedSize(true);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
            BackupAdapter mAdapter = new BackupAdapter(mBackupItems);
            mRecyclerView.setAdapter(mAdapter);
            return v;
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            super.onCreateOptionsMenu(menu, inflater);
            menu.findItem(R.id.action_done).setVisible(false);
            menu.findItem(R.id.action_refresh).setVisible(true);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            return super.onOptionsItemSelected(item);
        }
    }
}
