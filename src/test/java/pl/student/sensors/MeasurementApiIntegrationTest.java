package pl.student.sensors;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pl.student.sensors.generated.model.SensorInput;
import pl.student.sensors.generated.model.Series;
import pl.student.sensors.generated.model.SeriesInput;
import pl.student.sensors.model.MeasurementRepository;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pl.student.sensors.TestUtil.OPEN_API_VALIDATOR;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MeasurementApiIntegrationTest extends AbstractIntegrationTest {

    private static final String API_KEY_HEADER = "x-api-key";
    private static final Pattern LOCATION_PATTERN = Pattern.compile("/api/measurements/[0-9a-z]+");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeasurementRepository measurementRepository;

    @Test
    void postMeasurementReturnsCreatedAndPersistsMeasurementForSensorSeries() throws Exception {
        var seriesId = createSeries();
        var sensor = registerSensor("SENSOR_OK", seriesId);

        var result = mockMvc.perform(post("/api/measurements")
                        .header(API_KEY_HEADER, sensor.apiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(measurementBody(42.5)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(LOCATION_PATTERN)))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.seriesId").value(seriesId))
                .andExpect(jsonPath("$.sensorId").value(sensor.id()))
                .andExpect(jsonPath("$.value").value(closeTo(42.5, 0.001)))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR))
                .andReturn();

        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        assertThat(measurementRepository.existsById(id)).isTrue();
    }

    @Test
    void postMeasurementAcceptsValuesOnSeriesBounds() throws Exception {
        var seriesId = createSeries();
        var sensor = registerSensor("SENSOR_BOUNDS", seriesId);

        for (var value : new double[] {0, 100}) {
            mockMvc.perform(post("/api/measurements")
                            .header(API_KEY_HEADER, sensor.apiKey())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(measurementBody(value)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", matchesPattern(LOCATION_PATTERN)))
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.seriesId").value(seriesId))
                    .andExpect(jsonPath("$.sensorId").value(sensor.id()))
                    .andExpect(jsonPath("$.value").value(closeTo(value, 0.001)))
                    .andExpect(jsonPath("$.timestamp").isNotEmpty())
                    .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
        }
    }

    @Test
    void postMeasurementAssignsSeriesFromApiKeyOfGivenSensor() throws Exception {
        var seriesA = createSeries();
        var seriesB = createSeries();
        registerSensor("SENSOR_A", seriesA);
        var sensorB = registerSensor("SENSOR_B", seriesB);

        mockMvc.perform(post("/api/measurements")
                        .header(API_KEY_HEADER, sensorB.apiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(measurementBody(10)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seriesId").value(seriesB))
                .andExpect(jsonPath("$.sensorId").value(sensorB.id()));
    }

    @Test
    void postMeasurementReturnsUnauthorizedWhenApiKeyMissing() throws Exception {
        var before = measurementRepository.count();

        mockMvc.perform(post("/api/measurements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(measurementBody(10)))
                .andExpect(status().isUnauthorized())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));

        assertThat(measurementRepository.count()).isEqualTo(before);
    }

    @Test
    void postMeasurementReturnsUnauthorizedWhenApiKeyBlank() throws Exception {
        mockMvc.perform(post("/api/measurements")
                        .header(API_KEY_HEADER, "  ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(measurementBody(10)))
                .andExpect(status().isUnauthorized())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    void postMeasurementReturnsUnauthorizedWhenApiKeyUnknown() throws Exception {
        mockMvc.perform(post("/api/measurements")
                        .header(API_KEY_HEADER, "unknown-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(measurementBody(10)))
                .andExpect(status().isUnauthorized())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    void postMeasurementReturnsUnauthorizedWhenOnlyAdminTokenGiven() throws Exception {
        mockMvc.perform(post("/api/measurements")
                        .with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(measurementBody(10)))
                .andExpect(status().isUnauthorized())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    void postMeasurementReturnsUnprocessableWhenValueAboveMax() throws Exception {
        var series = createSeries();
        var sensor = registerSensor("SENSOR_ABOVE", series);
        var countBefore = measurementRepository.count();

        mockMvc.perform(post("/api/measurements")
                        .header(API_KEY_HEADER, sensor.apiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(measurementBody(100.01)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));

        assertThat(measurementRepository.count()).isEqualTo(countBefore);
    }

    @Test
    void postMeasurementReturnsUnprocessableWhenValueBelowMin() throws Exception {
        var series = createSeries();
        var sensor = registerSensor("SENSOR_BELOW", series);
        var countBefore = measurementRepository.count();

        mockMvc.perform(post("/api/measurements")
                        .header(API_KEY_HEADER, sensor.apiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(measurementBody(-0.01)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));

        assertThat(measurementRepository.count()).isEqualTo(countBefore);
    }

    @Test
    void postMeasurementReturnsBadRequestWhenValueMissing() throws Exception {
        var series = createSeries();
        var sensor = registerSensor("SENSOR_NO_VALUE", series);

        mockMvc.perform(post("/api/measurements")
                        .header(API_KEY_HEADER, sensor.apiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private String createSeries() throws Exception {
        var seriesInput = new SeriesInput()
                .name("SERIES_" + UUID.randomUUID())
                .minValue(BigDecimal.valueOf(0))
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

    private RegisterSensorResponse registerSensor(String name, String seriesId) throws Exception {
        var result = mockMvc.perform(post("/api/sensors")
                        .with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtil.toJson(new SensorInput().name(name).seriesId(seriesId))))
                .andExpect(status().isCreated())
                .andReturn();
        var body = result.getResponse().getContentAsString();
        return new RegisterSensorResponse(JsonPath.read(body, "$.id"), JsonPath.read(body, "$.apiKey"));
    }

    private String measurementBody(double value) {
        return TestUtil.toJson(Map.of("value", value));
    }

    private record RegisterSensorResponse(String id, String apiKey) {
    }
}
