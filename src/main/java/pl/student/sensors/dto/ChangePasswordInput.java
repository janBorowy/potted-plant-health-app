package pl.student.sensors.dto;

public record ChangePasswordInput(
        String oldPassword,
        String newPassword
) {
}
