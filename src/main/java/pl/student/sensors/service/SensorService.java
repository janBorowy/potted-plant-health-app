package pl.student.sensors.service;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pl.student.sensors.dto.CreateSensorInput;
import pl.student.sensors.dto.CreateSensorResponse;
import pl.student.sensors.dto.Sensor;
import pl.student.sensors.mapper.SensorMapper;
import pl.student.sensors.model.SeriesRepository;

@Service
@RequiredArgsConstructor
public class SensorService {

    private final SensorMapper sensorMapper;
    private final SeriesRepository seriesRepository;
    private final SensorAuthenticationService sensorAuthenticationService;

    public CreateSensorResponse createSensor(CreateSensorInput input) {
        var series = seriesRepository.findById(input.seriesId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "seriesCommandSeparatedIds with given id does not exist"));
        var sensors = new ArrayList<>(series.sensors() == null ? List.of() : series.sensors());
        if (sensors.stream()
                .anyMatch(it -> Objects.equals(input.name(), it.name()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sensor with given name already exists for this seriesCommandSeparatedIds");
        }

        var apiKey = sensorAuthenticationService.generateApiKey();
        var sensorRecord = sensorMapper.toRecord(input, UUID.randomUUID().toString(), apiKey);
        sensors.add(sensorRecord);
        seriesRepository.save(series.toBuilder()
                .sensors(sensors)
                .build());

        return sensorMapper.toCreateResponse(sensorRecord, series.id(), apiKey);
    }

    public Sensor getSensorById(@Nonnull String id) {
        return seriesRepository.findBySensors_id(id)
                .flatMap(series -> series.sensors().stream()
                        .filter(sensor -> Objects.equals(sensor.id(), id))
                        .map(sensor -> sensorMapper.toDomain(sensor, series.id()))
                        .findFirst()
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public List<Sensor> getAllSensors() {
        return seriesRepository.findAll().stream()
                .flatMap(series -> series.sensors().stream()
                        .map(sensor -> sensorMapper.toDomain(sensor, series.id())))
                .toList();
    }

    public void deleteSensorById(@Nonnull String sensorId) {
        var series = seriesRepository.findBySensors_id(sensorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "sensor does not exist"));
        var sensors = new ArrayList<>(series.sensors() == null ? List.of() : series.sensors());
        sensors.removeIf(it -> Objects.equals(it.id(), sensorId));
        seriesRepository.save(series.toBuilder()
                .sensors(sensors)
                .build());
    }
}
