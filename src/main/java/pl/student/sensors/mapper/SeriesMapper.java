package pl.student.sensors.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.student.sensors.dto.CreateSeriesInput;
import pl.student.sensors.dto.Series;
import pl.student.sensors.generated.model.SeriesInput;
import pl.student.sensors.model.SeriesRecord;

@Mapper(config = MapstructConfig.class)
public interface SeriesMapper {

    CreateSeriesInput toInput(SeriesInput input);

    @Mapping(target = "id", ignore = true)
    SeriesRecord toRecord(CreateSeriesInput input);

    Series toDomain(SeriesRecord input);

    pl.student.sensors.generated.model.Series toApiResponse(Series domain);
}
