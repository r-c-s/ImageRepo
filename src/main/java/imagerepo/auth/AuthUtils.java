package imagerepo.auth;

import imagerepo.auth.models.AuthenticatedUser;
import org.springframework.stereotype.Service;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Optional;

@Service
public class AuthUtils {

    public Optional<AuthenticatedUser> tryGetLoggedInUser(ServletRequest request) {
        if (request instanceof AuthenticatedHttpServletRequest) {
            return Optional.of(((AuthenticatedHttpServletRequest) request).getLoggedInUser());
        }
        if (request instanceof HttpServletRequestWrapper) {
            return tryGetLoggedInUser(((HttpServletRequestWrapper) request).getRequest());
        }
        return Optional.empty();
    }

    public boolean isAdmin(AuthenticatedUser user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.equals("ADMIN"));
    }
}
