package imagerepo.auth;

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
        return authUtils.tryGetLoggedInUser(request).isPresent();
    }

    public boolean canDeleteImage(ServletRequest request, String imagename) {
        return authUtils.tryGetLoggedInUser(request)
                .map(user -> authUtils.isAdmin(user) || isOwnerOfImage(user.getUsername(), imagename))
                .orElse(false);
    }

    private boolean isOwnerOfImage(String username, String imagename) {
        return repository.findByName(imagename).getUsername().equals(username);
    }
}
