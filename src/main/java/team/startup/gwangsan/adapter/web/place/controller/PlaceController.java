package team.startup.gwangsan.adapter.web.place.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.startup.gwangsan.adapter.web.place.dto.PlaceResponse;
import team.startup.gwangsan.application.place.port.in.PlaceQuery;

import java.util.List;

@RestController
@RequestMapping("/api/place")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceQuery placeQuery;

    @GetMapping
    public ResponseEntity<List<PlaceResponse>> findAll() {
        return ResponseEntity.ok(placeQuery.findAll().stream()
                .map(p -> new PlaceResponse(p.id(), p.name(), p.headId()))
                .toList());
    }
}
