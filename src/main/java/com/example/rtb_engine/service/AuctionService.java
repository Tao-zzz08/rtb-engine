
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

    // M1-01 约定的保留价
    private static final BigDecimal RESERVE_PRICE =
            new BigDecimal("2.00");

    // 最低加价单位
    private static final BigDecimal MIN_INCREMENT =
            new BigDecimal("0.01");

    private final AdMapper adMapper;
    private final CandidateRanker candidateRanker;
    private final BudgetService budgetService;

    public AuctionService(
            AdMapper adMapper,
            CandidateRanker candidateRanker,
            BudgetService budgetService) {

        this.adMapper = Objects.requireNonNull(adMapper);
        this.candidateRanker =
                Objects.requireNonNull(candidateRanker);
        this.budgetService =
                Objects.requireNonNull(budgetService);
    }

    /**
     * 执行一次内部广告竞价。
     *
     * 业务规则：
     * 1. 仅允许出价 >= 2.00 的广告参与竞价。
     * 2. 按出价降序排序，同价使用确定性哈希裁决。
     * 3. 多候选使用二价 + 0.01 的定价规则。
     * 4. 最后一名候选按保留价 2.00 成交。
     * 5. 预算不足时尝试下一名候选。
     * 6. 预算结果未知时停止，返回 PENDING。
     */
    public AuctionResult execute(
            String requestId,
            String region,
            String categoryCode) {

        // 1. 校验请求参数
        if (requestId == null || requestId.isBlank()
                || region == null || region.isBlank()
                || categoryCode == null || categoryCode.isBlank()) {

            throw new IllegalArgumentException(
                    "竞价请求参数不能为空"
            );
        }

        // 2. 查询满足地域、类别和投放状态的广告
        List<Ad> candidates = adMapper.findCandidates(
                region,
                categoryCode
        );

        if (candidates == null || candidates.isEmpty()) {
            return AuctionResult.noAd(requestId);
        }

        // 3. 过滤低于保留价的候选广告
        List<Ad> qualified = candidates.stream()
                .filter(Objects::nonNull)
                .filter(ad -> ad.getBid() != null)
                .filter(ad ->
                        ad.getBid().compareTo(RESERVE_PRICE) >= 0
                )
                .toList();

        if (qualified.isEmpty()) {
            return AuctionResult.noAd(requestId);
        }

        // 4. 排序：出价降序，同价确定性哈希裁决
        List<Ad> ranked = candidateRanker.rank(
                qualified,
                requestId
        );

        // 5. 按顺序尝试候选广告
        for (int i = 0; i < ranked.size(); i++) {

            Ad current = ranked.get(i);

            // 6. 计算当前候选的成交价格
            BigDecimal clearingPrice =
                    calculatePrice(ranked, i);

            // 7. 尝试预算扣减
            DeductResult deductResult = Objects.requireNonNull(
                    budgetService.tryDeduct(
                            requestId,
                            current.getId(),
                            current.getDailyBudget(),
                            clearingPrice
                    ),
                    "预算服务不能返回 null"
            );

            // 8. 扣减成功，确认赢家
            if (deductResult == DeductResult.SUCCESS) {
                return AuctionResult.won(
                        requestId,
                        current.getId(),
                        clearingPrice
                );
            }

            // 9. 预算结果未知，禁止继续尝试
            if (deductResult == DeductResult.UNKNOWN) {
                return AuctionResult.pending(requestId);
            }

            // INSUFFICIENT：
            // 预算不足，跳过当前广告，继续下一名
        }

        // 10. 所有合格候选均无法成交
        return AuctionResult.noAd(requestId);
    }

    /**
     * 计算指定候选广告的成交价。
     *
     * 前提：
     * - ranked 非空。
     * - 所有候选出价均 >= 保留价。
     * - 已按出价降序排序。
     *
     * 定价规则：
     *
     * 单候选（或回退至最后一名）：
     *   P = RESERVE_PRICE
     *
     * 存在下一名候选：
     *   P = min(
     *       currentBid,
     *       max(RESERVE_PRICE, nextBid + MIN_INCREMENT)
     *   )
     */
    public BigDecimal calculatePrice(
            List<Ad> ranked,
            int index) {

        Objects.requireNonNull(
                ranked,
                "候选列表不能为空"
        );

        // 空列表、负索引、越界索引统一拒绝
        if (index < 0 || index >= ranked.size()) {
            throw new IllegalArgumentException(
                    "候选广告索引无效"
            );
        }

        Ad current = Objects.requireNonNull(
                ranked.get(index),
                "当前广告不能为空"
        );

        BigDecimal currentBid = Objects.requireNonNull(
                current.getBid(),
                "当前广告出价不能为空"
        );

        // 出价低于保留价，不允许成交
        if (currentBid.compareTo(RESERVE_PRICE) < 0) {
            throw new IllegalArgumentException(
                    "广告出价低于保留价"
            );
        }

        // 最后一名候选按保留价成交
        if (index == ranked.size() - 1) {
            return RESERVE_PRICE;
        }

        Ad next = Objects.requireNonNull(
                ranked.get(index + 1),
                "下一名广告不能为空"
        );

        BigDecimal nextBid = Objects.requireNonNull(
                next.getBid(),
                "下一名广告出价不能为空"
        );

        // 防止未过滤或未排序的候选列表进入计算
        if (nextBid.compareTo(RESERVE_PRICE) < 0) {
            throw new IllegalArgumentException(
                    "下一名广告出价低于保留价"
            );
        }

        if (currentBid.compareTo(nextBid) < 0) {
            throw new IllegalArgumentException(
                    "候选广告没有按出价降序排列"
            );
        }

        // 第二高出价加上最小加价
        BigDecimal secondPrice =
                nextBid.add(MIN_INCREMENT);

        // 成交价至少为保留价
        BigDecimal priceWithReserve =
                secondPrice.max(RESERVE_PRICE);

        // 成交价不能超过自己的出价
        return priceWithReserve.min(currentBid);
    }
}
