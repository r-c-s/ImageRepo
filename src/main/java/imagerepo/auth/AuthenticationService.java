package imagerepo.auth;

import imagerepo.auth.models.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@PropertySource("file:${app.properties}")
public class AuthenticationService {

    public static final String authTokenName = "JSESSIONID";

    private final RestTemplate restTemplate;
    private final String authServiceAuthenticateUrl;

    public AuthenticationService(
            RestTemplate restTemplate,
            @Value("${services.auth.authenticate}") String authServiceAuthenticateUrl) {
        this.restTemplate = restTemplate;
        this.authServiceAuthenticateUrl = authServiceAuthenticateUrl;
    }

    public HttpServletRequest authenticate(HttpServletRequest request) {
        return Optional.ofNullable(getCookieValue(request, authTokenName))
                .map(this::buildRequestEntity)
                .map(requestEntity -> restTemplate.exchange(
                        authServiceAuthenticateUrl,
                        HttpMethod.GET,
                        requestEntity,
                        AuthenticatedUser.class))
                .map(ResponseEntity::getBody)
                .map(user -> (HttpServletRequest) new AuthenticatedHttpServletRequest(request, user))
                .orElse(request);
    }

    private HttpEntity buildRequestEntity(String authTokenValue) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Cookie", authTokenName + "=" + authTokenValue);
        return new HttpEntity(null, requestHeaders);
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
