package imagerepo.auth.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * TODO: this should come as a maven dependency from Auth service
 */
@Getter
@AllArgsConstructor
public class LoginCredentials {
    private String username;
    private String password;
}