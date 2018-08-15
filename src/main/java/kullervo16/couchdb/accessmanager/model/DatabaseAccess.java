package kullervo16.couchdb.accessmanager.model;

import lombok.Data;

@Data
public class DatabaseAccess {
    private String dbName;
    private boolean readAccess;
    private boolean writeAccess;
    private boolean adminAccess;
    private String expires;
}
