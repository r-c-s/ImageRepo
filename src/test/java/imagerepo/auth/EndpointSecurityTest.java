package imagerepo.auth;

import imagerepo.auth.models.AuthenticatedUser;
import imagerepo.models.ImageRecord;
import imagerepo.repositories.ImageRecordsRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.ServletRequest;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EndpointSecurityTest {

    @Mock
    private AuthUtils authUtils;

    @Mock
    private ImageRecordsRepository repository;

    @InjectMocks
    private EndpointSecurity target;

    @Test
    public void testIsLoggedIn() {
        // Arrange
        ServletRequest request = mock(ServletRequest.class);

        when(authUtils.tryGetLoggedInUser(request))
                .thenReturn(Optional.of(mock(AuthenticatedUser.class)));

        // Act
        boolean actual = target.isLoggedIn(request);

        // Assert
        assertThat(actual).isTrue();
    }

    @Test
    public void testIsLoggedInFails() {
        // Arrange
        ServletRequest request = mock(ServletRequest.class);

        when(authUtils.tryGetLoggedInUser(request))
                .thenReturn(Optional.empty());

        // Act
        boolean actual = target.isLoggedIn(request);

        // Assert
        assertThat(actual).isFalse();
    }

    @Test
    public void testCanDeleteImageUserIsAdmin() {
        // Arrange
        ServletRequest request = mock(ServletRequest.class);
        String imagename = "name";

        AuthenticatedUser user = mock(AuthenticatedUser.class);

        when(authUtils.tryGetLoggedInUser(request))
                .thenReturn(Optional.of(user));

        when(authUtils.isAdmin(user))
                .thenReturn(true);

        // Act
        boolean actual = target.canDeleteImage(request, imagename);

        // Assert
        assertThat(actual).isTrue();
    }

    @Test
    public void testCanDeleteImageUserOwnsImage() {
        // Arrange
        ServletRequest request = mock(ServletRequest.class);
        String imagename = "name";

        AuthenticatedUser user = new AuthenticatedUser("username", null);

        when(authUtils.tryGetLoggedInUser(request))
                .thenReturn(Optional.of(user));

        when(authUtils.isAdmin(user))
                .thenReturn(false);

        when(repository.findByName(imagename))
                .thenReturn(new ImageRecord(null, null, user.getUsername(), null, null, null));

        // Act
        boolean actual = target.canDeleteImage(request, imagename);

        // Assert
        assertThat(actual).isTrue();
    }

    @Test
    public void testCanDeleteImageNotLoggedIn() {
        // Arrange
        ServletRequest request = mock(ServletRequest.class);
        String imagename = "name";

        when(authUtils.tryGetLoggedInUser(request))
                .thenReturn(Optional.empty());

        // Act
        boolean actual = target.canDeleteImage(request, imagename);

        // Assert
        assertThat(actual).isFalse();
    }
}
