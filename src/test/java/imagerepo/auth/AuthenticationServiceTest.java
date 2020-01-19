package imagerepo.auth;

import imagerepo.auth.exceptions.UnauthorizedException;
import imagerepo.auth.models.AuthenticatedUser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class AuthenticationServiceTest {

    private RestTemplate restTemplate;
    private String authServiceUrl;
    private AuthenticationService target;

    @Before
    public void setup() {
        restTemplate = mock(RestTemplate.class);
        authServiceUrl = "auth-service-url";
        target = new AuthenticationService(restTemplate, authServiceUrl);
    }

    @Test
    public void testAuthenticate() {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies())
                .thenReturn(new Cookie[]{ new Cookie("JSESSIONID", "123456789")});

        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(restTemplate.exchange(eq(authServiceUrl), eq(HttpMethod.GET), any(), eq(AuthenticatedUser.class)))
                .thenReturn(ResponseEntity.ok().body(user));

        // Act
        AuthenticatedHttpServletRequest actual = target.authenticate(request);

        // Assert
        assertThat(actual).isExactlyInstanceOf(AuthenticatedHttpServletRequest.class);
        assertThat(actual.getLoggedInUser()).isEqualTo(user);
    }

    @Test
    public void testAuthenticateNoCookies() {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies())
                .thenReturn(null);

        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(restTemplate.exchange(eq(authServiceUrl), eq(HttpMethod.GET), any(), eq(AuthenticatedUser.class)))
                .thenReturn(ResponseEntity.ok().body(user));

        // Act
        AuthenticatedHttpServletRequest actual = target.authenticate(request);

        // Assert
        assertThat(actual).isExactlyInstanceOf(AuthenticatedHttpServletRequest.class);
        assertThrows(
                UnauthorizedException.class,
                actual::getLoggedInUser);
    }
}
