package org.amoradi.topoli;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

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

public class BackupHandler implements IBackupHandler {
    private List<BackupItem> mBackupItems;
    Context mContext;

    public BackupHandler(Context ctx) {
        mContext = ctx;
        updateBackupList();
    }

    public void addBackup(BackupItem item) {
        if (item.source.equals("") || item.name.equals("") || item.destination.equals("")) {
            return;
        }

        BackupSyncOpenHelper dbHelper = new BackupSyncOpenHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(BackupSyncSchema.COLUMN_TYPE, "backup");
        values.put(BackupSyncSchema.COLUMN_NAME, item.name);
        values.put(BackupSyncSchema.COLUMN_SOURCE, item.source);
        values.put(BackupSyncSchema.COLUMN_DESTINATION, item.destination);
        values.put(BackupSyncSchema.COLUMN_LAST_UPDATE, "");

        db.insert(BackupSyncSchema.TABLE_NAME, null, values);
        db.close();
        dbHelper.close();

        updateBackupList();
    }

    public List<BackupItem> getBackups() {
        return mBackupItems;
    }

    public void updateBackupList() {
        List<BackupItem> bl = new ArrayList<>();

        BackupSyncOpenHelper dbHelper = new BackupSyncOpenHelper(mContext);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        /*
        String[] proj = {BackupSyncSchema.COLUMN_NAME,
                BackupSyncSchema.COLUMN_SOURCE,
                BackupSyncSchema.COLUMN_DESTINATION,
                BackupSyncSchema.COLUMN_LAST_UPDATE};
        */

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

        b.lastUpdate = new Date();

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        values.put(BackupSyncSchema.COLUMN_LAST_UPDATE, df.format(b.lastUpdate));

        db.update(BackupSyncSchema.TABLE_NAME, values, "name='" + b.name + "'", null);
        db.close();
        dbHelper.close();
    }

    public int runBackup(BackupItem b) {
        try {
            // Executes the command.
            String rsyncPath = new File(mContext.getFilesDir(), "rsync").getAbsolutePath();
            String sshPath = new File(mContext.getFilesDir(), "ssh").getAbsolutePath();

            File f = new File(rsyncPath);
            FileOutputStream logFile = mContext.openFileOutput(b.logFileName, Context.MODE_PRIVATE);

            updateBackupTimestamp(b);
            logFile.write((b.lastUpdate.toString() + " \n\n").getBytes());

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            String rsync_username = prefs.getString(SettingsActivity.KEY_RSYNC_USERNAME, "");
            String rsync_options = prefs.getString(SettingsActivity.KEY_RSYNC_OPTIONS, "");
            String server_address = prefs.getString(SettingsActivity.KEY_SERVER_ADDRESS, "");
            String protocol = prefs.getString(SettingsActivity.KEY_PROTOCOL, "SSH");
            String private_key = prefs.getString(SettingsActivity.KEY_PRIVATE_KEY, "");
            String port = prefs.getString(SettingsActivity.KEY_PORT, "22");

            List<String> args = new ArrayList<>();

            args.add(f.getAbsolutePath());
            args.add("--itemize-changes");
            Collections.addAll(args, rsync_options.split(" "));

            if (protocol.equals("SSH")) {
                args.add("-e");
                args.add(sshPath + " -y -p " + port + " -i " + private_key);
            }

            args.add(b.source);
            args.add(rsync_username + "@" + server_address + ":" + b.destination);

            ProcessBuilder pb = new ProcessBuilder(args);
            pb.directory(mContext.getFilesDir());
            //pb.redirectErrorStream(true);
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
                logFile.write(output.toString().getBytes());
                //Log.v(TAG, "STDOUT: " + output.toString());
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

    public void syncBackups() {}
    public void showLog(BackupItem b) {}
}
