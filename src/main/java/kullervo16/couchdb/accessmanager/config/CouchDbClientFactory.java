package kullervo16.couchdb.accessmanager.config;

import org.lightcouch.CouchDbClient;
import org.lightcouch.CouchDbProperties;

import java.util.HashMap;
import java.util.Map;

public class CouchDbClientFactory {

    private Map<String, CouchDbClient> clientMap = new HashMap<>();

    private String host;
    private int port;
    private String userName;
    private String pwd;

    public CouchDbClientFactory(String host, int port, String userName, String pwd) {
        this.host = host;
        this.port = port;
        this.userName = userName;
        this.pwd = pwd;
    }

    public CouchDbClient getClient(String dbName) {
        if(!this.clientMap.containsKey(dbName)) {
            CouchDbProperties properties = new CouchDbProperties()
                    .setDbName(dbName)
                    .setCreateDbIfNotExist(true)
                    .setProtocol("http")
                    .setHost(this.host)
                    .setPort(this.port)
                    .setUsername(this.userName)
                    .setPassword(this.pwd)
                    .setMaxConnections(100)
                    .setConnectionTimeout(0);

            CouchDbClient dbClient3 = new CouchDbClient(properties);
        }
        return this.clientMap.get(dbName);
    }


}
