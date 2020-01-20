package imagerepo.auth;

import com.google.common.collect.ImmutableList;
import imagerepo.auth.exceptions.UnauthorizedException;
import imagerepo.auth.models.AuthenticatedUser;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnitParamsRunner.class)
public class AuthUtilsTest {

    private AuthUtils target;

    @Before
    public void setup() {
        target = new AuthUtils();
    }

    @Test
    public void testTryGetLoggedInUserRequestIsAuthenticated() {
        // Arrange
        AuthenticatedHttpServletRequest authenticatedRequest = mock(AuthenticatedHttpServletRequest.class);
        AuthenticatedUser loggedInUser = mock(AuthenticatedUser.class);

        when(authenticatedRequest.getLoggedInUser())
                .thenReturn(loggedInUser);

        // Act
        AuthenticatedUser actual = target.tryGetLoggedInUser(authenticatedRequest);

        // Assert
        assertThat(actual).isSameAs(loggedInUser);
    }

    @Test
    public void testTryGetLoggedInUserRequestIsWrapperAndDelegateIsAuthenticated() {
        // Arrange
        HttpServletRequestWrapper request = mock(HttpServletRequestWrapper.class);
        AuthenticatedHttpServletRequest authenticatedRequest = mock(AuthenticatedHttpServletRequest.class);
        AuthenticatedUser loggedInUser = mock(AuthenticatedUser.class);

        when(request.getRequest())
                .thenReturn(authenticatedRequest);

        when(authenticatedRequest.getLoggedInUser())
                .thenReturn(loggedInUser);

        // Act
        AuthenticatedUser actual = target.tryGetLoggedInUser(request);

        // Assert
        assertThat(actual).isSameAs(loggedInUser);
    }

    @Test
    public void testTryGetLoggedInUserRequestIsWrapperAndDelegateIsNotAuthenticated() {
        // Arrange
        HttpServletRequestWrapper request = mock(HttpServletRequestWrapper.class);
        HttpServletRequest delegateRequest = mock(HttpServletRequest.class);
        AuthenticatedUser loggedInUser = mock(AuthenticatedUser.class);

        when(request.getRequest())
                .thenReturn(delegateRequest);

        // Act & assert
        assertThrows(
                UnauthorizedException.class,
                () -> target.tryGetLoggedInUser(request));
    }

    @Test
    public void testTryGetLoggedInUserRequestIsNotAuthenticated() {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);

        // Act & assert
        assertThrows(
                UnauthorizedException.class,
                () -> target.tryGetLoggedInUser(request));
    }

    @Test
    @Parameters({
            "ADMIN | true",
            "USER | false"
    })
    public void testIsAdmin(String role, boolean expected) {
        // Arrange
        AuthenticatedUser user = new AuthenticatedUser(
                "username",
                ImmutableList.of(role, "OTHER"));

        // Act
        boolean actual = target.isAdmin(user);

        // Assert
        assertThat(actual).isEqualTo(expected);
    }
}