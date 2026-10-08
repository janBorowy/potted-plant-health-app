package pl.student.sensors.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import pl.student.sensors.generated.api.SensorsApi;
import pl.student.sensors.generated.model.Sensor;
import pl.student.sensors.generated.model.SensorCreated;
import pl.student.sensors.generated.model.SensorInput;

public class SensorsApiImpl implements SensorsApi {
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
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }
}
