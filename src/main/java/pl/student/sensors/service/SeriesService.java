package pl.student.sensors.service;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pl.student.sensors.dto.CreateSeriesInput;
import pl.student.sensors.dto.ReplaceSeriesInput;
import pl.student.sensors.dto.Series;
import pl.student.sensors.mapper.SeriesMapper;
import pl.student.sensors.model.MeasurementRepository;
import pl.student.sensors.model.SeriesRecord;
import pl.student.sensors.model.SeriesRepository;

@Service
@AllArgsConstructor
@Slf4j
public class SeriesService {

    private final SeriesMapper seriesMapper;
    private final SeriesRepository seriesRepository;
    private final MeasurementRepository measurementRepository;
    private final MappingMongoConverter mappingMongoConverter;

    public Series createSeries(CreateSeriesInput input) {
        if (seriesRepository.existsByName(input.name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "series with name '%s' already exists".formatted(input.name()));
        }

        try {
            var seriesRecord = seriesRepository.insert(seriesMapper.toRecord(input));
            return seriesMapper.toDomain(seriesRecord);
        } catch (MongoWriteException e) {
            if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "series with given name already exists");
            }
            throw e;
        }
    }

    public Series getSeriesById(String seriesId) {
        return seriesRepository.findById(seriesId)
                .map(seriesMapper::toDomain)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "series does not exist"));
    }

    public List<Series> getAllSeries() {
        return seriesRepository.findAll().stream()
                .map(seriesMapper::toDomain)
                .toList();
    }

    public void deleteSeries(String seriesId) {
        if (!seriesRepository.existsById(seriesId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "series does not exist");
        }
        seriesRepository.deleteById(seriesId);
    }

    public Series replaceSeries(ReplaceSeriesInput input) {
        var existingSeries = seriesRepository.findById(input.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "series with given id does not exist"));

        if (input.minValue() > existingSeries.minValue() || input.maxValue() < existingSeries.maxValue()) {
            validateExistingMeasurementsValues(existingSeries, input.minValue(), input.maxValue());
        }

        var savedSeries = seriesRepository.save(seriesMapper.toRecord(input, existingSeries.sensors()));

        return seriesMapper.toDomain(savedSeries);
    }

    private void validateExistingMeasurementsValues(SeriesRecord series, double minValue, double maxValue) {
        if (measurementRepository.existsByMetadataSeriesIdAndValueOutsideRange(series.id(), minValue, maxValue)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "existing measurement values would fall outside of given value range");
        }
    }
}
