package pl.student.sensors.mapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import pl.student.sensors.dto.CreateMeasurementInput;
import pl.student.sensors.dto.Measurement;
import pl.student.sensors.dto.MeasurementSearchInput;
import pl.student.sensors.generated.model.MeasurementInput;
import pl.student.sensors.generated.model.MeasurementSort;
import pl.student.sensors.model.MeasurementRecord;

@Mapper(config = MapstructConfig.class,
        uses = DateMapperHelper.class)
public abstract class MeasurementMapper {

    public abstract CreateMeasurementInput toCreateInput(MeasurementInput input, String apiKey);

    public abstract pl.student.sensors.generated.model.Measurement toApiResponse(Measurement domain);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timestamp", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "value", source = "input.value")
    public abstract MeasurementRecord toRecord(CreateMeasurementInput input,
                                               String seriesId,
                                               String sensorId);

    @AfterMapping
    protected void toRecordAfterMapping(CreateMeasurementInput input, String seriesId, String sensorId,
                                        @MappingTarget MeasurementRecord.MeasurementRecordBuilder builder) {
        builder.timestamp(
                Optional.ofNullable(input.timestamp())
                        .orElseGet(Instant::now)
        ).metadata(MeasurementRecord.MeasurementMetadata.builder()
                .seriesId(seriesId)
                .sensorId(sensorId)
                .build());
    }

    @Mapping(target = "seriesId", source = "metadata.seriesId")
    @Mapping(target = "sensorId", source = "metadata.sensorId")
    public abstract Measurement toDomain(MeasurementRecord measurementRecord);

    public abstract MeasurementSearchInput toSearchInput(@Nullable String seriesCommandSeparatedIds,
                                                         @Nullable OffsetDateTime from,
                                                         @Nullable OffsetDateTime to,
                                                         MeasurementSort sort,
                                                         Integer limit);
}
