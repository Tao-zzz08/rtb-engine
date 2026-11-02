
package com.example.rtb_engine.budget;

import java.math.BigDecimal;

public interface BudgetService {

    /**
     * 尝试扣减广告预算。
     *
     * @param requestId 竞价请求唯一标识
     * @param adId 广告ID
     * @param dailyBudget 每日预算上限
     * @param amount 本次成交金额
     *
     * @return SUCCESS / INSUFFICIENT / UNKNOWN
     */
    DeductResult tryDeduct(
            String requestId,
            Long adId,
            BigDecimal dailyBudget,
            BigDecimal amount
    );

}
