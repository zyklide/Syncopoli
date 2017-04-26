package org.amoradi.syncopoli.models;

import java.util.Date;
import java.util.Set;

public class BackupItem {
    public enum Direction {
        INCOMING,
        OUTGOING
    }

    public String name;
    public String source;
    public String destination;
    public String logFileName;
    public Date lastUpdate;
    public Direction direction;
    public Set<String> folders;

    public String rsync_options;
}