package kullervo16.couchdb.accessmanager;

import kullervo16.couchdb.accessmanager.config.CouchDbClientFactory;
import org.keycloak.KeycloakPrincipal;
import org.lightcouch.CouchDbClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@RestController

public class Controller {

    private static final String template = "Hello, %s %s %s!";
    private final AtomicLong counter = new AtomicLong();

    @Autowired
    private CouchDbClientFactory couchDbClientFactory;

    @RequestMapping("/greeting")
    public Greeting greeting(Principal user) {
//        throw new IllegalArgumentException(user.toString());
        CouchDbClient client = couchDbClientFactory.getClient("_users");
        return new Greeting(counter.incrementAndGet(),
                String.format(template, user.getName(), getRoles(user), getAttributes(user)));
    }

    @RequestMapping("/greeting2")
    public Greeting greeting2(@RequestParam(value="name", defaultValue="World") String name, Principal user) {
        if(this.getRoles(user).contains("secret")) {
            return new Greeting(counter.incrementAndGet(),
                    String.format(template, name, "I am an example of content filtered on role !"));
        } else {
            System.err.println("DOES NOT CONTAIN");
        }
        return new Greeting(counter.incrementAndGet(),
                String.format(template, name, getRoles(user)));
    }

    // TODO : move this to some helper class

    /**
     * Combines the realm and the client roles
     * @return
     * @param user
     */
    private Set<String> getRoles(Principal user) {
        //KeycloakPrincipal principal = (KeycloakPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        KeycloakPrincipal principal = (KeycloakPrincipal) user;
        Set<String> roles = new HashSet<>(principal.getKeycloakSecurityContext().getToken().getRealmAccess().getRoles());
        roles.addAll(principal.getKeycloakSecurityContext().getToken().getResourceAccess("couchdb").getRoles());
        return roles;
    }

    private Map<String,Object> getAttributes(Principal user) {
        //KeycloakPrincipal principal = (KeycloakPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        KeycloakPrincipal principal = (KeycloakPrincipal) user;
        return principal.getKeycloakSecurityContext().getToken().getOtherClaims();
    }
}
