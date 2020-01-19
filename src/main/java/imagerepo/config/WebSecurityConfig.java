package imagerepo.config;

import imagerepo.auth.AuthenticationFilter;
import imagerepo.auth.EndpointSecurity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private EndpointSecurity endpointSecurity;

    @Autowired
    private AuthenticationFilter authenticationFilter;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf()
                .disable()
                .exceptionHandling()
                .and()
                .authorizeRequests()

                .antMatchers(HttpMethod.GET, "/api/images")
                .permitAll()

                .antMatchers(HttpMethod.GET, "/api/images/{name}")
                .permitAll()

                .antMatchers(HttpMethod.POST, "/api/images")
                .access("@endpointSecurity.isLoggedIn(request)")

                .antMatchers(HttpMethod.DELETE, "/api/images/{name}")
                .access("@endpointSecurity.canDeleteImage(request, #name)")

                .and()
                .addFilterBefore(authenticationFilter, BasicAuthenticationFilter.class)
                .formLogin()
                .disable();
    }
}