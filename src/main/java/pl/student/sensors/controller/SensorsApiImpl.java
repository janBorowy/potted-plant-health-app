package pl.student.sensors.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.student.sensors.generated.api.SensorsApi;
import pl.student.sensors.generated.model.Sensor;
import pl.student.sensors.generated.model.SensorCreated;
import pl.student.sensors.generated.model.SensorInput;
import pl.student.sensors.mapper.SensorMapper;
import pl.student.sensors.service.SensorService;

@Controller
@RequiredArgsConstructor
public class SensorsApiImpl implements SensorsApi {

    private final SensorMapper sensorMapper;
    private final SensorService sensorService;

    @Override
    public ResponseEntity<Void> deleteSensor(String sensorId) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<List<Sensor>> getAllSensors() {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<Sensor> getSensorById(String sensorId) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
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
