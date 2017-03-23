package org.amoradi.syncopoli;


import android.accounts.Account;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.amoradi.syncopoli.activities.BackupActivity;

public class BackupReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (!intent.getAction().equals("android.net.wifi.WIFI_STATE_CHANGED") &&
            !intent.getAction().equals("android.net.wifi.STATE_CHANGE")) {
            return;
        }

        BackupHandler h = new BackupHandler(ctx);
        if (h.getRunOnWifi() && h.canRunBackup()) {
            Account acc = new Account(BackupActivity.SYNC_ACCOUNT_NAME, BackupActivity.SYNC_ACCOUNT_TYPE);

            Bundle settingsBundle = new Bundle();
            settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

            h.setRunOnWifi(false);
            ContentResolver.requestSync(acc, BackupActivity.SYNC_AUTHORITY, settingsBundle);
        }
    }
}
