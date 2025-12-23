package com.example.mini_drive_storage.controller;

import com.example.mini_drive_storage.service.MockDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/debug")
@RequiredArgsConstructor
public class MockDataController {
    private final MockDataService mockDataService;

    @PostMapping("/generate-system")
    public ResponseEntity<String> generateSystemMockData() {
        mockDataService.generateSystemMockData();
        return ResponseEntity.ok("Mock system data generated successfully");
    }
}
