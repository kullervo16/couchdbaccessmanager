package kullervo16.couchdb.accessmanager;

import com.fasterxml.jackson.databind.util.JSONPObject;
import kullervo16.couchdb.accessmanager.config.CouchDbClientFactory;
import kullervo16.couchdb.accessmanager.model.UserData;
import org.json.JSONObject;
import org.lightcouch.CouchDbClient;
import org.lightcouch.NoDocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/access")
public class Controller {

    private static final String template = "Hello, %s %s %s!";
    private final AtomicLong counter = new AtomicLong();

    @Autowired
    private CouchDbClientFactory couchDbClientFactory;

    @RequestMapping("/userData")
    public UserData getUserData(Principal user) {
        UserData userData = new UserData();
        userData.setUserName(AccessHelper.getUserName(user));
        userData.setUserId(AccessHelper.getUserId(user));

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
        }
        return userData;
    }


}
