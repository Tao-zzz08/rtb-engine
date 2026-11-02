package com.example.rtb_engine.service;

import com.example.rtb_engine.auction.CandidateRanker;
import com.example.rtb_engine.budget.BudgetService;
import com.example.rtb_engine.budget.DeductResult;
import com.example.rtb_engine.dto.AuctionResult;
import com.example.rtb_engine.entity.Ad;
import com.example.rtb_engine.mapper.AdMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class AuctionService {

    // 最小加价单位：0.01 元
    private static final BigDecimal MIN_INCREMENT =
            new BigDecimal("0.01");

    private final AdMapper adMapper;
    private final CandidateRanker candidateRanker;
    private final BudgetService budgetService;

    public AuctionService(
            AdMapper adMapper,
            CandidateRanker candidateRanker,
            BudgetService budgetService) {

        this.adMapper = adMapper;
        this.candidateRanker = candidateRanker;
        this.budgetService = budgetService;
    }

    /**
     * 执行一次内部广告竞价。
     */
    public AuctionResult execute(
            String requestId,
            String region,
            String categoryCode) {

        if (requestId == null || requestId.isBlank()
                || region == null || region.isBlank()
                || categoryCode == null || categoryCode.isBlank()) {
            throw new IllegalArgumentException(
                    "竞价请求参数不能为空"
            );
        }

        // 1. 查询候选广告
        List<Ad> candidates = adMapper.findCandidates(
                region, categoryCode
        );

        if (candidates == null || candidates.isEmpty()) {
            return AuctionResult.noAd(requestId);
        }

        // 2. 出价排序 + 确定性哈希平局裁决
        List<Ad> ranked = candidateRanker.rank(
                candidates, requestId
        );

        // 3. 按照排序结果逐个尝试
        for (int i = 0; i < ranked.size(); i++) {

            Ad current = ranked.get(i);

            // 4. 计算当前候选的成交价格
            BigDecimal price = calculatePrice(
                    ranked, i
            );

            // 5. 原子尝试预算扣减
            DeductResult result = Objects.requireNonNull(
                    budgetService.tryDeduct(
                            requestId,
                            current.getId(),
                            current.getDailyBudget(),
                            price
                    ),
                    "预算服务不能返回 null"
            );

            if (result == DeductResult.SUCCESS) {
                return AuctionResult.won(
                        requestId,
                        current.getId(),
                        price
                );
            }

            if (result == DeductResult.UNKNOWN) {
                // 扣费结果未知，不允许继续尝试下一名
                return AuctionResult.pending(requestId);
            }

            // INSUFFICIENT:
            // 当前广告预算不足，继续尝试下一名
        }

        // 所有候选都无法支付
        return AuctionResult.noAd(requestId);
    }

    /**
     * 二价公式：
     * min(当前广告出价, 下一名广告出价 + 0.01)
     *
     * 与参考项目保持一致：
     * 最后一名广告按自身出价计费。
     */
    public BigDecimal calculatePrice(
            List<Ad> ranked,
            int index) {

        Objects.requireNonNull(ranked, "ranked 不能为空");

        if (index < 0 || index >= ranked.size()) {
            throw new IllegalArgumentException(
                    "候选广告索引无效"
            );
        }

        Ad current = ranked.get(index);

        BigDecimal currentBid =
                Objects.requireNonNull(
                        current.getBid(),
                        "广告出价不能为空"
                );

        if (currentBid.signum() <= 0) {
            throw new IllegalArgumentException(
                    "广告出价必须大于 0"
            );
        }

        // 只有最后一名时，按照自身出价计费
        if (index == ranked.size() - 1) {
            return currentBid;
        }

        BigDecimal nextBid =
                Objects.requireNonNull(
                        ranked.get(index + 1).getBid(),
                        "下一名广告出价不能为空"
                );

        if (nextBid.signum() <= 0
                || currentBid.compareTo(nextBid) < 0) {
            throw new IllegalArgumentException(
                    "候选列表出价无效或未按降序排列"
            );
        }

        return nextBid.add(MIN_INCREMENT)
                .min(currentBid);
    }
}
