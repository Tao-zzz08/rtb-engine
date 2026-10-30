package com.example.rtb_engine.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Ad {
    private Long id;
    private Long advertiserId;
    private String title;
    private String creativeUrl;
    private String categoryCode;
    private BigDecimal bid;
    private BigDecimal dailyBudget;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
