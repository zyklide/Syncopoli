package org.amoradi.syncopoli.interfaces;

import org.amoradi.syncopoli.models.BackupItem;

import java.util.List;

public interface IBackupHandler {
    int addBackup(BackupItem bi);
    int removeBackup(BackupItem bi);
    int editBackup(BackupItem bi);
    int updateBackup(BackupItem bi);
    int runBackup(BackupItem bi);
    void showLog(BackupItem bi);
    void updateBackupTimestamp(BackupItem bi);
    void updateBackupList();
    List<BackupItem> getBackups();
    void syncBackups();
}