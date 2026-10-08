package pl.student.sensors.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import pl.student.sensors.config.AppProperties;
import pl.student.sensors.dto.JwtDto;

@Service
@RequiredArgsConstructor
public class TokenService {

    @Value("${app.token-expiry-minutes:60}")
    private int tokenExpiryMinutes;

    private final JwtEncoder jwtEncoder;
    private final AppProperties appProperties;

    public JwtDto generateToken(String subject) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .subject(subject)
                .issuer(appProperties.getUrl())
                .issuedAt(now)
                .expiresAt(now.plus(tokenExpiryMinutes, ChronoUnit.MINUTES))
                .build();
        return JwtDto.builder()
                .accessToken(jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue())
                .expiresInSeconds(tokenExpiryMinutes * 60)
                .build();
    }
}
