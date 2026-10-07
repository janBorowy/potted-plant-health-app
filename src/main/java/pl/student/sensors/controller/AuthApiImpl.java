package pl.student.sensors.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import pl.student.sensors.generated.api.AuthApi;
import pl.student.sensors.generated.model.LoginRequest;
import pl.student.sensors.generated.model.LoginResponse;
import pl.student.sensors.generated.model.PasswordChange;

public class AuthApiImpl implements AuthApi {
    @Override
    public ResponseEntity<Void> changePassword(PasswordChange passwordChange) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<LoginResponse> login(LoginRequest loginRequest) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<Void> logout() {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }
}
