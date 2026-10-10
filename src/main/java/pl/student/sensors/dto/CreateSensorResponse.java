package pl.student.sensors.dto;

public record CreateSensorResponse(
        Sensor sensor,
        String apiKey
) {
}
