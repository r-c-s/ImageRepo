package imagerepo.auth;

import imagerepo.auth.exceptions.UnauthorizedException;
import imagerepo.auth.models.AuthenticatedUser;
import imagerepo.repositories.ImageRecordsRepository;
import org.springframework.stereotype.Component;

import javax.servlet.ServletRequest;

@Component
public class EndpointSecurity {

    private AuthUtils authUtils;
    private ImageRecordsRepository repository;

    public EndpointSecurity(AuthUtils authUtils, ImageRecordsRepository repository) {
        this.authUtils = authUtils;
        this.repository = repository;
    }

    public boolean isLoggedIn(ServletRequest request) {
        try {
            authUtils.tryGetLoggedInUser(request);
            return true;
        } catch (UnauthorizedException e) {
            return false;
        }
    }

    public boolean canDeleteImage(ServletRequest request, String imagename) {
        try {
            AuthenticatedUser user = authUtils.tryGetLoggedInUser(request);
            return authUtils.isAdmin(user) || isOwnerOfImage(user.getUsername(), imagename);
        } catch (UnauthorizedException e) {
            return false;
        }
    }

    private boolean isOwnerOfImage(String username, String imagename) {
        return repository.findByName(imagename).getUsername().equals(username);
    }
}
