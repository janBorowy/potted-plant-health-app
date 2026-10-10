package pl.student.sensors.service;

import java.security.SecureRandom;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SensorAuthenticationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    public String generateApiKey() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public String hashApiKey(String apiKey) {
        return DigestUtils.sha256Hex(apiKey);
    }
}
