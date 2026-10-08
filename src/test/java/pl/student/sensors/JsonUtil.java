package pl.student.sensors;

import lombok.experimental.UtilityClass;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

@UtilityClass
public class JsonUtil {

    private final ObjectMapper mapper = new ObjectMapper();

    public String toJson(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("failed writing object to json", e);
        }
    }

    public <T> T toObject(String str, Class<? extends T> clazz) {
        try {
            return mapper.readValue(str, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("failed reading object from json", e);
        }
    }
}
