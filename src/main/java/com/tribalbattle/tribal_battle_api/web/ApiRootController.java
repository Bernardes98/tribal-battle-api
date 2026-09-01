package com.tribalbattle.tribal_battle_api.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ApiRootController {

    @GetMapping("/")
    public ResponseEntity<Map<String, String>> root() {
        return ResponseEntity.ok(
                Map.of(
                        "name",
                        "Tribal Battle API",
                        "status",
                        "UP"
                )
        );
    }
}
