package imagerepo.auth;

import imagerepo.auth.exceptions.UnauthorizedException;
import imagerepo.auth.models.AuthenticatedUser;
import org.springframework.stereotype.Service;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

@Service
public class AuthUtils {

    public AuthenticatedUser tryGetLoggedInUser(ServletRequest request) {
        if (request instanceof AuthenticatedHttpServletRequest) {
            return ((AuthenticatedHttpServletRequest) request).getLoggedInUser();
        }
        if (request instanceof HttpServletRequestWrapper) {
            return tryGetLoggedInUser(((HttpServletRequestWrapper) request).getRequest());
        }
        throw new UnauthorizedException();
    }

    public boolean isAdmin(AuthenticatedUser user) {
        return user.getAuthorities().stream()
                .anyMatch(role -> role.equals("ROLE_ADMIN"));
    }
}
