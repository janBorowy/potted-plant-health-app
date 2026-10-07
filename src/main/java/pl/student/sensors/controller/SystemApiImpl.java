package pl.student.sensors.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import pl.student.sensors.generated.api.SystemApi;
import pl.student.sensors.generated.model.GetApiHealth200Response;

public class SystemApiImpl implements SystemApi {

    @Override
    public ResponseEntity<GetApiHealth200Response> getApiHealth() {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }

}
