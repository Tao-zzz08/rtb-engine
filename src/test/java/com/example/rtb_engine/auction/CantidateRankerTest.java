
package com.example.rtb_engine.auction;

import com.example.rtb_engine.entity.Ad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CandidateRankerTest {

    private CandidateRanker ranker;

    @BeforeEach
    void setUp() {
        ranker = new CandidateRanker();
    }

    private Ad createAd(long id, String bid) {
        Ad ad = new Ad();
        ad.setId(id);
        ad.setBid(new BigDecimal(bid));
        return ad;
    }

    @Test
    void testHigherBidAlwaysFirst() {
        List<Ad> ads = List.of(
                createAd(1, "8.00"),
                createAd(2, "10.00"),
                createAd(3, "6.00")
        );

        List<Ad> result = ranker.rank(ads, "request-001");

        assertEquals(2L, result.get(0).getId());
        assertEquals(1L, result.get(1).getId());
        assertEquals(3L, result.get(2).getId());
    }

    @Test
    void testSameRequestProducesSameOrder() {
        List<Ad> ads = List.of(
                createAd(1, "10.00"),
                createAd(2, "10.00"),
                createAd(3, "10.00")
        );

        List<Ad> first = ranker.rank(ads, "request-001");

        // 故意打乱原始顺序，验证排序不依赖数据库返回顺序
        List<Ad> reversed = List.of(
                ads.get(2), ads.get(1), ads.get(0)
        );

        List<Ad> second = ranker.rank(
                reversed, "request-001"
        );

        assertEquals(
                first.stream().map(Ad::getId).toList(),
                second.stream().map(Ad::getId).toList()
        );
    }

    @Test
    void testDifferentRequestsCanChangeWinner() {
        List<Ad> ads = List.of(
                createAd(1, "10.00"),
                createAd(2, "10.00")
        );

        long winsA = 0;
        long winsB = 0;

        for (int i = 0; i < 1000; i++) {
            List<Ad> result = ranker.rank(
                    ads, "request-" + i
            );

            if (result.get(0).getId() == 1L) {
                winsA++;
            } else {
                winsB++;
            }
        }

        // 验证两条同价广告都能获得第一优先级
        assertTrue(winsA > 0);
        assertTrue(winsB > 0);

        System.out.println("广告 A 优先次数：" + winsA);
        System.out.println("广告 B 优先次数：" + winsB);
    }

    @Test
    void testEmptyRequestIdRejected() {
        List<Ad> ads = List.of(createAd(1, "10.00"));

        assertThrows(
                IllegalArgumentException.class,
                () -> ranker.rank(ads, "")
        );
    }
}
