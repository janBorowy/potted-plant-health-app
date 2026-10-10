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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void getMeasurementByIdReturnsMeasurementWhenFound() throws Exception {
        var series = createSeries();
        var sensor = registerSensor("SENSOR_GET", series);
        var created = mockMvc.perform(post("/api/measurements")
                        .header(API_KEY_HEADER, sensor.apiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(measurementBody(12.5)))
                .andExpect(status().isCreated())
                .andReturn();
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/measurements/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.seriesId").value(series))
                .andExpect(jsonPath("$.sensorId").value(sensor.id()))
                .andExpect(jsonPath("$.value").value(closeTo(12.5, 0.001)))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    void getMeasurementByIdReturnsNotFoundWhenMeasurementDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/measurements/{id}", "000000000000000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    void searchMeasurementsReturnsAllMeasurementsOfSingleSeriesAndIgnoresOtherSeries() throws Exception {
        var series = createSeries();
        var otherSeries = createSeries();
        var sensor = registerSensor("SENSOR_SEARCH_ALL", series);
        var otherSensor = registerSensor("SENSOR_SEARCH_OTHER", otherSeries);
        postMeasurement(sensor, 1, "2024-01-01T10:00:00Z");
        postMeasurement(sensor, 2, "2024-01-02T10:00:00Z");
        postMeasurement(otherSensor, 99, "2024-01-01T12:00:00Z");

        mockMvc.perform(get("/api/measurements").param("series", series))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].seriesId").value(contains(series, series)))
                .andExpect(jsonPath("$[*].value").value(contains(1.0, 2.0)))
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    void searchMeasurementsReturnsEmptyListWhenSeriesHasNoMeasurements() throws Exception {
        var series = createSeries();

        mockMvc.perform(get("/api/measurements").param("series", series))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(empty()))
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    void searchMeasurementsFiltersByFromOnly() throws Exception {
        var series = createSeries();
        postFiveDailyMeasurements(series);

        mockMvc.perform(get("/api/measurements")
                        .param("series", series)
                        .param("from", "2024-01-03T12:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].value").value(contains(4.0, 5.0)))
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    void searchMeasurementsFiltersByToOnly() throws Exception {
        var series = createSeries();
        postFiveDailyMeasurements(series);

        mockMvc.perform(get("/api/measurements")
                        .param("series", series)
                        .param("to", "2024-01-02T12:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].value").value(contains(1.0, 2.0)))
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    void searchMeasurementsFiltersByFromAndToRange() throws Exception {
        var series = createSeries();
        postFiveDailyMeasurements(series);

        mockMvc.perform(get("/api/measurements")
                        .param("series", series)
                        .param("from", "2024-01-01T12:00:00Z")
                        .param("to", "2024-01-04T12:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].value").value(contains(2.0, 3.0, 4.0)))
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    void searchMeasurementsReturnsEmptyListWhenRangeContainsNoMeasurements() throws Exception {
        var series = createSeries();
        postFiveDailyMeasurements(series);

        mockMvc.perform(get("/api/measurements")
                        .param("series", series)
                        .param("from", "2023-01-01T00:00:00Z")
                        .param("to", "2023-12-31T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(empty()))
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    void searchMeasurementsSortsAscendingByTimestampByDefault() throws Exception {
        var series = createSeries();
        postFiveDailyMeasurements(series);

        mockMvc.perform(get("/api/measurements").param("series", series))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].value").value(contains(1.0, 2.0, 3.0, 4.0, 5.0)));
    }

    @Test
    void searchMeasurementsSortsAscendingWhenSortIsTimestamp() throws Exception {
        var series = createSeries();
        postFiveDailyMeasurements(series);

        mockMvc.perform(get("/api/measurements")
                        .param("series", series)
                        .param("sort", "timestamp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].value").value(contains(1.0, 2.0, 3.0, 4.0, 5.0)))
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    void searchMeasurementsSortsDescendingWhenSortIsMinusTimestamp() throws Exception {
        var series = createSeries();
        postFiveDailyMeasurements(series);

        mockMvc.perform(get("/api/measurements")
                        .param("series", series)
                        .param("sort", "-timestamp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].value").value(contains(5.0, 4.0, 3.0, 2.0, 1.0)))
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    void searchMeasurementsSortsByTimestampRegardlessOfInsertionOrder() throws Exception {
        var series = createSeries();
        var sensor = registerSensor("SENSOR_SEARCH_ORDER", series);
        postMeasurement(sensor, 3, "2024-01-03T10:00:00Z");
        postMeasurement(sensor, 1, "2024-01-01T10:00:00Z");
        postMeasurement(sensor, 2, "2024-01-02T10:00:00Z");

        mockMvc.perform(get("/api/measurements").param("series", series))
                .andExpect(jsonPath("$[*].value").value(contains(1.0, 2.0, 3.0)));
        mockMvc.perform(get("/api/measurements").param("series", series).param("sort", "timestamp"))
                .andExpect(jsonPath("$[*].value").value(contains(1.0, 2.0, 3.0)));
        mockMvc.perform(get("/api/measurements").param("series", series).param("sort", "-timestamp"))
                .andExpect(jsonPath("$[*].value").value(contains(3.0, 2.0, 1.0)));
    }

    @Test
    void searchMeasurementsRespectsLimitForAscendingSort() throws Exception {
        var series = createSeries();
        postFiveDailyMeasurements(series);

        mockMvc.perform(get("/api/measurements")
                        .param("series", series)
                        .param("sort", "timestamp")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].value").value(contains(1.0, 2.0)))
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    void searchMeasurementsRespectsLimitForDescendingSort() throws Exception {
        var series = createSeries();
        postFiveDailyMeasurements(series);

        mockMvc.perform(get("/api/measurements")
                        .param("series", series)
                        .param("sort", "-timestamp")
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].value").value(contains(5.0, 4.0, 3.0)))
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    void searchMeasurementsReturnsAllWhenLimitExceedsMeasurementCount() throws Exception {
        var series = createSeries();
        postFiveDailyMeasurements(series);

        mockMvc.perform(get("/api/measurements")
                        .param("series", series)
                        .param("limit", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    void searchMeasurementsAllFilters() throws Exception {
        var series = createSeries();
        postFiveDailyMeasurements(series);

        mockMvc.perform(get("/api/measurements")
                        .param("series", series)
                        .param("from", "2024-01-01T12:00:00Z")
                        .param("to", "2024-01-05T12:00:00Z")
                        .param("sort", "-timestamp")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].value").value(contains(5.0, 4.0)));
    }

    private void postFiveDailyMeasurements(String seriesId) throws Exception {
        var sensor = registerSensor("SENSOR_" + UUID.randomUUID(), seriesId);
        for (int day = 1; day <= 5; day++) {
            postMeasurement(sensor, day, "2024-01-0" + day + "T10:00:00Z");
        }
    }

    private void postMeasurement(RegisterSensorResponse sensor, double value, String timestamp) throws Exception {
        mockMvc.perform(post("/api/measurements")
                        .header(API_KEY_HEADER, sensor.apiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtil.toJson(Map.of("value", value, "timestamp", timestamp))))
                .andExpect(status().isCreated());
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
