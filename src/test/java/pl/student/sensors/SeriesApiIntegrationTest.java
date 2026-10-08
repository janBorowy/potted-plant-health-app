package pl.student.sensors;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import pl.student.sensors.generated.model.Series;
import pl.student.sensors.generated.model.SeriesInput;
import pl.student.sensors.model.SeriesRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        var body = JsonUtil.toJson(
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
                .andReturn();

        var resultBody = JsonUtil.toObject(result.getResponse().getContentAsString(), Series.class);

        assertThat(resultBody)
                .satisfies(it -> seriesRepository.existsById(it.getId()));
    }

    @Test
    void createSeriesReturnsUnauthorizedWhenNotLoggedIn() throws Exception {
        var body = JsonUtil.toJson(
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
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllSeriesReturnsOkWhenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/api/series"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
