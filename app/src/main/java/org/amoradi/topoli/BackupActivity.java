package org.amoradi.topoli;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

interface IBackupItemClickHandler {
    void onBackupShowLog(int pos);
}

public class BackupActivity extends BaseActivity implements IBackupHandler {
    private static final String TAG = "BackupActivity";
    private static final String SYNC_AUTHORITY = "org.amoradi.topoli.provider";
    private static final String SYNC_ACCOUNT_NAME = "Topoli Sync Account";
    private static final String SYNC_ACCOUNT_TYPE = "org.amoradi.topoli.sync";
    Account mAccount;
    BackupHandler mBackupHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);
        setNavDrawerSelected(R.id.nav_backup);

        mAccount = createSyncAccount(this);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        long freq = Long.parseLong(prefs.getString(SettingsActivity.KEY_FREQUENCY, "8"));
        freq = freq * 3600; // hours to seconds

        ContentResolver.addPeriodicSync(mAccount, SYNC_AUTHORITY, new Bundle(), freq);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.e(TAG, "replacing fragment");
                    AddBackupItemFragment f = new AddBackupItemFragment();
                    getFragmentManager().beginTransaction().replace(R.id.content_container, f).addToBackStack(null).commit();
                }
            });
        }

        mBackupHandler = new BackupHandler(this);

        BackupListFragment f = new BackupListFragment();
        f.setBackupHandler(this);
        getFragmentManager().beginTransaction().replace(R.id.content_container, f).commit();
    }

    public static Account createSyncAccount(Context ctx) {
        Account acc = new Account(SYNC_ACCOUNT_NAME, SYNC_ACCOUNT_TYPE);
        AccountManager accman = AccountManager.get(ctx);

        if (accman.addAccountExplicitly(acc, null, null)) {
            ContentResolver.setIsSyncable(acc, SYNC_AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(acc, SYNC_AUTHORITY, true);
        }

        return acc;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.backup, menu);
        menu.findItem(R.id.action_done).setVisible(false);
        menu.findItem(R.id.action_refresh).setVisible(true);
        menu.findItem(R.id.action_run).setVisible(true);

        return true;
    }

    public void syncBackups() {
        Toast.makeText(this, "Syncing", Toast.LENGTH_SHORT).show();
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(mAccount, SYNC_AUTHORITY, settingsBundle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_run) {
            Toast.makeText(getApplicationContext(), "Running all tasks", Toast.LENGTH_SHORT).show();
            syncBackups();
        } else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }

    public void addBackup(BackupItem item) {
        mBackupHandler.addBackup(item);
        BackupListFragment f = new BackupListFragment();
        f.setBackupHandler(this);
        getFragmentManager().beginTransaction().replace(R.id.content_container, f).addToBackStack(null).commit();
    }

    public void updateBackupList() {
        mBackupHandler.updateBackupList();
    }

    public List<BackupItem> getBackups() {
        return mBackupHandler.getBackups();
    }

    public void updateBackupTimestamp(BackupItem b) {
        mBackupHandler.updateBackupTimestamp(b);
    }

    public int runBackup(BackupItem b) {
        syncBackups();
        return 0;
    }

    public void showLog(BackupItem b) {
        BackupLogFragment f = new BackupLogFragment();
        f.setBackupItem(b);
        getFragmentManager().beginTransaction().replace(R.id.content_container, f).addToBackStack(null).commit();
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
            menu.findItem(R.id.action_run).setVisible(false);
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
            } else {
                return super.onOptionsItemSelected(item);
            }

            return true;
        }
    }

    public static class BackupLogFragment extends Fragment {
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

    public static class BackupAdapter extends RecyclerView.Adapter<BackupAdapter.ViewHolder> implements IBackupItemClickHandler {
        IBackupHandler mBackupHandler;

        public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            IBackupItemClickHandler mBackupClickHandler;

            public TextView mProfileTextView;
            public TextView mSrcTextView;

            public ViewHolder(View v, IBackupItemClickHandler handler) {
                super(v);
                LinearLayout l = (LinearLayout) v.findViewById(R.id.backup_item_info);
                l.setOnClickListener(this);

                mBackupClickHandler = handler;
                mProfileTextView = (TextView) v.findViewById(R.id.backup_item_profile_text);
                mSrcTextView = (TextView) v.findViewById(R.id.backup_item_source);
            }

            @Override
            public void onClick(View v) {
                if (v instanceof LinearLayout) {
                    mBackupClickHandler.onBackupShowLog(getAdapterPosition());
                }
            }
        }

        public BackupAdapter(IBackupHandler handler) {
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
            holder.mProfileTextView.setText(mBackupHandler.getBackups().get(pos).name);

            if (mBackupHandler.getBackups().get(pos).lastUpdate == null) {
                holder.mSrcTextView.setText("This backup has never run");
            } else {
                holder.mSrcTextView.setText("Last update: " + mBackupHandler.getBackups().get(pos).lastUpdate.toString());
            }
        }

        @Override
        public int getItemCount() {
            return mBackupHandler.getBackups().size();
        }

        public void onBackupRun(int pos) {
        }
        public void onBackupShowLog(int pos) {
            mBackupHandler.showLog(mBackupHandler.getBackups().get(pos));
        }
    }

    public static class BackupListFragment extends Fragment {
        private List<BackupItem> mBackupItems;
        private IBackupHandler mBackupHandler;
        private BackupAdapter mAdapter;

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
            mAdapter = new BackupAdapter(mBackupHandler);
            mRecyclerView.setAdapter(mAdapter);
            mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
            return v;
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            super.onCreateOptionsMenu(menu, inflater);
            menu.findItem(R.id.action_done).setVisible(false);
            menu.findItem(R.id.action_refresh).setVisible(true);
            menu.findItem(R.id.action_run).setVisible(true);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.action_refresh) {
                mBackupHandler.updateBackupList();
                mBackupItems = mBackupHandler.getBackups();
                mAdapter.notifyDataSetChanged();
            } else {
                super.onOptionsItemSelected(item);
            }

            return true;
        }

        public void setBackupHandler(IBackupHandler handler) {
            mBackupHandler = handler;
        }
    }
}
