package pl.student.sensors.dto;

public record Series(
    String id,
    String name,
    Double minValue,
    Double maxValue,
    String color,
    String icon,
    String unit
) {}
