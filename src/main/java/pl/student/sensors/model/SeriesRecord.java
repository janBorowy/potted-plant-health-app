package pl.student.sensors.model;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("series")
@Builder
public record SeriesRecord(
        @Id String id,
        String name,
        Double minValue,
        Double maxValue,
        String color,
        String icon,
        String unit
) {}
