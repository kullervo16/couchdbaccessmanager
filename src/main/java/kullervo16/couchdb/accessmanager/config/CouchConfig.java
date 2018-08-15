package kullervo16.couchdb.accessmanager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CouchConfig {

    @Bean
    public CouchDbClientFactory createClientFactory(@Value("${couch.host}") String host,
                                                    @Value("${couch.port}")int port,
                                                    @Value("${couch.userName}")String userName,
                                                    @Value("${couch.pwd}")String pwd) {
        CouchDbClientFactory factory = new CouchDbClientFactory(host, port, userName, pwd);
        return factory;
    }
}
