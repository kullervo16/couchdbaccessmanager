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
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PlainRestClient {

    @Value("${couch.host}")
    private String chouchHost;

    @Value("${couch.port}")
    private Integer couchPort;

    @Value("${couch.userName}")
    private String couchUsername;

    @Value("${couch.pwd}")
    private String couchPwd;
    private CredentialsProvider provider;
    private BasicAuthCache authCache;
    private CloseableHttpClient client;
    private HttpClientContext context;

    private void init() {
        if(this.client == null) {
            this.provider = getCredentialsProvider();

            HttpHost targetHost = new HttpHost(this.chouchHost, this.couchPort, "http");
            this.authCache = new BasicAuthCache();
            authCache.put(targetHost, new BasicScheme());

            // Add AuthCache to the execution context
            this.context = HttpClientContext.create();
            context.setCredentialsProvider(provider);
            context.setAuthCache(authCache);

            this.client = HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(provider)
                    .build();
        }
    }


    public List<String> getDbNames()  {

        init();
        try {
            HttpResponse response = client.execute(
                    new HttpGet("http://" + this.chouchHost + ":" + this.couchPort + "/_all_dbs"), context);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                try (InputStream is = response.getEntity().getContent()){
                    JSONArray dbs = new JSONArray(getContent(is));
                    return dbs.toList().stream().map(o -> o.toString()).collect(Collectors.toList());
                }

            } else {
                throw new IllegalStateException("Cannot get all databases : " + statusCode);

            }
        }catch(IOException ioe) {
            this.client = null; // force recreation
            throw new IllegalStateException("Cannot get all databases : " +ioe.getMessage(), ioe);
        }
    }

    /**
     * For some reason, lightcouch doesn't load the user doc as it should.. fall back to basic HTTP :-)
     * @param userName
     * @return
     */
    public JSONObject getUserDoc(String userName) {
        init();
        try {
        HttpResponse response = client.execute(
                new HttpGet("http://"+this.chouchHost+":"+this.couchPort+"/_users/org.couchdb.user:"+userName), context);
        int statusCode = response.getStatusLine().getStatusCode();
        if(statusCode == 200) {
            try(InputStream is = response.getEntity().getContent()) {
                return new JSONObject(getContent(is));
            }

        } else {
            throw new IllegalStateException("Cannot list user : "+statusCode);
        }
        }catch(IOException ioe) {
            this.client = null; // force recreation
            throw new IllegalStateException("Cannot list user : " +ioe.getMessage(), ioe);
        }
    }

    /**
     * For some reason, lightcouch doesn't load the user views as it should.. fall back to basic HTTP :-)
     * @return
     */
    public JSONArray getPendingExpirations() {
        init();
        try {
            HttpResponse response = client.execute(
                    new HttpGet("http://"+this.chouchHost+":"+this.couchPort+"/_users/_design/accessManager/_view/expiration"), context);
            int statusCode = response.getStatusLine().getStatusCode();
            if(statusCode == 200) {
                try(InputStream is = response.getEntity().getContent()) {
                    return new JSONObject(getContent(is)).getJSONArray("rows");
                }

            } else if(statusCode == 404) {
              // design doc not (yet) present.. indicate via return null so the logic can create it
                return null;
            } else {
                throw new IllegalStateException("Cannot get pending expirations : "+statusCode);
            }
        }catch(IOException ioe) {
            this.client = null; // force recreation
            throw new IllegalStateException("Cannot get pending expirations : " +ioe.getMessage(), ioe);
        }
    }

    private String getContent(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder content = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
            content.append(line);
            line = reader.readLine();
        }
        return content.toString();
    }

    private CredentialsProvider getCredentialsProvider() {
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials
                = new UsernamePasswordCredentials(this.couchUsername, this.couchPwd);
        provider.setCredentials(AuthScope.ANY, credentials);
        return provider;
    }
}
