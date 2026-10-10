package pl.student.sensors.dto;

import jakarta.annotation.Nonnull;
import java.time.Instant;

public record CreateMeasurementInput(
        @Nonnull String apiKey,
        double value,
        Instant timestamp
) {
}
