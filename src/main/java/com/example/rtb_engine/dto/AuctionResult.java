
package com.example.rtb_engine.dto;

import java.math.BigDecimal;

public record AuctionResult(
        String requestId,
        Status status,
        Long winnerAdId,
        BigDecimal clearingPrice
) {

    public enum Status {
        WON,
        NO_AD,
        PENDING
    }

    // 竞价成功
    public static AuctionResult won(
            String requestId,
            Long adId,
            BigDecimal price) {

        return new AuctionResult(
                requestId,
                Status.WON,
                adId,
                price
        );
    }

    // 无广告可成交
    public static AuctionResult noAd(String requestId) {

        return new AuctionResult(
                requestId,
                Status.NO_AD,
                null,
                null
        );
    }

    // 预算扣减结果未知
    public static AuctionResult pending(String requestId) {

        return new AuctionResult(
                requestId,
                Status.PENDING,
                null,
                null
        );
    }
}
