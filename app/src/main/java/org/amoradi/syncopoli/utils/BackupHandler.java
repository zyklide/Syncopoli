package org.amoradi.syncopoli.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import org.amoradi.syncopoli.fragments.SettingsFragment;
import org.amoradi.syncopoli.interfaces.IBackupHandler;
import org.amoradi.syncopoli.models.BackupItem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BackupHandler implements IBackupHandler {
    private List<BackupItem> mBackupItems;
    Context mContext;

    public static final int ERROR_NO_WIFI = -2;
    public static final int ERROR_EXISTS = -3;
    public static final int ERROR_MISSING = -4;
    public static final int ERROR_TOO_MANY_RESULTS = -5;

    public BackupHandler(Context ctx) {
        mContext = ctx;
        updateBackupList();
    }

    public int addBackup(BackupItem item) {
        if (item.source.equals("") || item.name.equals("") || item.destination.equals("")) {
            return -1;
        }

        for (BackupItem x : mBackupItems) {
            if (x.name.equals(item.name)) {
                return ERROR_EXISTS;
            }
        }

        BackupSyncOpenHelper dbHelper = new BackupSyncOpenHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(BackupSyncSchema.COLUMN_TYPE, "backup");
        values.put(BackupSyncSchema.COLUMN_NAME, item.name);
        values.put(BackupSyncSchema.COLUMN_SOURCE, item.source);
        values.put(BackupSyncSchema.COLUMN_DESTINATION, item.destination);
        values.put(BackupSyncSchema.COLUMN_RSYNC_OPTIONS, "");
        values.put(BackupSyncSchema.COLUMN_LAST_UPDATE, "");

        if (item.direction == BackupItem.Direction.INCOMING) {
            values.put(BackupSyncSchema.COLUMN_DIRECTION, "INCOMING");
        } else {
            values.put(BackupSyncSchema.COLUMN_DIRECTION, "OUTGOING");
        }

        db.insert(BackupSyncSchema.TABLE_NAME, null, values);
        db.close();
        dbHelper.close();

        updateBackupList();
        return 0;
    }

    public int removeBackup(BackupItem item) {
        BackupSyncOpenHelper dbHelper = new BackupSyncOpenHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor c = db.query(
                BackupSyncSchema.TABLE_NAME,
                null,
                "name = '" + item.name + "'",
                null,
                null,
                null,
                BackupSyncSchema.COLUMN_NAME + " DESC",
                null
        );

        if (c.getCount() <= 0) {
            return BackupHandler.ERROR_MISSING;
        }

        if (c.getCount() > 1) {
            return BackupHandler.ERROR_TOO_MANY_RESULTS;
        }

        db.delete(BackupSyncSchema.TABLE_NAME, "name = '" + item.name + "'", null);
        return 0;
    }

    public List<BackupItem> getBackups() {
        return mBackupItems;
    }

    public void updateBackupList() {
        List<BackupItem> bl = new ArrayList<>();

        BackupSyncOpenHelper dbHelper = new BackupSyncOpenHelper(mContext);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.query(
                BackupSyncSchema.TABLE_NAME,
                null, //proj
                "type = 'backup'",
                null,
                null,
                null,
                BackupSyncSchema.COLUMN_NAME + " DESC",
                null
        );

        if (c.getCount() <= 0) {
            c.close();
            db.close();
            dbHelper.close();
            mBackupItems = bl;
            return;
        }

        c.moveToFirst();

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

        do {
            BackupItem x = new BackupItem();
            x.name = c.getString(c.getColumnIndex(BackupSyncSchema.COLUMN_NAME));
            x.source = c.getString(c.getColumnIndex(BackupSyncSchema.COLUMN_SOURCE));
            x.destination = c.getString(c.getColumnIndex(BackupSyncSchema.COLUMN_DESTINATION));
            x.rsync_options = c.getString(c.getColumnIndex(BackupSyncSchema.COLUMN_RSYNC_OPTIONS));

            String dir = c.getString(c.getColumnIndex(BackupSyncSchema.COLUMN_DIRECTION));
            if (dir.equals("INCOMING")) {
                x.direction = BackupItem.Direction.INCOMING;
            } else {
                x.direction = BackupItem.Direction.OUTGOING;
            }

            try {
                x.lastUpdate = df.parse(c.getString(c.getColumnIndex(BackupSyncSchema.COLUMN_LAST_UPDATE)));
            } catch (ParseException e) {
                x.lastUpdate = null;
            }

            x.logFileName = "log_" + x.name.replace(" ", "_");
            bl.add(x);
        } while(c.moveToNext());

        c.close();
        db.close();
        dbHelper.close();

        mBackupItems = bl;
    }

    public void updateBackupTimestamp(BackupItem b) {
        BackupSyncOpenHelper dbHelper = new BackupSyncOpenHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(BackupSyncSchema.COLUMN_TYPE, "backup");
        values.put(BackupSyncSchema.COLUMN_NAME, b.name);
        values.put(BackupSyncSchema.COLUMN_SOURCE, b.source);
        values.put(BackupSyncSchema.COLUMN_DESTINATION, b.destination);
        values.put(BackupSyncSchema.COLUMN_RSYNC_OPTIONS, b.rsync_options);

        b.lastUpdate = new Date();

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        values.put(BackupSyncSchema.COLUMN_LAST_UPDATE, df.format(b.lastUpdate));

        db.update(BackupSyncSchema.TABLE_NAME, values, "name='" + b.name + "'", null);
        db.close();
        dbHelper.close();
    }

    public int updateBackup(BackupItem b) {
        BackupSyncOpenHelper dbHelper = new BackupSyncOpenHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(BackupSyncSchema.COLUMN_TYPE, "backup");
        values.put(BackupSyncSchema.COLUMN_NAME, b.name);
        values.put(BackupSyncSchema.COLUMN_SOURCE, b.source);
        values.put(BackupSyncSchema.COLUMN_DESTINATION, b.destination);
        values.put(BackupSyncSchema.COLUMN_LAST_UPDATE, "");
        values.put(BackupSyncSchema.COLUMN_RSYNC_OPTIONS, b.rsync_options);

        if (b.direction == BackupItem.Direction.INCOMING) {
            values.put(BackupSyncSchema.COLUMN_DIRECTION, "INCOMING");
        } else {
            values.put(BackupSyncSchema.COLUMN_DIRECTION, "OUTGOING");
        }

        db.update(BackupSyncSchema.TABLE_NAME, values, "name='" + b.name + "'", null);
        db.close();
        dbHelper.close();

        return 0;
    }

    public int runBackup(BackupItem b) {
        if (!canRunBackup()) {
            return ERROR_NO_WIFI;
        }

        FileOutputStream logFile;
        try {
          logFile = mContext.openFileOutput(b.logFileName, Context.MODE_PRIVATE);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }

        try {
            String rsyncPath = new File(mContext.getFilesDir(), "rsync").getAbsolutePath();
            String sshPath = new File(mContext.getFilesDir(), "ssh").getAbsolutePath();
            logFile.write(("Rsync path: " + rsyncPath + "\n").getBytes());
            logFile.write(("SSH path: " + sshPath + "\n").getBytes());

            File f = new File(rsyncPath);

            updateBackupTimestamp(b);
            logFile.write((b.lastUpdate.toString() + " \n\n").getBytes());

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            String rsync_username = prefs.getString(SettingsFragment.KEY_RSYNC_USERNAME, "");

            if (rsync_username.equals("")) {
                logFile.write("ERROR: Username not specified. Please set username in settings.".getBytes());
                return -1;
            }

            String rsync_options = prefs.getString(SettingsFragment.KEY_RSYNC_OPTIONS, "");
            String rsync_password = prefs.getString(SettingsFragment.KEY_RSYNC_PASSWORD, "");

            String server_address = prefs.getString(SettingsFragment.KEY_SERVER_ADDRESS, "");

            if (server_address.equals("")) {
                logFile.write("ERROR: Server address not specified. Please set Server address in settings.".getBytes());
                return -1;
            }

            String protocol = prefs.getString(SettingsFragment.KEY_PROTOCOL, "SSH");
            String private_key = prefs.getString(SettingsFragment.KEY_PRIVATE_KEY, "");
            String port = prefs.getString(SettingsFragment.KEY_PORT, "22");

            if (port.equals("")) {
                logFile.write("ERROR: Port not specified. Please set Port in settings.".getBytes());
                return -1;
            }

            /*
             * BUILD ARGUMENTS
             */

            List<String> args = new ArrayList<>();

            args.add(f.getAbsolutePath());

            if (!rsync_options.equals("")) {
                Collections.addAll(args, rsync_options.split(" "));
            }

            if (!b.rsync_options.equals("")) {
                Collections.addAll(args, b.rsync_options.split(" "));
            }

            if (protocol.equals("SSH")) {
                if (private_key.equals("")) {
                    logFile.write("ERROR: Private key is not specified while SSH protocol is in use.".getBytes());
                    return -1;
                }

                args.add("-e");
                args.add(sshPath + " -y -p " + port + " -i " + private_key);

                if (b.direction == BackupItem.Direction.OUTGOING) {
                    args.add(b.source);
                    args.add(rsync_username + "@" + server_address + ":" + b.destination);
                } else {
                    args.add(rsync_username + "@" + server_address + ":" + b.source);
                    args.add(b.destination);
                }

            } else if (protocol.equals("Rsync")) {
                if (b.direction == BackupItem.Direction.OUTGOING) {
                    args.add(b.source);
                    args.add(rsync_username + "@" + server_address + "::" + b.destination);
                } else {
                    args.add(rsync_username + "@" + server_address + "::" + b.source);
                    args.add(b.destination);
                }
            }

            /*
             * BUILD PROCESS
             */

            logFile.write("building process\n".getBytes());
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.directory(mContext.getFilesDir());
            pb.redirectErrorStream(true);

            // Set environment (make sure we have reasonable $HOME, so ssh can store keys)
            Map<String, String> env = pb.environment();
            env.put("HOME", mContext.getFilesDir().getAbsolutePath());

            if (protocol.equals("Rsync") && !rsync_password.equals("")) {
                env.put("RSYNC_PASSWORD", rsync_password);
            }

            /*
             * RUN PROCESS
             */

            Process process = pb.start();

            /*
             * GET STDOUT/STDERR
             */

            String temp = "";
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            /* Read STDOUT & STDERR */
            while ((temp = reader.readLine()) != null) {
                Log.v("BackupHandler", temp + "\n");
                logFile.write((temp + "\n").getBytes());
            }
            reader.close();

            // Wait for the command to finish.
            process.waitFor();

            // Show message how it ended.
            int errno = process.exitValue();
            if (errno != 0) {
                logFile.write(("\nSync FAILED (error code " + errno + ").\n").getBytes());
            } else {
                logFile.write("\nSync complete.\n".getBytes());
            }

            logFile.close();

            return errno;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean canRunBackup() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean wifi_only = prefs.getBoolean(SettingsFragment.KEY_WIFI_ONLY, false);

        if (wifi_only) {
            ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            if (!mWifi.isConnected()) {
                return false;
            }
        }

        return true;
    }

    public void setRunOnWifi(boolean run) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        editor.putBoolean("RunOnWifi", true);
        editor.apply();
    }

    public boolean getRunOnWifi() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        return prefs.getBoolean("RunOnWifi", false);
    }

    public void syncBackups() {}
    public void showLog(BackupItem b) {}
    public int editBackup(BackupItem b) {return 0;}
}