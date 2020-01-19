package imagerepo.auth;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AuthenticationFilterTest {

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AuthenticationFilter target;

    @Test
    public void testDoFilter() throws IOException, ServletException {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        AuthenticatedHttpServletRequest authenticatedRequest = mock(AuthenticatedHttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(authenticationService.authenticate(request))
                .thenReturn(authenticatedRequest);

        // Act
        target.doFilter(request, response, chain);

        // Assert
        verify(chain).doFilter(authenticatedRequest, response);
    }
}
