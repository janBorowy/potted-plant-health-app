package pl.student.sensors.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import pl.student.sensors.model.TokenRepository;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers("/error").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/series").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/series/*").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/measurements").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/measurements/*").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/measurements").permitAll()
                                .anyRequest().authenticated())
                // --- Disables stateful authentication ---
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .rememberMe(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // ----------------------------------------
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(problemJsonEntryPoint()))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(problemJsonEntryPoint()))
                .build();
    }

    // return problem details on unauthorized
    private AuthenticationEntryPoint problemJsonEntryPoint() {
        var bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();
        return (request, response, authException) -> {
            bearerEntryPoint.commence(request, response, authException);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(
                    "{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,\"instance\":\"%s\"}"
                            .formatted(request.getRequestURI().replace("\"", "")));
        };
    }

    @Bean
    JwtDecoder jwtDecoder(RSAKey rsaKey, TokenRepository tokenRepository) throws JOSEException {
        var decoder = NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
        OAuth2TokenValidator<Jwt> tokenStatusValidator = jwt -> Optional.ofNullable(jwt.getId())
                .flatMap(tokenRepository::findByJti)
                .filter(token -> !token.wasRevoked())
                .map(token -> OAuth2TokenValidatorResult.success())
                .orElseGet(() -> OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Token is revoked or unknown", null)));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                tokenStatusValidator));
        return decoder;
    }

    @Bean
    JwtEncoder jwtEncoder(RSAKey rsaKey) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    @Bean
    RSAKey rsaKey(AuthorizationServerKeysConfig keyConfig) {
        return new RSAKey
                .Builder(keyConfig.getPublicKey())
                .privateKey(keyConfig.getPrivateKey())
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
