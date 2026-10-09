package pl.student.sensors.model;

import java.util.List;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("series")
@Builder(toBuilder = true)
public record SeriesRecord(
        @Id String id,
        @Indexed(unique = true) String name,
        Double minValue,
        Double maxValue,
        String color,
        String icon,
        String unit,
        List<SensorRecord> sensors
) {
}
