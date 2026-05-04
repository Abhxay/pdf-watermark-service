package com.geminid.watermark.controller;

import com.geminid.watermark.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final StorageService storageService;

    @GetMapping("/storage/check")
    public ResponseEntity<?> checkObject(@RequestParam String key) {
        boolean exists = storageService.objectExists(key);
        return ResponseEntity.ok(Map.of(
                "key", key,
                "exists", exists,
                "status", exists ? "PRESENT" : "DELETED_OR_NOT_FOUND"
        ));
    }
}