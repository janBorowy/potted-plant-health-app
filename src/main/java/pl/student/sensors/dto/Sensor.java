package pl.student.sensors.dto;

import java.time.Instant;

public record Sensor(
        String id,
        String name,
        String seriesId,
        Instant createdAt,
        Instant lastMeasurementAt
) {
}
