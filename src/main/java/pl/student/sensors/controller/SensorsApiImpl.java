package pl.student.sensors.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.student.sensors.generated.api.SensorsApi;
import pl.student.sensors.generated.model.Sensor;
import pl.student.sensors.generated.model.SensorCreated;
import pl.student.sensors.generated.model.SensorInput;
import pl.student.sensors.mapper.SensorMapper;
import pl.student.sensors.service.SensorService;

@RestController
@RequiredArgsConstructor
public class SensorsApiImpl implements SensorsApi {

    private final SensorMapper sensorMapper;
    private final SensorService sensorService;

    @Override
    public ResponseEntity<Void> deleteSensor(String sensorId) {
        sensorService.deleteSensorById(sensorId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<Sensor>> getAllSensors() {
        var sensors = sensorService.getAllSensors();
        return ResponseEntity.ok().body(
                sensors.stream()
                        .map(sensorMapper::toApiResponse)
                        .toList()
        );
    }

    @Override
    public ResponseEntity<Sensor> getSensorById(String sensorId) {
        var sensor = sensorService.getSensorById(sensorId);
        return ResponseEntity.ok().body(sensorMapper.toApiResponse(sensor));
    }

    @Override
    public ResponseEntity<SensorCreated> registerSensor(SensorInput sensorInput) {
        var input = sensorMapper.toCreateSensorInput(sensorInput);
        var response = sensorService.createSensor(input);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .scheme(null)
                .host(null)
                .port(null)
                .path("/{id}")
                .buildAndExpand(response.sensor().id())
                .toUri();
        return ResponseEntity.created(location).body(sensorMapper.toApiResponse(response));
    }
}
