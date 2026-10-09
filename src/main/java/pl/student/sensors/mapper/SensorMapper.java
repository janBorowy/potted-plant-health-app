package pl.student.sensors.mapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import pl.student.sensors.dto.CreateSensorInput;
import pl.student.sensors.dto.Sensor;
import pl.student.sensors.generated.model.SensorCreated;
import pl.student.sensors.generated.model.SensorInput;
import pl.student.sensors.model.SensorRecord;

@Mapper(config = MapstructConfig.class)
public abstract class SensorMapper {

    public abstract CreateSensorInput toCreateSensorInput(SensorInput api);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastMeasurementAt", ignore = true)
    public abstract SensorRecord toRecord(CreateSensorInput input, String id);

    @AfterMapping
    protected void afterRecordMapping(CreateSensorInput source, @MappingTarget SensorRecord.SensorRecordBuilder builder) {
        builder.createdAt(Instant.now())
                .lastMeasurementAt(null);
    }

    public abstract Sensor toDomain(SensorRecord sensorRecord, String seriesId);

    // TODO: add api key handling
    @Mapping(target = "apiKey", constant = "dummyapikey")
    public abstract SensorCreated toApiResponse(Sensor sensor);

    protected OffsetDateTime toOffset(Instant i) {
        return i == null ? null : i.atOffset(ZoneOffset.UTC);
    }

    protected Instant toInstant(OffsetDateTime o) {
        return o == null ? null : o.toInstant();
    }
}
