package kullervo16.couchdb.accessmanager.model;

import lombok.Data;

import java.util.Map;

@Data
public class UserData {
    private String userName;
    private String userId;
    private boolean newUser;
    private String password;
    private Map<String, DatabaseAccess> accessMap;
}
