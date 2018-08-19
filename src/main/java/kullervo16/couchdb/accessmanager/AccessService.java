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
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
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
     * @param attributes
     * @throws NoAccessException
     */
    public void addAccess(String userId, String type, String dbName, Set<String> userroles, Map<String, Object> attributes) throws NoAccessException {
        CouchDbClient dbClient = this.couchDbClientFactory.getClient(dbName);
        CouchDbClient usersClient = this.couchDbClientFactory.getClient("_users");
        CouchDbClient accessManagerClient = this.couchDbClientFactory.getClient(ACCESS_MANAGER);
        Map security = getSecurityDoc(dbClient, dbName);

        // first check whether the user has the proper role
        if(!userroles.contains("couch_"+type)) {
            throw new NoAccessException("Missing role: couch_"+type);
        }

        // then check whether there is a db restriction for this user
        List<String> dbRestrictions = this.getDbRestriction(userId, type, attributes);
        if(!dbRestrictions.isEmpty() && !dbRestrictions.contains(dbName)) {
            throw new NoAccessException(dbName+" is not in the allowed databases for type "+type);
        }
        int duration = 1440;
        if(attributes.containsKey("couch_"+type+"_duration")) {
            duration = (Integer)attributes.get("couch_"+type+"_duration");
        }

        switch(type) {
            case "admin":
                if(!security.containsKey("admins")) {
                    security.put("admins", getEmptySecurityMap());
                }
                ((List<String>)((Map)security.get("admins")).get("names")).add(userId);

                break;
            case "reader":
            case "writer":
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
        if(duration > 0) {
            JSONObject couchUserData = this.plainRestClient.getUserDoc(userId);
            if (!couchUserData.has("expiration")) {
                couchUserData.put("expiration", new JSONArray());
            }
            long expirationTime = System.currentTimeMillis()+duration*60*1000;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Map<String,String> expirationEntry = new HashMap<>();
            expirationEntry.put("type", type);
            expirationEntry.put("expirationTime", sdf.format(new Date(expirationTime)));
            expirationEntry.put("expirationTimeStamp", ""+expirationTime);
            expirationEntry.put("db", dbName);
            couchUserData.getJSONArray("expiration").put(expirationEntry);

            usersClient.update(couchUserData.toMap());
        }


        security.put("_id", "_security");
        dbClient.save(security);

        Map creationEvent = new HashMap();
        creationEvent.put("user", userId);
        creationEvent.put("type", "accessGranted");
        creationEvent.put("database", dbName);
        creationEvent.put("accessType", type);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        creationEvent.put("timeStamp", sdf.format(new Date()));
        accessManagerClient.save(creationEvent);
    }

    /**
     * Remove the access. No roles or attributes needed, you can give up your claim without specifying a reason :-)
     * @param userId
     * @param type
     * @param dbName
     */
    public void removeAccess(String userId, String type, String dbName) {
        CouchDbClient dbClient = this.couchDbClientFactory.getClient(dbName);
        CouchDbClient usersClient = this.couchDbClientFactory.getClient("_users");
        CouchDbClient accessManagerClient = this.couchDbClientFactory.getClient(ACCESS_MANAGER);
        Map security = getSecurityDoc(dbClient, dbName);
        boolean change = false;
        switch(type) {
            case "admin":
                if(security.containsKey("admins")) {
                    List<String> nameList = (List<String>) ((Map) security.get("admins")).get("names");
                    while(nameList.contains(userId)) {
                        change = true;
                        nameList.remove(userId); // cope with manual changes... may be present multiple times
                    }
                    if(change) {
                        dbClient.save(security);
                    }

                }
                break;
            case "reader":
                // if you loose reader, you also can'r write any more... as described in the doc
                if(security.containsKey("members")) {
                    List<String> nameList = (List<String>) ((Map) security.get("members")).get("names");
                    while(nameList.contains(userId)) {
                        change = true;
                        nameList.remove(userId); // cope with manual changes... may be present multiple times
                    }
                    if(change) {
                        dbClient.save(security);
                    }
                }
                break;
            case "writer":
                JSONObject couchUserData = this.plainRestClient.getUserDoc(userId);
                if(couchUserData.has("roles")) {
                    List<String> roleList = couchUserData.getJSONArray("roles").toList().stream().map(o -> o.toString()).collect(Collectors.toList());
                    while(roleList.contains(dbName+"_writer")) {
                        roleList.remove(dbName+"_writer"); // cope with manual changes... may be present multiple times
                        change = true;
                    }
                    couchUserData.put("roles", new JSONArray(roleList));
                    usersClient.update(couchUserData.toMap());
                }

                break;
        }

        if(change) {
            Map removalEvent = new HashMap();
            removalEvent.put("user", userId);
            removalEvent.put("type", "accessRemoved");
            removalEvent.put("database", dbName);
            removalEvent.put("accessType", type);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            removalEvent.put("timeStamp", sdf.format(new Date()));
            accessManagerClient.save(removalEvent);
        }
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
