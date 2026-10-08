package pl.student.sensors.dto;

import lombok.Builder;

@Builder
public record JwtDto(
        String accessToken,
        int expiresInSeconds
) {
}
