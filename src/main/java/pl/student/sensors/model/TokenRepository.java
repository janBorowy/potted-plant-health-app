package pl.student.sensors.model;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TokenRepository extends MongoRepository<TokenRecord, String> {

    Optional<TokenRecord> findByJti(String jti);
}
