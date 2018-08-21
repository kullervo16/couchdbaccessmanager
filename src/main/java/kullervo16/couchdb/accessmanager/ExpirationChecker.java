package kullervo16.couchdb.accessmanager;

import kullervo16.couchdb.accessmanager.config.CouchDbClientFactory;
import kullervo16.couchdb.accessmanager.utils.PlainRestClient;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.lightcouch.CouchDbClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ExpirationChecker {

    @Autowired
    private CouchDbClientFactory couchDbClientFactory;

    @Autowired
    private PlainRestClient restClient;

    @Autowired
    private AccessService accessService;

    @Scheduled(fixedRate = 2*60*1000)
    public void reportCurrentTime() {
        long startTime = System.currentTimeMillis();
        JSONArray viewResult = restClient.getPendingExpirations();
        if(viewResult == null) {
            // the view does not yet/no longer exist, (re)create it
            try(InputStream is = this.getClass().getResourceAsStream("/expiration.json")) {
                this.restClient.updateUserViewDoc(is);
            }catch(IOException ioe) {
                ioe.printStackTrace();
                log.error("Cannot save view "+ioe.getMessage());
            }
        } else {
            List<Map> pendingTasks = viewResult.toList().stream().map(o -> (Map)o).collect(Collectors.toList());
            for (Map pendingTask : pendingTasks) {
                long expirationTime = Long.valueOf(pendingTask.get("key").toString());
                if(startTime > expirationTime) {
                    String userId = pendingTask.get("id").toString().split(":")[1];
                    String db = ((Map) pendingTask.get("value")).get("db").toString();
                    String type = ((Map) pendingTask.get("value")).get("type").toString();
                    log.info("Remove " + type + " access from " + db + " for user " + userId);
                    this.accessService.removeAccess(userId,type, db);

                    JSONObject userDoc = restClient.getUserDoc(userId);
                    JSONArray openExpirations = userDoc.getJSONArray("expiration");
                    for(int i=0;i<openExpirations.length();i++) {
                        JSONObject expiration = openExpirations.getJSONObject(i);
                        if(expiration.get("db").equals(db) &&
                                expiration.get("type").equals(type) &&
                                expiration.get("expirationTimeStamp").equals(pendingTask.get("key").toString())) {
                            // this is our iteration
                            openExpirations.remove(i);
                            break;
                        }
                    }
                    couchDbClientFactory.getClient("_users").update(userDoc.toMap());
                }
            }
        }
    }
}
