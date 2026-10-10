package pl.student.sensors.model;

import jakarta.annotation.Nonnull;
import java.time.Instant;
import lombok.Builder;

@Builder(toBuilder = true)
public record SensorRecord(
        @Nonnull String id,
        @Nonnull String name,
        @Nonnull String hashedApiKey,
        @Nonnull Instant createdAt,
        Instant lastMeasurementAt
) {
}