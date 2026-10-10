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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import pl.student.sensors.generated.model.SensorInput;
import pl.student.sensors.generated.model.Series;
import pl.student.sensors.generated.model.SeriesInput;
import pl.student.sensors.model.SeriesRepository;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pl.student.sensors.TestUtil.OPEN_API_VALIDATOR;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SeriesApiIntegrationTest extends AbstractIntegrationTest {

    private static final Pattern LOCATION_PATTERN = Pattern.compile("/api/series/[0-9a-z]+");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SeriesRepository seriesRepository;

    @Test
    @WithMockUser
    void createSeriesReturnsCreatedAndPersistsSeries() throws Exception {
        var body = TestUtil.toJson(
                new SeriesInput()
                        .name("TEMP_SENSOR_0")
                        .color("#ff0000")
                        .minValue(BigDecimal.valueOf(-20.0))
                        .maxValue(BigDecimal.valueOf(100.0))
                        .unit("°C")
        );

        var result = mockMvc.perform(post("/api/series")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(LOCATION_PATTERN)))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("TEMP_SENSOR_0"))
                .andExpect(jsonPath("$.color").value("#ff0000"))
                .andExpect(jsonPath("$.minValue").value(closeTo(-20, 0.01)))
                .andExpect(jsonPath("$.maxValue").value(closeTo(100.0, 0.01)))
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR))
                .andReturn();

        var resultBody = TestUtil.toObject(result.getResponse().getContentAsString(), Series.class);

        assertThat(resultBody)
                .satisfies(it -> seriesRepository.existsById(it.getId()));
    }

    @Test
    void createSeriesReturnsUnauthorizedWhenNotLoggedIn() throws Exception {
        var body = TestUtil.toJson(
                new SeriesInput()
                        .name("TEMP_SENSOR_0")
                        .color("#ff0000")
                        .minValue(BigDecimal.valueOf(-20.0))
                        .maxValue(BigDecimal.valueOf(100.0))
                        .unit("°C")
        );

        mockMvc.perform(post("/api/series")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    @WithMockUser
    void createSeriesReturnsBadRequestWhenNameAlreadyExists() throws Exception {
        var body = TestUtil.toJson(
                new SeriesInput()
                        .name("DUPLICATE_SENSOR")
                        .color("#ff0000")
                        .minValue(BigDecimal.valueOf(-20.0))
                        .maxValue(BigDecimal.valueOf(100.0))
                        .unit("°C")
        );

        mockMvc.perform(post("/api/series")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));

        mockMvc.perform(post("/api/series")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    void getAllSeriesReturnsOkWhenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/api/series"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    void deleteSeriesReturnsUnauthorizedWhenNotLoggedIn() throws Exception {
        mockMvc.perform(delete("/api/series/{id}", "someId"))
                .andExpect(status().isUnauthorized())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    @WithMockUser
    void deleteSeriesReturnsNotFoundWhenSeriesDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/series/{id}", "nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    @WithMockUser
    void deleteSeriesReturnsNoContentAndRemovesSeries() throws Exception {
        var body = TestUtil.toJson(
                new SeriesInput()
                        .name("SERIES_TO_DELETE")
                        .color("#00ff00")
                        .minValue(BigDecimal.valueOf(0))
                        .maxValue(BigDecimal.valueOf(10))
                        .unit("°C")
        );
        var created = mockMvc.perform(post("/api/series")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        var id = TestUtil.toObject(created.getResponse().getContentAsString(), Series.class).getId();
        assertThat(seriesRepository.existsById(id)).isTrue();

        mockMvc.perform(delete("/api/series/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));

        assertThat(seriesRepository.existsById(id)).isFalse();

        mockMvc.perform(delete("/api/series/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    void getSeriesByIdReturnsOkWhenNotLoggedIn() throws Exception {
        var body = TestUtil.toJson(
                new SeriesInput()
                        .name("NEW_SERIES")
                        .color("#0000ff")
                        .minValue(BigDecimal.valueOf(0))
                        .maxValue(BigDecimal.valueOf(10))
                        .unit("°C")
        );
        var created = mockMvc.perform(post("/api/series")
                        .with(user("user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        var id = TestUtil.toObject(created.getResponse().getContentAsString(), Series.class).getId();

        mockMvc.perform(get("/api/series/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("NEW_SERIES"))
                .andExpect(jsonPath("$.color").value("#0000ff"))
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    void getSeriesByIdReturnsNotFoundWhenSeriesDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/series/{id}", "nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    @WithMockUser
    void replaceSeriesReturnsOkAndUpdatesSeries() throws Exception {
        var id = createSeries("REPLACE_ORIGINAL", 0, 100);
        var body = TestUtil.toJson(seriesInput("REPLACE_UPDATED", -50, 200).color("#00ff00").unit("K"));

        mockMvc.perform(put("/api/series/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("REPLACE_UPDATED"))
                .andExpect(jsonPath("$.color").value("#00ff00"))
                .andExpect(jsonPath("$.unit").value("K"))
                .andExpect(jsonPath("$.minValue").value(closeTo(-50, 0.01)))
                .andExpect(jsonPath("$.maxValue").value(closeTo(200, 0.01)))
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));

        var stored = seriesRepository.findById(id).orElseThrow();
        assertThat(stored.name()).isEqualTo("REPLACE_UPDATED");
        assertThat(stored.color()).isEqualTo("#00ff00");
        assertThat(stored.unit()).isEqualTo("K");
        assertThat(stored.minValue()).isEqualTo(-50);
        assertThat(stored.maxValue()).isEqualTo(200);
    }

    @Test
    @WithMockUser
    void replaceSeriesNarrowsRangeWhenExistingMeasurementsFit() throws Exception {
        var id = createSeries("REPLACE_NARROW", 0, 100);
        addMeasurements(id, 10, 20, 30);

        mockMvc.perform(put("/api/series/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtil.toJson(seriesInput("REPLACE_NARROW", 10, 30))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minValue").value(closeTo(10, 0.01)))
                .andExpect(jsonPath("$.maxValue").value(closeTo(30, 0.01)))
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));

        var stored = seriesRepository.findById(id).orElseThrow();
        assertThat(stored.minValue()).isEqualTo(10);
        assertThat(stored.maxValue()).isEqualTo(30);
    }

    @Test
    @WithMockUser
    void replaceSeriesReturnsConflictAndDoesNotUpdateWhenMeasurementBelowNewMinValue() throws Exception {
        var id = createSeries("REPLACE_CONFLICT_MIN", 0, 100);
        addMeasurements(id, 5, 50);

        mockMvc.perform(put("/api/series/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtil.toJson(seriesInput("REPLACE_CONFLICT_MIN_NEW", 10, 100))))
                .andExpect(status().isConflict())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));

        assertSeriesUnchanged(id, "REPLACE_CONFLICT_MIN", 0, 100);
    }

    @Test
    @WithMockUser
    void replaceSeriesReturnsConflictAndDoesNotUpdateWhenMeasurementAboveNewMaxValue() throws Exception {
        var id = createSeries("REPLACE_CONFLICT_MAX", 0, 100);
        addMeasurements(id, 50, 95);

        mockMvc.perform(put("/api/series/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtil.toJson(seriesInput("REPLACE_CONFLICT_MAX_NEW", 0, 90))))
                .andExpect(status().isConflict())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));

        assertSeriesUnchanged(id, "REPLACE_CONFLICT_MAX", 0, 100);
    }

    @Test
    @WithMockUser
    void replaceSeriesReturnsConflictWhenBothBoundsExcludeMeasurements() throws Exception {
        var id = createSeries("REPLACE_CONFLICT_BOTH", 0, 100);
        addMeasurements(id, 5, 95);

        mockMvc.perform(put("/api/series/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtil.toJson(seriesInput("REPLACE_CONFLICT_BOTH", 10, 90))))
                .andExpect(status().isConflict())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));

        assertSeriesUnchanged(id, "REPLACE_CONFLICT_BOTH", 0, 100);
    }

    @Test
    @WithMockUser
    void replaceSeriesAcceptsMeasurementsExactlyOnNewBounds() throws Exception {
        var id = createSeries("REPLACE_ON_BOUNDS", 0, 100);
        addMeasurements(id, 10, 90);

        mockMvc.perform(put("/api/series/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtil.toJson(seriesInput("REPLACE_ON_BOUNDS", 10, 90))))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    @WithMockUser
    void replaceSeriesReturnsNotFoundWhenSeriesDoesNotExist() throws Exception {
        mockMvc.perform(put("/api/series/{id}", "nonexistent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtil.toJson(seriesInput("REPLACE_MISSING", 0, 10))))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    void replaceSeriesReturnsUnauthorizedWhenNotLoggedIn() throws Exception {
        var id = createSeries("REPLACE_UNAUTHORIZED", 0, 100);

        mockMvc.perform(put("/api/series/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtil.toJson(seriesInput("REPLACE_UNAUTHORIZED_NEW", 0, 200))))
                .andExpect(status().isUnauthorized())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));

        assertSeriesUnchanged(id, "REPLACE_UNAUTHORIZED", 0, 100);
    }

    private SeriesInput seriesInput(String name, double min, double max) {
        return new SeriesInput()
                .name(name)
                .color("#ff0000")
                .minValue(BigDecimal.valueOf(min))
                .maxValue(BigDecimal.valueOf(max));
    }

    private String createSeries(String name, double min, double max) throws Exception {
        var result = mockMvc.perform(post("/api/series")
                        .with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtil.toJson(seriesInput(name, min, max))))
                .andExpect(status().isCreated())
                .andReturn();
        return TestUtil.toObject(result.getResponse().getContentAsString(), Series.class).getId();
    }

    private void addMeasurements(String seriesId, double... values) throws Exception {
        var sensorResult = mockMvc.perform(post("/api/sensors")
                        .with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtil.toJson(new SensorInput().name("SENSOR_" + UUID.randomUUID()).seriesId(seriesId))))
                .andExpect(status().isCreated())
                .andReturn();
        String apiKey = JsonPath.read(sensorResult.getResponse().getContentAsString(), "$.apiKey");
        for (var value : values) {
            mockMvc.perform(post("/api/measurements")
                            .header("x-api-key", apiKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(TestUtil.toJson(Map.of("value", value))))
                    .andExpect(status().isCreated());
        }
    }

    private void assertSeriesUnchanged(String id, String name, double min, double max) {
        var stored = seriesRepository.findById(id).orElseThrow();
        assertThat(stored.name()).isEqualTo(name);
        assertThat(stored.minValue()).isEqualTo(min);
        assertThat(stored.maxValue()).isEqualTo(max);
        assertThat(stored.color()).isEqualTo("#ff0000");
    }
}
