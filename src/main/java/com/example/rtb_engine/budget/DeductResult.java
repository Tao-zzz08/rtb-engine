
package com.example.rtb_engine.budget;

public enum DeductResult {

    // 已确认扣减成功
    SUCCESS,

    // 预算不足，且没有发生扣减
    INSUFFICIENT,

    // 无法确认是否扣减成功
    UNKNOWN

}
