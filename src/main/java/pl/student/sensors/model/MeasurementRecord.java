package pl.student.sensors.model;

import java.time.Instant;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.TimeSeries;
import org.springframework.data.mongodb.core.timeseries.Granularity;

@Document("measurement")
@TimeSeries(
        timeField = "timestamp",
        metaField = "metadata",
        granularity = Granularity.SECONDS
)
@Builder
public record MeasurementRecord(
        @Id String id,
        Instant timestamp,
        MeasurementMetadata metadata,
        Double value
) {
    @Builder
    public record MeasurementMetadata(String seriesId, String sensorId) {
    }
}
