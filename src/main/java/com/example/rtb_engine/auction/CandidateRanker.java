
package com.example.rtb_engine.auction;

import com.example.rtb_engine.entity.Ad;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
public class CandidateRanker {

    /**
     * 按出价降序排列。
     * 出价相同时，使用 requestId + adId 的稳定哈希排序。
     *
     * 不修改传入的原始 List。
     */
    public List<Ad> rank(List<Ad> candidates, String requestId) {

        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException(
                    "requestId 不能为空"
            );
        }

        Objects.requireNonNull(candidates, "candidates 不能为空");

        Comparator<Ad> comparator =
                Comparator.comparing(
                        Ad::getBid,
                        Comparator.reverseOrder()
                ).thenComparing(
                        ad -> hashPriority(requestId, ad.getId()),
                        Long::compareUnsigned
                ).thenComparing(Ad::getId);

        return candidates.stream()
                .sorted(comparator)
                .toList();
    }

    /**
     * 取 SHA-256 摘要前 8 字节作为无符号 64 位优先级。
     */
    private long hashPriority(String requestId, Long adId) {

        Objects.requireNonNull(adId, "adId 不能为空");

        String key = requestId + ":" + adId;

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    key.getBytes(StandardCharsets.UTF_8)
            );

            return ByteBuffer.wrap(hash).getLong();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 不可用", e
            );
        }
    }
}
