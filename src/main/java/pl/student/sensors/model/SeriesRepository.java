package pl.student.sensors.model;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SeriesRepository extends MongoRepository<SeriesRecord, String> {

    boolean existsByName(String name);

    Optional<SeriesRecord> findBySensors_id(String sensorId);

    Optional<SeriesRecord> findBySensors_hashedApiKey(String sensorId);
}
