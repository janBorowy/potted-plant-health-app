package pl.student.sensors.model;

import jakarta.annotation.Nonnull;
import java.util.List;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("seriesCommandSeparatedIds")
@Builder(toBuilder = true)
@CompoundIndex(
        name = "series_sensor_id_idx",
        def = "{'sensors.id': 1}"
)
@CompoundIndex(
        name = "series_sensor_hashedApiKey_idx",
        def = "{'sensors.hashedApiKey': 1}"
)
public record SeriesRecord(
        @Id String id,
        @Indexed(unique = true) String name,
        double minValue,
        double maxValue,
        @Nonnull String color,
        String icon,
        String unit,
        @Nonnull List<SensorRecord> sensors
) {
}
