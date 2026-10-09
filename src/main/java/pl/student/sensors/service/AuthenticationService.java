package pl.student.sensors.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pl.student.sensors.dto.AuthenticationInput;
import pl.student.sensors.dto.ChangePasswordInput;
import pl.student.sensors.dto.JwtDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsManager userDetailsManager;

    public JwtDto authenticate(AuthenticationInput input) {
        final UserDetails userDetails;
        try {
            userDetails = userDetailsManager.loadUserByUsername(input.username());
        } catch (UsernameNotFoundException e) {
            log.error("User not found, username={}", input.username());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        if (!passwordEncoder.matches(input.password(), userDetails.getPassword())) {
            log.error("Password mismatch, username={}", input.username());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        log.info("User logged in, username={}", input.username());
        return tokenService.generateToken(userDetails.getUsername());
    }

    public void changePassword(ChangePasswordInput input) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        var user = Optional.ofNullable(authentication.getName())
                .map(userDetailsManager::loadUserByUsername)
                .orElseThrow(() -> new RuntimeException("Authenticated user does not exists in db"));

        if (!passwordEncoder.matches(input.oldPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        userDetailsManager.updateUser(
                User.withUsername(user.getUsername())
                        .password(passwordEncoder.encode(input.newPassword()))
                        .build()
        );
    }

    public void logout() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            tokenService.revokeToken(jwtAuth.getToken().getId());
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
