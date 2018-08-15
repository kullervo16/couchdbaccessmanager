package kullervo16.couchdb.accessmanager.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class UserData {
    private String userName;
    private String userId;
    private boolean newUser;
    private String password;
    private Map<String, DatabaseAccess> accessMap = new HashMap<>();
    private List<String> roles = new ArrayList<>();
}
