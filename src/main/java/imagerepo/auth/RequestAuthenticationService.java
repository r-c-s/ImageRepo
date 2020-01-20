package imagerepo.auth;

import imagerepo.apis.AuthService;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@PropertySource("file:${app.properties}")
public class RequestAuthenticationService {

    private AuthService authService;

    public RequestAuthenticationService(AuthService authService) {
        this.authService = authService;
    }

    public HttpServletRequest authenticate(HttpServletRequest request) {
        return Optional.ofNullable(getCookieValue(request, AuthService.authTokenName))
                .map(authService::authenticate)
                .map(ResponseEntity::getBody)
                .map(user -> (HttpServletRequest) new AuthenticatedHttpServletRequest(request, user))
                .orElse(request);
    }

    private String getCookieValue(HttpServletRequest req, String cookieName) {
        return Optional.ofNullable(req.getCookies())
                .map(Arrays::stream)
                .orElse(Stream.empty())
                .filter(c -> c.getName().equals(cookieName))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }
}
