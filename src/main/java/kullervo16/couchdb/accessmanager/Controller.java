package kullervo16.couchdb.accessmanager;

import kullervo16.couchdb.accessmanager.model.NoAccessException;
import kullervo16.couchdb.accessmanager.model.UserData;
import kullervo16.couchdb.accessmanager.utils.AccessHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.Principal;

@RestController
@RequestMapping("/access")
@Slf4j
public class Controller {


    @Autowired
    AccessService accessService;

    @Autowired
    private AccessHelper accessHelper;

    @RequestMapping("/userData")
    public UserData getUserData(Principal user) throws IOException {
        return this.accessService.getUserData(
                accessHelper.getUserName(user),
                accessHelper.getUserId(user),
                accessHelper.getRoles(user));
    }



    @RequestMapping("/requestAccess/{db}/{type}")
    public ResponseEntity getUserData(@PathVariable(value = "db") String db,
                                @PathVariable(value = "type") String type,
                                Principal user) throws IOException {

        try {
            this.accessService.addAccess(
                    accessHelper.getUserId(user),
                    type,
                    db,
                    accessHelper.getRoles(user));
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch(NoAccessException nae) {
            log.warn("No access for "+accessHelper.getUserId(user)+":"+nae.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

    }

}
