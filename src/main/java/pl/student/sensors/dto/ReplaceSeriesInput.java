package pl.student.sensors.dto;

import jakarta.annotation.Nonnull;

public record ReplaceSeriesInput(
        @Nonnull String id,
        @Nonnull String name,
        double minValue,
        double maxValue,
        @Nonnull String color,
        String icon,
        String unit
) {
}
