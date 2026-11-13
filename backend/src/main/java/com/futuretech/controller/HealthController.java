package com.futuretech.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Simple endpoint to check that the backend is up: GET /api/health */
@RestController
@RequestMapping("/api")
public class HealthController {

	@GetMapping("/health")
	public Map<String, String> health() {
		return Map.of("status", "UP");
	}
}
