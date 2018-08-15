package kullervo16.couchdb.accessmanager;

import kullervo16.couchdb.accessmanager.config.CouchDbClientFactory;
import kullervo16.couchdb.accessmanager.model.DatabaseAccess;
import kullervo16.couchdb.accessmanager.model.UserData;
import kullervo16.couchdb.accessmanager.utils.AccessHelper;
import kullervo16.couchdb.accessmanager.utils.DbLister;
import org.json.JSONObject;
import org.lightcouch.CouchDbClient;
import org.lightcouch.NoDocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/access")
public class Controller {

    private static final String template = "Hello, %s %s %s!";
    public static final String ACCESS_MANAGER = "access_manager";
    private final AtomicLong counter = new AtomicLong();

    @Autowired
    private CouchDbClientFactory couchDbClientFactory;

    @Autowired
    private DbLister dbLister;

    @RequestMapping("/userData")
    public UserData getUserData(Principal user) throws IOException {
        UserData userData = new UserData();
        userData.setUserName(AccessHelper.getUserName(user));
        userData.setUserId(AccessHelper.getUserId(user));
        userData.getRoles().addAll(AccessHelper.getRoles(user));

        CouchDbClient client = couchDbClientFactory.getClient("_users");
        try {
            JSONObject dbUser = client.find(JSONObject.class, "org.couchdb.user:" + userData.getUserId());
            userData.setNewUser(false);

        }catch(NoDocumentException e) {
            // user not in yet, create it
            userData.setNewUser(true);
            userData.setPassword(UUID.randomUUID().toString());
            Map newUser = new HashMap<>();
            newUser.put("name", userData.getUserId());
            newUser.put("password", userData.getPassword());
            newUser.put("roles", new ArrayList<>()); // TODO : set based on attributes
            newUser.put("type", "user");
            newUser.put("_id","org.couchdb.user:" + userData.getUserId());
            System.out.println(newUser);
            client.save(newUser);

            CouchDbClient accessManagerClient = this.couchDbClientFactory.getClient(ACCESS_MANAGER);
            Map creationEvent = new HashMap();
            creationEvent.put("user", AccessHelper.getUserId(user));
            creationEvent.put("type", "userCreated");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            creationEvent.put("timeStamp", sdf.format(new Date()));
            accessManagerClient.save(creationEvent);
        }
        // now list all databases and check whether the user has access or not
        for(String dbName : this.dbLister.getDbNames()) {
            if(!"_users".equals(dbName)) {
                System.out.println(dbName);
                CouchDbClient dbClient = couchDbClientFactory.getClient(dbName);
                DatabaseAccess dbAccess = new DatabaseAccess();
                Map security = getSecurityDoc(dbClient, dbName);

                dbAccess.setAdminAccess(AccessHelper.hasAdminAccess(user, security));
                dbAccess.setReadAccess(AccessHelper.hasReadAccess(user, security));
                dbAccess.setWriteAccess(AccessHelper.hasWriteAccess(user, security));
                userData.getAccessMap().put(dbName, dbAccess);
            }
        }
        return userData;
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

    @RequestMapping("/requestAccess/{db}/{type}")
    public ResponseEntity getUserData(@PathVariable(value = "db") String db,
                                @PathVariable(value = "type") String type,
                                Principal user) throws IOException {
        CouchDbClient dbClient = this.couchDbClientFactory.getClient(db);
        CouchDbClient accessManagerClient = this.couchDbClientFactory.getClient(ACCESS_MANAGER);
        Map security = getSecurityDoc(dbClient, db);
        switch(type) {
            case "admin":
                // first check whether the user has the proper role
                if(!AccessHelper.getRoles(user).contains("couch_admin")) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
                if(!security.containsKey("admins")) {
                    security.put("admins", getEmptySecurityMap());
                }
                ((List<String>)((Map)security.get("admins")).get("names")).add(AccessHelper.getUserId(user));
                break;
            case "reader":
            case "writer":
                // first check whether the user has the proper role
                if(!AccessHelper.getRoles(user).contains("couch_"+type)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
                if(!security.containsKey("members")) {
                    security.put("members", getEmptySecurityMap());
                }
                ((List<String>)((Map)security.get("members")).get("names")).add(AccessHelper.getUserId(user));
                break;
        }


        security.put("_id", "_security");
        dbClient.save(security);

        Map creationEvent = new HashMap();
        creationEvent.put("user", AccessHelper.getUserId(user));
        creationEvent.put("type", "accessChanged");
        creationEvent.put("database", db);
        creationEvent.put("accessType", type);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        creationEvent.put("timeStamp", sdf.format(new Date()));
        accessManagerClient.save(creationEvent);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
