package pl.student.sensors.service;

import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pl.student.sensors.dto.CreateMeasurementInput;
import pl.student.sensors.dto.Measurement;
import pl.student.sensors.mapper.MeasurementMapper;
import pl.student.sensors.model.MeasurementRepository;
import pl.student.sensors.model.SeriesRecord;
import pl.student.sensors.model.SeriesRepository;

@Service
public class MeasurementService {

    private final SeriesRepository seriesRepository;
    private final SensorAuthenticationService sensorAuthenticationService;
    private final MeasurementMapper measurementMapper;
    private final MeasurementRepository measurementRepository;

    public MeasurementService(SeriesRepository seriesRepository, SensorAuthenticationService sensorAuthenticationService,
                              MeasurementMapper measurementMapper, MeasurementRepository measurementRepository) {
        this.seriesRepository = seriesRepository;
        this.sensorAuthenticationService = sensorAuthenticationService;
        this.measurementMapper = measurementMapper;
        this.measurementRepository = measurementRepository;
    }

    public Measurement createMeasurement(CreateMeasurementInput input) {
        var hashedApiKey = sensorAuthenticationService.hashApiKey(input.apiKey());
        var series = seriesRepository.findBySensors_hashedApiKey(hashedApiKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        validateMeasurementValue(input.value(), series);

        var sensor = series.sensors().stream()
                .filter(it -> Objects.equals(it.hashedApiKey(), hashedApiKey))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("sensor not found - race"));
        var measurementRecord = measurementMapper.toRecord(input, series.id(), sensor.id());
        var savedRecord = measurementRepository.insert(measurementRecord);

        return measurementMapper.toDomain(savedRecord);
    }

    private void validateMeasurementValue(double value, SeriesRecord series) {
        if (value > series.maxValue() || value < series.minValue()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "value must be between %.2f and %.2f".formatted(series.minValue(), series.maxValue()));
        }
    }
}
