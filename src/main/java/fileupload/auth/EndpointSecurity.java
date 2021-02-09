package fileupload.auth;

import fileupload.repositories.FileUploadRecordsRepository;
import org.springframework.stereotype.Component;

import javax.servlet.ServletRequest;

@Component
public class EndpointSecurity {

    private AuthUtils authUtils;
    private FileUploadRecordsRepository repository;

    public EndpointSecurity(AuthUtils authUtils, FileUploadRecordsRepository repository) {
        this.authUtils = authUtils;
        this.repository = repository;
    }

    public boolean isLoggedIn(ServletRequest request) {
        return authUtils.tryGetLoggedInUser(request).isPresent();
    }

    public boolean canDeleteFile(ServletRequest request, String filename) {
        return authUtils.tryGetLoggedInUser(request)
                .map(user -> authUtils.isAdmin(user) || isOwnerOfFile(user.getUsername(), filename))
                .orElse(false);
    }

    private boolean isOwnerOfFile(String username, String filename) {
        return repository.findByName(filename).getUsername().equals(username);
    }
}
