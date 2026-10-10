package pl.student.sensors.mapper;

import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import pl.student.sensors.dto.CreateSeriesInput;
import pl.student.sensors.dto.Series;
import pl.student.sensors.generated.model.SeriesInput;
import pl.student.sensors.model.SeriesRecord;

@Mapper(config = MapstructConfig.class)
public abstract class SeriesMapper {

    public abstract CreateSeriesInput toInput(SeriesInput input);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sensors", ignore = true)
    public abstract SeriesRecord toRecord(CreateSeriesInput input);

    @AfterMapping
    protected void toRecordAfterMapping(CreateSeriesInput source, @MappingTarget SeriesRecord.SeriesRecordBuilder builder) {
        builder.sensors(List.of());
    }

    public abstract Series toDomain(SeriesRecord input);

    public abstract pl.student.sensors.generated.model.Series toApiResponse(Series domain);
}
