package com.example.application.data.controller;

import com.example.application.data.service.MetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/exporter")
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }


    @GetMapping("/{id}")
    public ResponseEntity<String> getMetrics(@PathVariable("id") int id) {
        // Fetch metrics using the provided ID
        String prometheusMetrics = metricsService.displayMonitoringData(id);
        return ResponseEntity.ok().body(prometheusMetrics);
    }
}
