package pl.student.sensors.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.student.sensors.generated.api.SeriesApi;
import pl.student.sensors.generated.model.Series;
import pl.student.sensors.generated.model.SeriesInput;
import pl.student.sensors.mapper.SeriesMapper;
import pl.student.sensors.service.SeriesService;

@RestController
@RequiredArgsConstructor
public class SeriesApiImpl implements SeriesApi {

    private final SeriesService seriesService;
    private final SeriesMapper seriesMapper;

    @Override
    public ResponseEntity<Series> createSeries(SeriesInput seriesInput) {
        var input = seriesMapper.toInput(seriesInput);
        var series = seriesService.createSeries(input);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .scheme(null)
                .host(null)
                .port(null)
                .path("/{id}")
                .buildAndExpand(series.id())
                .toUri();
        return ResponseEntity.created(location).body(seriesMapper.toApiResponse(series));
    }

    @Override
    public ResponseEntity<Void> deleteSeries(String seriesId) {
        seriesService.deleteSeries(seriesId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<Series>> getAllSeries() {
        var series = seriesService.getAllSeries().stream()
                .map(seriesMapper::toApiResponse)
                .toList();
        return ResponseEntity.ok(series);
    }

    @Override
    public ResponseEntity<Series> getSeriesById(String seriesId) {
        var series = seriesService.getSeriesById(seriesId);
        return ResponseEntity.ok(seriesMapper.toApiResponse(series));
    }

    @Override
    public ResponseEntity<Series> replaceSeries(String seriesId, SeriesInput seriesInput) {
        // must check whether the values already stored in measurements exceed given
        // minValue-maxValue, so I'm leaving this unimplemented until those are done
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }
}
