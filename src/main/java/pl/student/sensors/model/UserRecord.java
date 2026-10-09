package pl.student.sensors.model;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("user")
@Builder
public record UserRecord(
        @Id String id,
        String username,
        String hashedPassword) {
}

