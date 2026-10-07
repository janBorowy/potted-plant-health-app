package pl.student.sensors.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import pl.student.sensors.generated.api.SeriesApi;
import pl.student.sensors.generated.model.Id;
import pl.student.sensors.generated.model.Series;
import pl.student.sensors.generated.model.SeriesInput;

public class SeriesApiImpl implements SeriesApi {

    @Override
    public ResponseEntity<Series> createSeries(SeriesInput seriesInput) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<Void> deleteSeries(Id seriesId) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<List<Series>> getAllSeries() {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<Series> getSeriesById(Id seriesId) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<Series> replaceSeries(Id seriesId, SeriesInput seriesInput) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }
}
