package org.amoradi.topoli;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class BackupItem {
    public String name;
    public String source;
    public String destination;

    public BackupItem() {}
    public BackupItem(String n, String src, String dest) {
        name = n;
        source = src;
        destination = dest;
    }
}

interface IBackupHandler {
    void addBackup(BackupItem bi);
    void runBackup(BackupItem bi);
}

interface IBackupItemClickHandler {
    void onBackupItemClick(int pos);
}

public class BackupActivity extends BaseActivity implements IBackupHandler {
    private static final String TAG = "BackupActivity";

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

        mBackupItems = getBackups();

        BackupListFragment f = new BackupListFragment();
        f.setBackupItems(mBackupItems);
        f.setBackupHandler(this);
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
        if (item.source.equals("") || item.name.equals("") || item.destination.equals("")) {
            return;
        }

        Toast.makeText(getApplicationContext(), "Adding new backup '" + item.source + "'", Toast.LENGTH_SHORT).show();

        BackupSyncOpenHelper dbHelper = new BackupSyncOpenHelper(getApplicationContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(BackupSyncSchema.COLUMN_TYPE, "backup");
        values.put(BackupSyncSchema.COLUMN_NAME, item.name);
        values.put(BackupSyncSchema.COLUMN_SOURCE, item.source);
        values.put(BackupSyncSchema.COLUMN_DESTINATION, item.destination);

        db.insert(BackupSyncSchema.TABLE_NAME, null, values);

        mBackupItems.clear();
        mBackupItems = getBackups();

        BackupListFragment f = new BackupListFragment();
        f.setBackupItems(mBackupItems);
        f.setBackupHandler(this);
        getFragmentManager().beginTransaction().replace(R.id.content_container, f).commit();
    }

    public List<BackupItem> getBackups() {
        List<BackupItem> bl = new ArrayList<>();

        BackupSyncOpenHelper dbHelper = new BackupSyncOpenHelper(getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] proj = {BackupSyncSchema.COLUMN_NAME,
                         BackupSyncSchema.COLUMN_SOURCE,
                         BackupSyncSchema.COLUMN_DESTINATION};

        Cursor c = db.query(
                BackupSyncSchema.TABLE_NAME,
                proj,
                "type = 'backup'",
                null,
                null,
                null,
                BackupSyncSchema.COLUMN_NAME + " DESC",
                null
        );

        if (c.getCount() <= 0) {
            c.close();
            return bl;
        }

        c.moveToFirst();

        do {
            BackupItem x = new BackupItem();
            x.name = c.getString(c.getColumnIndex(BackupSyncSchema.COLUMN_NAME));
            x.source = c.getString(c.getColumnIndex(BackupSyncSchema.COLUMN_SOURCE));
            x.destination = c.getString(c.getColumnIndex(BackupSyncSchema.COLUMN_DESTINATION));
            bl.add(x);
        } while(c.moveToNext());

        c.close();
        return bl;
    }

    public void runBackup(BackupItem b) {
        new RunBackupProcess().execute(b);
    }

    private class RunBackupProcess extends AsyncTask<BackupItem, Void, Integer> {
        @Override
        protected Integer doInBackground(BackupItem... bs) {
            BackupItem b = bs[0];

            try {
                // Executes the command.
                File f = new File(rsyncPath);

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                String rsync_username = prefs.getString(SettingsActivity.KEY_RSYNC_USERNAME, "");
                String rsync_options = prefs.getString(SettingsActivity.KEY_RSYNC_OPTIONS, "");
                String server_address = prefs.getString(SettingsActivity.KEY_SERVER_ADDRESS, "");
                String protocol = prefs.getString(SettingsActivity.KEY_PROTOCOL, "SSH");
                String private_key = prefs.getString(SettingsActivity.KEY_PRIVATE_KEY, "");
                String port = prefs.getString(SettingsActivity.KEY_PORT, "22");

                List<String> args = new ArrayList<>();

                args.add(f.getAbsolutePath());
                Collections.addAll(args, rsync_options.split(" "));

                if (protocol.equals("SSH")) {
                    args.add("-e");
                    args.add(sshPath + " -y -p " + port + " -i " + private_key);
                }

                args.add(b.source);
                args.add(rsync_username + "@" + server_address + ":" + b.destination);

                // DEBUG
                StringBuilder sb = new StringBuilder();
                for (String x : args) {
                    sb.append(x);
                    sb.append(" ");
                }

                Log.e(TAG, sb.toString());

                ProcessBuilder pb = new ProcessBuilder(args);
                pb.directory(getFilesDir());
                pb.redirectErrorStream(true);
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
                return process.exitValue();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void onPreExecute() {
            Toast.makeText(getApplicationContext(), "Running backup", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPostExecute(Integer res) {
            if (res != 0) {
                Toast.makeText(getApplicationContext(), "Error: Rsync returned " + res, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Backup completed", Toast.LENGTH_SHORT).show();
            }
        }
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

                t = (EditText) getView().findViewById(R.id.addbackupitem_name);
                i.name = t.getText().toString();

                mHandler.addBackup(i);
            }

            return super.onOptionsItemSelected(item);
        }
    }


    public static class BackupAdapter extends RecyclerView.Adapter<BackupAdapter.ViewHolder> implements IBackupItemClickHandler {
        List<BackupItem> mBackups;
        IBackupHandler mBackupHandler;

        public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            IBackupItemClickHandler mBackupClickHandler;

            public TextView mProfileTextView;
            public TextView mSrcTextView;

            public ViewHolder(View v, IBackupItemClickHandler handler) {
                super(v);
                v.setOnClickListener(this);
                mBackupClickHandler = handler;
                mProfileTextView = (TextView) v.findViewById(R.id.backup_item_profile_text);
                mSrcTextView = (TextView) v.findViewById(R.id.backup_item_source);
            }

            @Override
            public void onClick(View v) {
                mBackupClickHandler.onBackupItemClick(getAdapterPosition());
            }
        }

        public BackupAdapter(List<BackupItem> backups, IBackupHandler handler) {
            mBackups = backups;
            mBackupHandler = handler;
        }

        @Override
        public BackupAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.backup_item, parent, false);
            ViewHolder vh = new ViewHolder(v.findViewById(R.id.backup_item), this);
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int pos) {
            holder.mProfileTextView.setText(mBackups.get(pos).name);
            holder.mSrcTextView.setText(mBackups.get(pos).source);
        }

        @Override
        public int getItemCount() {
            return mBackups.size();
        }

        public void onBackupItemClick(int pos) {
            mBackupHandler.runBackup(mBackups.get(pos));
        }
    }

    public static class BackupListFragment extends Fragment {
        private List<BackupItem> mBackupItems;
        private IBackupHandler mBackupHandler;

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
            BackupAdapter mAdapter = new BackupAdapter(mBackupItems, mBackupHandler);
            mRecyclerView.setAdapter(mAdapter);
            mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
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

        public void setBackupHandler(IBackupHandler handler) {
            mBackupHandler = handler;
        }
    }
}
