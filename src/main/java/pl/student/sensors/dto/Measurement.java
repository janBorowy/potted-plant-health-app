package pl.student.sensors.dto;

import jakarta.annotation.Nonnull;
import java.time.Instant;

public record Measurement(
        @Nonnull String id,
        @Nonnull String seriesId,
        @Nonnull String sensorId,
        @Nonnull Double value,
        @Nonnull Instant timestamp
) {
}
