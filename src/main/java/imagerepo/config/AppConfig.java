package imagerepo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import rcs.auth.api.AuthService;

@Configuration
public class AppConfig {

    @Value("${services.auth.baseUrl}")
    private String authServiceBaseUrl;

    @Bean
    public AuthService authService() {
        return new AuthService(authServiceBaseUrl, restTemplate());
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
