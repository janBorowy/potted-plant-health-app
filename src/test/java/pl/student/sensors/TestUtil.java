package pl.student.sensors;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import lombok.experimental.UtilityClass;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

@UtilityClass
public class TestUtil {

    public static final OpenApiInteractionValidator OPEN_API_VALIDATOR =
            OpenApiInteractionValidator.createFor("api-specification.yaml")
                    .withLevelResolver(LevelResolver.create()
                            // @WithMockUser no auth header
                            .withLevel("validation.request.security.missing", ValidationReport.Level.IGNORE)
                            .withLevel("validation.schema.additionalProperties", ValidationReport.Level.IGNORE)
                            .build())
                    .build();

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
