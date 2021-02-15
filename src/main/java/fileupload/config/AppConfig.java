package fileupload.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import rcs.auth.api.AuthService;
import rcs.auth.api.AuthUtils;
import rcs.auth.api.AuthenticationFilter;
import rcs.auth.api.RequestAuthenticationService;

@Configuration
public class AppConfig {

    @Value("${services.auth.baseUrl}")
    private String authServiceBaseUrl;

    @Bean
    public AuthService authService() {
        return new AuthService(authServiceBaseUrl, restTemplate());
    }

    @Bean
    public AuthUtils getAuthUtils() {
        return new AuthUtils();
    }

    @Bean
    public AuthenticationFilter getAuthenticationFilter() {
        return new AuthenticationFilter(new RequestAuthenticationService(authService()));
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
