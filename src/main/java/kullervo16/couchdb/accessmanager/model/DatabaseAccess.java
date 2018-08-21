package kullervo16.couchdb.accessmanager.model;

import lombok.Data;

@Data
public class DatabaseAccess {
    private boolean readAccess;
    private boolean writeAccess;
    private boolean adminAccess;
    private String readExpires;
    private String writeExpires;
    private String adminExpires;
}
