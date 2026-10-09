package pl.student.sensors.mapper;

import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import pl.student.sensors.dto.CreateSensorInput;
import pl.student.sensors.dto.CreateSeriesInput;
import pl.student.sensors.dto.Series;
import pl.student.sensors.generated.model.SeriesInput;
import pl.student.sensors.model.SeriesRecord;

@Mapper(config = MapstructConfig.class)
public interface SeriesMapper {

    CreateSeriesInput toInput(SeriesInput input);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sensors", ignore = true)
    SeriesRecord toRecord(CreateSeriesInput input);

    @AfterMapping
    default void toRecordAfterMapping(CreateSensorInput source, @MappingTarget SeriesRecord.SeriesRecordBuilder builder) {
        builder.sensors(List.of());
    }

    Series toDomain(SeriesRecord input);

    pl.student.sensors.generated.model.Series toApiResponse(Series domain);
}
