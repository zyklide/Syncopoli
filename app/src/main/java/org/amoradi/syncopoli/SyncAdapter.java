package org.amoradi.syncopoli;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

import java.io.IOException;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    public SyncAdapter(Context ctx, boolean autoInit) {
        super(ctx, autoInit);
    }

    public SyncAdapter(Context ctx, boolean autoInit, boolean autoParallel) {
        super(ctx, autoInit, autoParallel);
    }

    @Override
    public void onPerformSync(Account acc, Bundle bun, String authority, ContentProviderClient cpc, SyncResult res) {
        BackupHandler backupHandler = new BackupHandler(getContext());

        for (BackupItem b : backupHandler.getBackups()) {
            int ret = backupHandler.runBackup(b);
            if (ret != 0) {
                try {
                    getContext().openFileOutput(b.logFileName, Context.MODE_PRIVATE)
                            .write(("An error occurred: rsync return status code " + ret + "\n").getBytes());
                } catch (IOException e) {
                }
            }
        }
    }
}
