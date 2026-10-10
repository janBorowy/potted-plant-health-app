package pl.student.sensors.model;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MeasurementRepository extends MongoRepository<MeasurementRecord, String> {
}
