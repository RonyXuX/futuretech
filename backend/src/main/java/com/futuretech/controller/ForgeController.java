package com.futuretech.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.futuretech.dto.forge.BuildCheckRequest;
import com.futuretech.dto.forge.CompatibilityReport;
import com.futuretech.service.CompatibilityService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/forge")
public class ForgeController {

    private final CompatibilityService compatibilityService;

    public ForgeController(CompatibilityService compatibilityService) {
        this.compatibilityService = compatibilityService;
    }

    /** Open to guests (see SecurityConfig): it only calculates, nothing is saved. */
    @PostMapping("/check")
    public CompatibilityReport check(@Valid @RequestBody BuildCheckRequest request) {
        return compatibilityService.check(request.items());
    }
}
