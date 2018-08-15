package kullervo16.couchdb.accessmanager.utils;

import org.keycloak.KeycloakPrincipal;

import java.security.Principal;
import java.util.HashSet;
import java.util.List;
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

    public static boolean hasAdminAccess(Principal user, Map security) {
        if(security.containsKey("admins")) {
            Map admins = (Map) security.get("admins");
            if(admins.containsKey("names")) {
                List<String> adminNames = (List<String>) admins.get("names");
                return adminNames.contains(getUserId(user));
            }
        }
        return false;
    }

    public static boolean hasWriteAccess(Principal user, Map security) {
        if(security.containsKey("members")) {
            Map members = (Map) security.get("members");
            if(members.containsKey("names")) {
                List<String> adminNames = (List<String>) members.get("names");
                return adminNames.contains(getUserId(user));
            }
        }
        return false;
    }

    public static boolean hasReadAccess(Principal user, Map security) {
        if(security.containsKey("members")) {
            Map members = (Map) security.get("members");
            if(members.containsKey("names")) {
                List<String> adminNames = (List<String>) members.get("names");
                return adminNames.contains(getUserId(user));
            }
        }
        return false;
    }
}
