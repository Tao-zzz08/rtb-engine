
package com.example.rtb_engine.controller;

import com.example.rtb_engine.dto.AdCandidateResponse;
import com.example.rtb_engine.service.AdService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ads")
public class AdController {

    private final AdService adService;

    public AdController(AdService adService) {
        this.adService = adService;
    }

    @GetMapping("/candidates")
    public ResponseEntity<List<AdCandidateResponse>>
    getCandidates(
            @RequestParam String region,
            @RequestParam String categoryCode) {

        if (region.isBlank() || categoryCode.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        List<AdCandidateResponse> candidates =
                adService.findCandidates(
                        region.trim(),
                        categoryCode.trim()
                );

        return ResponseEntity.ok(candidates);
    }
}
