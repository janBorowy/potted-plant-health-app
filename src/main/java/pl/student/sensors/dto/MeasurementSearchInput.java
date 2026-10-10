package pl.student.sensors.dto;

import jakarta.annotation.Nullable;
import java.time.OffsetDateTime;
import pl.student.sensors.generated.model.MeasurementSort;

public record MeasurementSearchInput(
        @Nullable String seriesCommandSeparatedIds,
        @Nullable OffsetDateTime from,
        @Nullable OffsetDateTime to,
        MeasurementSort sort,
        Integer limit) {
}
