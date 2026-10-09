package pl.student.sensors.model;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("jwt_token")
@Builder(toBuilder = true)
public record TokenRecord(
        @Id String id,
        @Indexed(unique = true) String jti,
        boolean wasRevoked
) {
}
