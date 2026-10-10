package pl.student.sensors.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.student.sensors.dto.CreateSensorInput;
import pl.student.sensors.dto.CreateSensorResponse;
import pl.student.sensors.dto.Sensor;
import pl.student.sensors.generated.model.SensorCreated;
import pl.student.sensors.generated.model.SensorInput;
import pl.student.sensors.model.SensorRecord;
import pl.student.sensors.service.SensorAuthenticationService;

@Component
@Mapper(config = MapstructConfig.class,
        uses = DateMapperHelper.class)
public abstract class SensorMapper {

    @Autowired
    protected SensorAuthenticationService sensorAuthenticationService;

    public abstract CreateSensorInput toCreateSensorInput(SensorInput api);

    @Mapping(target = "hashedApiKey", expression = "java(sensorAuthenticationService.hashApiKey(apiKey))")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "lastMeasurementAt", ignore = true)
    public abstract SensorRecord toRecord(CreateSensorInput input, String id, String apiKey);

    public abstract Sensor toDomain(SensorRecord sensorRecord, String seriesId);

    @Mapping(target = "sensor", expression = "java(toDomain(sensorRecord, seriesId))")
    public abstract CreateSensorResponse toCreateResponse(SensorRecord sensorRecord, String seriesId, String apiKey);

    @Mapping(target = "id", source = "sensor.id")
    @Mapping(target = "name", source = "sensor.name")
    @Mapping(target = "seriesId", source = "sensor.seriesId")
    @Mapping(target = "createdAt", source = "sensor.createdAt")
    @Mapping(target = "lastMeasurementAt", source = "sensor.lastMeasurementAt")
    public abstract SensorCreated toApiResponse(CreateSensorResponse sensor);

    public abstract pl.student.sensors.generated.model.Sensor toApiResponse(Sensor sensor);
}
