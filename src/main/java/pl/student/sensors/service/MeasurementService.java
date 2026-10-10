package pl.student.sensors.service;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pl.student.sensors.dto.CreateMeasurementInput;
import pl.student.sensors.dto.Measurement;
import pl.student.sensors.dto.MeasurementSearchInput;
import pl.student.sensors.generated.model.MeasurementSort;
import pl.student.sensors.mapper.MeasurementMapper;
import pl.student.sensors.model.MeasurementRecord;
import pl.student.sensors.model.MeasurementRepository;
import pl.student.sensors.model.SeriesRecord;
import pl.student.sensors.model.SeriesRepository;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Service
@RequiredArgsConstructor
public class MeasurementService {

    private final SeriesRepository seriesRepository;
    private final SensorAuthenticationService sensorAuthenticationService;
    private final MeasurementMapper measurementMapper;
    private final MeasurementRepository measurementRepository;
    private final MongoTemplate mongoTemplate;

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

    public Measurement getMeasurementById(@Nonnull String id) {
        return measurementRepository.findById(id)
                .map(measurementMapper::toDomain)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "measurement with given id was not found"));
    }

    public List<Measurement> searchMeasurements(MeasurementSearchInput input) {
        ArrayList<Criteria> criteria = new ArrayList<>();

        if (Strings.isNotBlank(input.seriesCommandSeparatedIds())) {
            var seriesIds = getSeriesFromCommaSeparatedList(input.seriesCommandSeparatedIds());
            criteria.add(where("metadata.seriesId").in(seriesIds));
        }

        if (input.from() != null) {
            criteria.add(where("timestamp").gte(input.from().toInstant()));
        }

        if (input.to() != null) {
            criteria.add(where("timestamp").lte(input.to().toInstant()));
        }

        var measurementRecords = mongoTemplate.find(query(new Criteria().andOperator(criteria))
                        .with(Sort.by(input.sort() == MeasurementSort._TIMESTAMP ? Sort.Direction.DESC : Sort.Direction.ASC, "timestamp"))
                        .limit(input.limit())
                , MeasurementRecord.class);

        return measurementRecords.stream()
                .map(measurementMapper::toDomain)
                .toList();
    }

    private void validateMeasurementValue(double value, SeriesRecord series) {
        if (value > series.maxValue() || value < series.minValue()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "value must be between %.2f and %.2f".formatted(series.minValue(), series.maxValue()));
        }
    }

    private List<String> getSeriesFromCommaSeparatedList(String seriesList) {
        return Arrays.stream(seriesList.split(",")).filter(Strings::isNotBlank).toList();
    }
}
