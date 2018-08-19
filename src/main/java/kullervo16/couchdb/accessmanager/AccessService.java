package kullervo16.couchdb.accessmanager;

import kullervo16.couchdb.accessmanager.config.CouchDbClientFactory;
import kullervo16.couchdb.accessmanager.model.DatabaseAccess;
import kullervo16.couchdb.accessmanager.model.NoAccessException;
import kullervo16.couchdb.accessmanager.model.UserData;
import kullervo16.couchdb.accessmanager.utils.PlainRestClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.lightcouch.CouchDbClient;
import org.lightcouch.NoDocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class AccessService {

    public static final String ACCESS_MANAGER = "access_manager";

    @Autowired
    private CouchDbClientFactory couchDbClientFactory;

    @Autowired
    private PlainRestClient plainRestClient;

    /**
     * Method to determine the access a user has on the various databases
     * @param userName
     * @param userId
     * @param userRoles
     * @return
     * @throws IOException
     */
    public UserData getUserData(String userName, String userId, Set<String> userRoles) throws IOException {
        UserData userData = new UserData();
        userData.setUserName(userName);
        userData.setUserId(userId);
        userData.getRoles().addAll(userRoles);

        CouchDbClient client = couchDbClientFactory.getClient("_users");
        try {
            JSONObject dbUser = client.find(JSONObject.class, "org.couchdb.user:" + userData.getUserId());
            userData.setNewUser(false);

        } catch (NoDocumentException e) {
            // user not in yet, create it
            userData.setNewUser(true);
            userData.setPassword(UUID.randomUUID().toString());
            Map newUser = new HashMap<>();
            newUser.put("name", userData.getUserId());
            newUser.put("password", userData.getPassword());
            newUser.put("roles", new ArrayList<>());
            newUser.put("type", "user");
            newUser.put("_id", "org.couchdb.user:" + userData.getUserId());
            System.out.println(newUser);
            client.save(newUser);

            CouchDbClient accessManagerClient = this.couchDbClientFactory.getClient(ACCESS_MANAGER);
            Map creationEvent = new HashMap();
            creationEvent.put("user", userId);
            creationEvent.put("type", "userCreated");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            creationEvent.put("timeStamp", sdf.format(new Date()));
            accessManagerClient.save(creationEvent);
        }
        // now list all databases and check whether the user has access or not
        for (String dbName : this.plainRestClient.getDbNames()) {
            if (!"_users".equals(dbName)) {
                System.out.println(dbName);
                CouchDbClient dbClient = couchDbClientFactory.getClient(dbName);

                DatabaseAccess dbAccess = new DatabaseAccess();
                Map security = getSecurityDoc(dbClient, dbName);
                JSONObject couchUserData = this.plainRestClient.getUserDoc(userId);

                dbAccess.setAdminAccess(this.hasAdminAccess(userId, security));
                dbAccess.setReadAccess(this.hasReadAccess(userId, security));
                if (couchUserData.has("roles")) {
                    dbAccess.setWriteAccess(this.hasWriteAccess(dbName, userId, security, couchUserData.getJSONArray("roles").toList()));
                } else {
                    dbAccess.setWriteAccess(false); // no roles for this user (yet), so defintely not a writer role for this database
                }
                userData.getAccessMap().put(dbName, dbAccess);
            }
        }
        return userData;
    }

    /**
     * Tries to add an access for this user of the requested type. Will check presence of required role and db restriction attribute
     * @param userId
     * @param type
     * @param dbName
     * @param userroles
     * @throws NoAccessException
     */
    public void addAccess(String userId, String type, String dbName, Set<String> userroles) throws NoAccessException {
        CouchDbClient dbClient = this.couchDbClientFactory.getClient(dbName);
        CouchDbClient usersClient = this.couchDbClientFactory.getClient("_users");
        CouchDbClient accessManagerClient = this.couchDbClientFactory.getClient(ACCESS_MANAGER);
        Map security = getSecurityDoc(dbClient, dbName);
        switch(type) {
            case "admin":
                // first check whether the user has the proper role
                if(!userroles.contains("couch_admin")) {
                    throw new NoAccessException("Missing role");
                }
                if(!security.containsKey("admins")) {
                    security.put("admins", getEmptySecurityMap());
                }
                ((List<String>)((Map)security.get("admins")).get("names")).add(userId);
                break;
            case "reader":
            case "writer":
                // first check whether the user has the proper role
                if(!userroles.contains("couch_"+type)) {
                    throw new NoAccessException("Missing role");
                }
                if(!security.containsKey("members")) {
                    security.put("members", getEmptySecurityMap());
                }
                ((List<String>)((Map)security.get("members")).get("names")).add(userId);
                if(type.equals("writer")) {
                    // add the role to the list of roles
                    JSONObject couchUserData = this.plainRestClient.getUserDoc(userId);
                    if(!couchUserData.has("roles")) {
                        couchUserData.put("roles", new JSONArray());
                    }
                    if(!couchUserData.getJSONArray("roles").toList().contains(dbName+"_writer")) {
                        couchUserData.getJSONArray("roles").put(dbName + "_writer");
                        usersClient.update(couchUserData.toMap());
                    }
                }
                break;
        }


        security.put("_id", "_security");
        dbClient.save(security);

        Map creationEvent = new HashMap();
        creationEvent.put("user", userId);
        creationEvent.put("type", "accessChanged");
        creationEvent.put("database", dbName);
        creationEvent.put("accessType", type);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        creationEvent.put("timeStamp", sdf.format(new Date()));
        accessManagerClient.save(creationEvent);
    }


    // =============================================================================
    //                  A U X    M E T H O D S
    // =============================================================================

    private boolean hasAdminAccess(String userId, Map security) {
        if(security.containsKey("admins")) {
            Map admins = (Map) security.get("admins");
            if(admins.containsKey("names")) {
                List<String> adminNames = (List<String>) admins.get("names");
                return adminNames.contains(userId);
            }
        }
        return false;
    }

    private boolean hasWriteAccess(String dbName, String userId, Map security, List<Object> couchUserRoles) {
        if(security.containsKey("members")) {
            Map members = (Map) security.get("members");
            if(members.containsKey("names")) {
                List<String> adminNames = (List<String>) members.get("names");
                if(adminNames.contains(userId) && couchUserRoles != null) {
                    return couchUserRoles.contains(dbName+"_writer");
                }
                return false;
            }
        }
        return false;
    }

    private boolean hasReadAccess(String userId, Map security) {
        if(security.containsKey("members")) {
            Map members = (Map) security.get("members");
            if(members.containsKey("names")) {
                List<String> adminNames = (List<String>) members.get("names");
                return adminNames.contains(userId);
            }
        }
        return false;
    }

    private List<String> getDbRestriction(String userId, String role, Map<String, Object> attributes) {
        if(attributes.containsKey("couch_"+role+"_dbs")) {
            return Arrays.stream(attributes.get("couch_" + role + "_dbs").toString().split(",")).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private Map getSecurityDoc(CouchDbClient dbClient, String dbName) {
        Map security;
        try {
            security = dbClient.find(Map.class, "_security");
            if(security.isEmpty()) {
                throw new NoDocumentException("trigger security creation");
            }
        }catch(NoDocumentException nde) {
            // no security setting yet, create a default one
            Map newSecurity = new HashMap();
            newSecurity.put("admins", getEmptySecurityMap());
            newSecurity.put("members", getEmptySecurityMap());
            newSecurity.put("_id","_security");
            dbClient.save(newSecurity);
            security = dbClient.find(Map.class, "_security");

            // also set the write protection view
            Map writeProtection = new HashMap();
            writeProtection.put("_id","_design/accessMgr");
            writeProtection.put("validate_doc_update", "function(newDoc, oldDoc, userCtx) { if (userCtx.roles.indexOf(userCtx.db + '_writer') < 0) throw({forbidden: 'you are not allowed to write documents in this database '+userCtx.db+'/'+userCtx.roles+'/'+userCtx.roles.indexOf(userCtx.db + '_writer')});}");
            dbClient.save(writeProtection);
        }
        return security;
    }

    private Map getEmptySecurityMap() {
        Map empty = new HashMap();
        List<String> nameList = new ArrayList<>();

        nameList.add("non-existing-user-to-prevent-access");

        empty.put("names",nameList);
        empty.put("roles", new ArrayList<String>());
        return empty;
    }
}
