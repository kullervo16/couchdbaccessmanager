package kullervo16.couchdb.accessmanager.utils;

import org.keycloak.KeycloakPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class AccessHelper {

    @Value("${keycloak.resource}")
    private String ourClient;

    /**
     * Combines the realm and the client roles
     * @return
     * @param user
     */
    public Set<String> getRoles(Principal user) {
        KeycloakPrincipal principal = (KeycloakPrincipal) user;
        Set<String> roles = new HashSet<>(principal.getKeycloakSecurityContext().getToken().getRealmAccess().getRoles());
        roles.addAll(principal.getKeycloakSecurityContext().getToken().getResourceAccess(ourClient).getRoles());
        return roles;
    }

    public Map<String,Object> getAttributes(Principal user) {
        KeycloakPrincipal principal = (KeycloakPrincipal) user;
        return principal.getKeycloakSecurityContext().getToken().getOtherClaims();
    }

    public String getUserName(Principal user) {
        KeycloakPrincipal principal = (KeycloakPrincipal) user;
        return principal.getKeycloakSecurityContext().getToken().getName();
    }

    public String getUserId(Principal user) {
        KeycloakPrincipal principal = (KeycloakPrincipal) user;
        return principal.getKeycloakSecurityContext().getToken().getPreferredUsername().replace("@","_");
    }


}
