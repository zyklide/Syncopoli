package org.amoradi.syncopoli.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.amoradi.syncopoli.async.DownloadBinaryTask;
import org.amoradi.syncopoli.utils.BackupHandler;
import org.amoradi.syncopoli.models.BackupItem;
import org.amoradi.syncopoli.interfaces.IBackupHandler;
import org.amoradi.syncopoli.R;
import org.amoradi.syncopoli.fragments.SettingsFragment;
import org.amoradi.syncopoli.fragments.AddBackupItemFragment;
import org.amoradi.syncopoli.fragments.BackupListFragment;
import org.amoradi.syncopoli.fragments.BackupLogFragment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class BackupActivity extends AppCompatActivity implements IBackupHandler {
    private static final String TAG = "BackupActivity";

    public static final String SYNC_AUTHORITY = "org.amoradi.syncopoli.provider";
    public static final String SYNC_ACCOUNT_NAME = "Syncopoli Sync Account";
    public static final String SYNC_ACCOUNT_TYPE = "org.amoradi.syncopoli";

    Account mAccount;
    BackupHandler mBackupHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);

        mAccount = createSyncAccount(this);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        long freq = Long.parseLong(prefs.getString(SettingsFragment.KEY_FREQUENCY, "8"));

        // hours to seconds
        freq = freq * 3600;

        ContentResolver.addPeriodicSync(mAccount, SYNC_AUTHORITY, new Bundle(), freq);

        copyExecutables();

        mBackupHandler = new BackupHandler(this);

        BackupListFragment f = new BackupListFragment();
        f.setBackupHandler(this);
        setCurrentFragment(f, false);
    }

    @Override
    public void setContentView(@LayoutRes int layoutResId) {
        super.setContentView(layoutResId);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    public static Account createSyncAccount(Context ctx) {
        Account account = new Account(SYNC_ACCOUNT_NAME, SYNC_ACCOUNT_TYPE);
        AccountManager accountManager = AccountManager.get(ctx);
        if (accountManager.addAccountExplicitly(account, null, null)) {
            ContentResolver.setIsSyncable(account, SYNC_AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account, SYNC_AUTHORITY, true);
        }
        return account;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // add items to the action bar if present
        getMenuInflater().inflate(R.menu.backup, menu);
        menu.findItem(R.id.action_done).setVisible(false);
        menu.findItem(R.id.action_refresh).setVisible(true);
        menu.findItem(R.id.action_run).setVisible(true);
        return true;
    }

    public void syncBackups() {
        Snackbar.make(findViewById(R.id.backuplist_coordinator), "Running all sync tasks", Snackbar.LENGTH_SHORT).show();
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
            syncBackups();
        } else if (id == R.id.menu_settings) {
            setCurrentFragment(new SettingsFragment(), true);
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public int addBackup(BackupItem item) {
        if (mBackupHandler.addBackup(item) == BackupHandler.ERROR_EXISTS) {
            Toast.makeText(getApplicationContext(), "Profile '" + item.name + "' already exists", Toast.LENGTH_SHORT).show();
        }

        BackupListFragment f = new BackupListFragment();
        f.setBackupHandler(this);
        setCurrentFragment(f, true);
        return 0;
    }

    public int updateBackup(BackupItem item) {
        mBackupHandler.updateBackup(item);
        mBackupHandler.updateBackupList();

        BackupListFragment f = new BackupListFragment();
        f.setBackupHandler(this);
        setCurrentFragment(f, true);
        return 0;
    }

    public int removeBackup(BackupItem item) {
        int ret = mBackupHandler.removeBackup(item);
        if (ret == 0) {
            mBackupHandler.updateBackupList();
        }

        return ret;
    }

    public int editBackup(BackupItem item) {
        AddBackupItemFragment f = new AddBackupItemFragment();
        f.setBackupContent(item);
        getFragmentManager().beginTransaction().replace(R.id.content_container, f).addToBackStack(null).commit();
        return 0;
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
        setCurrentFragment(f, true);
    }


    private void setCurrentFragment(Fragment f, boolean stack) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction().replace(R.id.content_container, f);
        if (stack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    public void copyExecutables() {
        copyExecutable("rsync");
        copyExecutable("ssh");
    }

    public void copyExecutable(String filename) {
        File file = getFileStreamPath(filename);
        if (file.exists()) {
            return;
        }
        new DownloadBinaryTask(this).execute(filename);
    }
}