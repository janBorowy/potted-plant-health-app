package pl.student.sensors;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import pl.student.sensors.generated.model.SensorInput;
import pl.student.sensors.generated.model.Series;
import pl.student.sensors.generated.model.SeriesInput;
import pl.student.sensors.model.SeriesRepository;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pl.student.sensors.TestUtil.OPEN_API_VALIDATOR;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SensorsApiIntegrationTest extends AbstractIntegrationTest {

    private static final Pattern LOCATION_PATTERN = Pattern.compile("/api/sensors/[0-9a-z-]+");
    private static final String DATE_TIME_PATTERN =
            "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:\\d{2})$";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SeriesRepository seriesRepository;

    @Test
    void registerSensorReturnsUnauthorizedWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/sensors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sensorBody("SENSOR", "seriesCommandSeparatedIds-id")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void registerSensorReturnsBadRequestWhenSeriesDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/sensors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sensorBody("SENSOR", "missing-seriesCommandSeparatedIds")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void registerSensorReturnsBadRequestWhenNameAlreadyExistsForSeries() throws Exception {
        var seriesId = createSeries();
        var body = sensorBody("DUPLICATE_SENSOR", seriesId);

        mockMvc.perform(post("/api/sensors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/sensors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void registerSensorReturnsCreatedWithDummyApiKey() throws Exception {
        var seriesId = createSeries();

        mockMvc.perform(post("/api/sensors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sensorBody("NEW_SENSOR", seriesId)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(LOCATION_PATTERN)))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("NEW_SENSOR"))
                .andExpect(jsonPath("$.seriesId").value(seriesId))
                .andExpect(jsonPath("$.apiKey").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").value(matchesPattern(DATE_TIME_PATTERN)));

        var savedSeries = seriesRepository.findById(seriesId).orElseThrow();
        assertThat(savedSeries.sensors())
                .anySatisfy(sensor -> assertThat(sensor.name()).isEqualTo("NEW_SENSOR"));
    }

    @Test
    void getAllSensorsReturnsUnauthorizedWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/sensors"))
                .andExpect(status().isUnauthorized())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    void getSensorByIdReturnsUnauthorizedWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/sensors/{id}", "some-sensor"))
                .andExpect(status().isUnauthorized())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    @WithMockUser
    void getAllSensorsReturnsRegisteredSensorsWithoutApiKeys() throws Exception {
        var seriesId = createSeries();
        var sensorId = registerSensor("LISTED_SENSOR", seriesId);

        mockMvc.perform(get("/api/sensors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.id == '%s')]", sensorId).isNotEmpty())
                .andExpect(jsonPath("$[?(@.id == '%s')].name", sensorId).value(hasItem("LISTED_SENSOR")))
                .andExpect(jsonPath("$[?(@.id == '%s')].seriesId", sensorId).value(hasItem(seriesId)))
                .andExpect(jsonPath("$[?(@.id == '%s')].apiKey", sensorId).doesNotExist())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    @WithMockUser
    void getSensorByIdReturnsRegisteredSensorWithoutApiKey() throws Exception {
        var seriesId = createSeries();
        var sensorId = registerSensor("RETRIEVED_SENSOR", seriesId);

        mockMvc.perform(get("/api/sensors/{id}", sensorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sensorId))
                .andExpect(jsonPath("$.name").value("RETRIEVED_SENSOR"))
                .andExpect(jsonPath("$.seriesId").value(seriesId))
                .andExpect(jsonPath("$.createdAt").value(matchesPattern(DATE_TIME_PATTERN)))
                .andExpect(jsonPath("$.apiKey").doesNotExist())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    @WithMockUser
    void getSensorByIdReturnsNotFoundWhenSensorDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/sensors/{id}", "missing-sensor"))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    void deleteSensorReturnsUnauthorizedWhenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/sensors/{id}", "some-sensor"))
                .andExpect(status().isUnauthorized())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    @WithMockUser
    void deleteSensorReturnsNotFoundWhenSensorDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/sensors/{id}", "missing-sensor"))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    @WithMockUser
    void deleteSensorReturnsNoContentAndRemovesSensor() throws Exception {
        var seriesId = createSeries();
        var sensorId = registerSensor("SENSOR_TO_DELETE", seriesId);

        mockMvc.perform(delete("/api/sensors/{id}", sensorId))
                .andExpect(status().isNoContent())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));

        assertThat(seriesRepository.findById(seriesId).orElseThrow().sensors())
                .noneSatisfy(sensor -> assertThat(sensor.id()).isEqualTo(sensorId));

        mockMvc.perform(get("/api/sensors/{id}", sensorId))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    private String createSeries() throws Exception {
        var seriesInput = new SeriesInput()
                .name("SERIES_" + UUID.randomUUID())
                .minValue(BigDecimal.ZERO)
                .maxValue(BigDecimal.valueOf(100))
                .color("#ff0000");
        var result = mockMvc.perform(post("/api/series")
                        .with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtil.toJson(seriesInput)))
                .andExpect(status().isCreated())
                .andReturn();
        return TestUtil.toObject(result.getResponse().getContentAsString(), Series.class).getId();
    }

    private String registerSensor(String name, String seriesId) throws Exception {
        var result = mockMvc.perform(post("/api/sensors")
                        .with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sensorBody(name, seriesId)))
                .andExpect(status().isCreated())
                .andReturn();
        var location = result.getResponse().getHeader("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private String sensorBody(String name, String seriesId) {
        return TestUtil.toJson(new SensorInput().name(name).seriesId(seriesId));
    }
}
