package kullervo16.couchdb.accessmanager;

import org.keycloak.KeycloakPrincipal;

import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AccessHelper {

    /**
     * Combines the realm and the client roles
     * @return
     * @param user
     */
    public static Set<String> getRoles(Principal user) {
        KeycloakPrincipal principal = (KeycloakPrincipal) user;
        Set<String> roles = new HashSet<>(principal.getKeycloakSecurityContext().getToken().getRealmAccess().getRoles());
        roles.addAll(principal.getKeycloakSecurityContext().getToken().getResourceAccess("couchdb").getRoles());
        return roles;
    }

    public static Map<String,Object> getAttributes(Principal user) {
        KeycloakPrincipal principal = (KeycloakPrincipal) user;
        return principal.getKeycloakSecurityContext().getToken().getOtherClaims();
    }

    public static String getUserName(Principal user) {
        KeycloakPrincipal principal = (KeycloakPrincipal) user;
        return principal.getKeycloakSecurityContext().getToken().getName();
    }

    public static String getUserId(Principal user) {
        KeycloakPrincipal principal = (KeycloakPrincipal) user;
        return principal.getKeycloakSecurityContext().getToken().getPreferredUsername().replace("@","_");
    }
}
