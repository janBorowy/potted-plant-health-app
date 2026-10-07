package pl.student.sensors.controller;

import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import pl.student.sensors.generated.api.MeasurementsApi;
import pl.student.sensors.generated.model.Id;
import pl.student.sensors.generated.model.Measurement;
import pl.student.sensors.generated.model.MeasurementInput;

public class MeasurementsApiImpl implements MeasurementsApi {

    @Override
    public ResponseEntity<Measurement> getMeasurementById(Id measurementId) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<Measurement> postMeasurement(MeasurementInput measurementInput) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<List<Measurement>> searchMeasurements(@Nullable String series, @Nullable OffsetDateTime from, @Nullable OffsetDateTime to,
                                                                String sort, Integer limit) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }

}
