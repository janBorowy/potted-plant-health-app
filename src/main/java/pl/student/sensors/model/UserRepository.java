package pl.student.sensors.model;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<UserRecord, String> {

    boolean existsByUsername(String username);

    Optional<UserRecord> findByUsername(String username);

}
