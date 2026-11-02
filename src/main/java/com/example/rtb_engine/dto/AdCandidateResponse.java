package com.example.rtb_engine.dto;

import java.math.BigDecimal;

public record AdCandidateResponse(
        Long id,
        Long advertiserId,
        String title,
        String creativeUrl,
        String categoryCode,
        BigDecimal bid
){
}
