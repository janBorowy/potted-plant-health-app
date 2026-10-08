package pl.student.sensors.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.student.sensors.dto.AuthenticationInput;
import pl.student.sensors.dto.JwtDto;
import pl.student.sensors.generated.model.LoginRequest;
import pl.student.sensors.generated.model.LoginResponse;

@Mapper(config = MapstructConfig.class)
public interface AuthMapper {

    AuthenticationInput toLoginInput(LoginRequest request);

    @Mapping(source = "expiresInSeconds", target = "expiresIn")
    @Mapping(target = "tokenType", constant = "BEARER")
    LoginResponse toResponse(JwtDto jwt);

}
