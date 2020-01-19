package imagerepo.auth;

import imagerepo.auth.exceptions.UnauthorizedException;
import imagerepo.auth.models.AuthenticatedUser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Optional;

public class AuthenticatedHttpServletRequest extends HttpServletRequestWrapper {

    private final AuthenticatedUser user;

    public AuthenticatedHttpServletRequest(HttpServletRequest request, AuthenticatedUser user) {
        super(request);
        this.user = user;
    }

    public AuthenticatedUser getLoggedInUser() {
        return Optional.ofNullable(user)
                .orElseThrow(UnauthorizedException::new);
    }
}
