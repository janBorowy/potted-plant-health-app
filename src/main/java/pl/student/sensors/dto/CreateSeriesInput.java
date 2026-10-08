package pl.student.sensors.dto;

public record CreateSeriesInput(
        String name,
        Double minValue,
        Double maxValue,
        String color,
        String icon,
        String unit
) {}
