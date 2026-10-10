package pl.student.sensors.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import pl.student.sensors.generated.api.SystemApi;
import pl.student.sensors.generated.model.GetApiHealth200Response;

@RestController
public class SystemApiImpl implements SystemApi {

    @Override
    public ResponseEntity<GetApiHealth200Response> getApiHealth() {
        return ResponseEntity.ok(new GetApiHealth200Response(GetApiHealth200Response.StatusEnum.OK));
    }

}
