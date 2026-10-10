package pl.student.sensors.service;

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "series with given id does not exist"));
        var sensors = new ArrayList<>(series.sensors() == null ? List.of() : series.sensors());
        if (sensors.stream()
                .anyMatch(it -> Objects.equals(input.name(), it.name()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sensor with given name already exists for this series");
        }

        var apiKey = sensorAuthenticationService.generateApiKey();
        var sensorRecord = sensorMapper.toRecord(input, UUID.randomUUID().toString(), apiKey);
        sensors.add(sensorRecord);
        seriesRepository.save(series.toBuilder()
                .sensors(sensors)
                .build());

        return sensorMapper.toCreateResponse(sensorRecord, series.id(), apiKey);
    }
}
