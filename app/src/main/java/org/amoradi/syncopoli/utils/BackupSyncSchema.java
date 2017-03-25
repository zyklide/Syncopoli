package org.amoradi.syncopoli.utils;

public class BackupSyncSchema {
    public static final String DATABASE_NAME = "syncopoli";
    public static final int DATABASE_VERSION = 3;
    public static final String TABLE_NAME = "backup_sync";

    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_SOURCE = "source";
    public static final String COLUMN_DESTINATION = "destination";
    public static final String COLUMN_LAST_UPDATE = "last_update";
    public static final String COLUMN_DIRECTION = "direction";
    public static final String COLUMN_RSYNC_OPTIONS = "rsync_options";
}