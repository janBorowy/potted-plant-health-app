package pl.student.sensors.model;

import jakarta.annotation.Nonnull;
import java.time.Instant;
import lombok.Builder;

@Builder
public record SensorRecord(
        @Nonnull String id,
        @Nonnull String name,
        @Nonnull Instant createdAt,
        Instant lastMeasurementAt
) {
}