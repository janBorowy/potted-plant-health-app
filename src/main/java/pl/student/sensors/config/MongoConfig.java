package pl.student.sensors.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

@Configuration
public class MongoConfig {

    @Bean
    public ApplicationRunner setTypeMapper(MappingMongoConverter mappingMongoConverter) {
        return args -> mappingMongoConverter.setTypeMapper(new DefaultMongoTypeMapper(null));
    }
}
