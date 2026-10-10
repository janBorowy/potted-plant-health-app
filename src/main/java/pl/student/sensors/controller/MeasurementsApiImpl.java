package pl.student.sensors.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.student.sensors.generated.api.MeasurementsApi;
import pl.student.sensors.generated.model.Measurement;
import pl.student.sensors.generated.model.MeasurementInput;
import pl.student.sensors.mapper.MeasurementMapper;
import pl.student.sensors.service.MeasurementService;

@RestController
@RequiredArgsConstructor
public class MeasurementsApiImpl implements MeasurementsApi {

    private final HttpServletRequest request;
    private final MeasurementService measurementService;
    private final MeasurementMapper measurementMapper;

    @Override
    public ResponseEntity<Measurement> getMeasurementById(String measurementId) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<Measurement> postMeasurement(MeasurementInput measurementInput) {
        var apiKey = request.getHeader("x-api-key");
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        var input = measurementMapper.toCreateInput(measurementInput, apiKey);
        var domain = measurementService.createMeasurement(input);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .scheme(null)
                .host(null)
                .port(null)
                .path("/{id}")
                .buildAndExpand(domain.id())
                .toUri();
        return ResponseEntity.created(location)
                .body(measurementMapper.toApiResponse(domain));
    }

    @Override
    public ResponseEntity<List<Measurement>> searchMeasurements(@Nullable String series, @Nullable OffsetDateTime from, @Nullable OffsetDateTime to,
                                                                String sort, Integer limit) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }

}
