package pl.student.sensors.model;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SeriesRepository extends MongoRepository<SeriesRecord, String> {

    boolean existsByName(String name);
}
