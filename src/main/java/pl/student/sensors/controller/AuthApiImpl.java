package pl.student.sensors.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import pl.student.sensors.generated.api.AuthApi;
import pl.student.sensors.generated.model.LoginRequest;
import pl.student.sensors.generated.model.LoginResponse;
import pl.student.sensors.generated.model.PasswordChange;
import pl.student.sensors.mapper.AuthMapper;
import pl.student.sensors.service.AuthenticationService;

@Controller
@RequiredArgsConstructor
public class AuthApiImpl implements AuthApi {

    private final AuthenticationService authenticationService;
    private final AuthMapper authMapper;

    @Override
    public ResponseEntity<Void> changePassword(PasswordChange passwordChange) {
        var input = authMapper.toChangePasswordInput(passwordChange);
        authenticationService.changePassword(input);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<LoginResponse> login(LoginRequest loginRequest) {
        var input = authMapper.toLoginInput(loginRequest);
        var result = authenticationService.authenticate(input);
        return ResponseEntity.ok(authMapper.toResponse(result));
    }

    @Override
    public ResponseEntity<Void> logout() {
        authenticationService.logout();
        return ResponseEntity.noContent().build();
    }
}
