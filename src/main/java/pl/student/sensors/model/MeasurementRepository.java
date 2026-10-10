package pl.student.sensors.model;

import org.springframework.data.mongodb.repository.ExistsQuery;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MeasurementRepository extends MongoRepository<MeasurementRecord, String> {

    @ExistsQuery("{ 'metadata.seriesId': ?0, $or: [ { 'value': { $lt: ?1 } }, { 'value': { $gt: ?2 } } ] }")
    boolean existsByMetadataSeriesIdAndValueOutsideRange(String seriesId, double minValue, double maxValue);
}
