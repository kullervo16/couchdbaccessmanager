package kullervo16.couchdb.accessmanager.utils;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DbLister {

    @Value("${couch.host}")
    private String chouchHost;

    @Value("${couch.port}")
    private Integer couchPort;

    @Value("${couch.userName}")
    private String couchUsername;

    @Value("${couch.pwd}")
    private String couchPwd;



    public List<String> getDbNames() throws IOException {

        CredentialsProvider provider = getCredentialsProvider();

        HttpHost targetHost = new HttpHost(this.chouchHost, this.couchPort, "http");
        AuthCache authCache = new BasicAuthCache();
        authCache.put(targetHost, new BasicScheme());

        // Add AuthCache to the execution context
        final HttpClientContext context = HttpClientContext.create();
        context.setCredentialsProvider(provider);
        context.setAuthCache(authCache);

        HttpClient client = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(provider)
                .build();

        HttpResponse response = client.execute(
                new HttpGet("http://"+this.chouchHost+":"+this.couchPort+"/_all_dbs"), context);
        int statusCode = response.getStatusLine().getStatusCode();
        if(statusCode == 200) {
            try(InputStream is = response.getEntity().getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                StringBuilder content = new StringBuilder();
                String line = reader.readLine();
                while(line != null) {
                    content.append(line);
                    line = reader.readLine();
                }
                JSONArray dbs = new JSONArray(content.toString());
                return dbs.toList().stream().map(o -> o.toString()).collect(Collectors.toList());
            }

        } else {
            throw new IllegalStateException("Cannot get all databases : "+statusCode);
        }
    }

    private CredentialsProvider getCredentialsProvider() {
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials
                = new UsernamePasswordCredentials(this.couchUsername, this.couchPwd);
        provider.setCredentials(AuthScope.ANY, credentials);
        return provider;
    }
}
