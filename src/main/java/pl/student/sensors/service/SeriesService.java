package pl.student.sensors.service;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pl.student.sensors.dto.CreateSeriesInput;
import pl.student.sensors.dto.Series;
import pl.student.sensors.mapper.SeriesMapper;
import pl.student.sensors.model.SeriesRepository;

@Service
@AllArgsConstructor
@Slf4j
public class SeriesService {

    private final SeriesMapper seriesMapper;
    private final SeriesRepository seriesRepository;

    public Series createSeries(CreateSeriesInput input) {
        try {
            var seriesRecord = seriesRepository.save(seriesMapper.toRecord(input));
            return seriesMapper.toDomain(seriesRecord);
        } catch (MongoWriteException e) {
            if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "series with given name already exists");
            }
            throw e;
        }
    }
}
