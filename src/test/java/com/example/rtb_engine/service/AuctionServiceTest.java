
package com.example.rtb_engine.service;

import com.example.rtb_engine.auction.CandidateRanker;
import com.example.rtb_engine.budget.BudgetService;
import com.example.rtb_engine.budget.DeductResult;
import com.example.rtb_engine.dto.AuctionResult;
import com.example.rtb_engine.entity.Ad;
import com.example.rtb_engine.mapper.AdMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuctionServiceTest {

    private AdMapper adMapper;
    private BudgetService budgetService;
    private AuctionService auctionService;

    // 创建测试广告
    private Ad createAd(long id, String bid) {
        Ad ad = new Ad();
        ad.setId(id);
        ad.setBid(new BigDecimal(bid));
        ad.setDailyBudget(new BigDecimal("100.00"));
        return ad;
    }

    @BeforeEach
    void setUp() {
        adMapper = mock(AdMapper.class);
        budgetService = mock(BudgetService.class);

        auctionService = new AuctionService(
                adMapper,
                new CandidateRanker(),
                budgetService
        );
    }

    // 1. 正常二价计算
    @Test
    void testSecondPriceCalculation() {
        List<Ad> ads = List.of(
                createAd(1, "10.00"),
                createAd(2, "8.00"),
                createAd(3, "6.00")
        );

        assertEquals(
                new BigDecimal("8.01"),
                auctionService.calculatePrice(ads, 0)
        );

        assertEquals(
                new BigDecimal("6.01"),
                auctionService.calculatePrice(ads, 1)
        );
    }

    // 2. 相同最高出价
    @Test
    void testEqualHighestBids() {
        List<Ad> ads = List.of(
                createAd(1, "10.00"),
                createAd(2, "10.00"),
                createAd(3, "8.00")
        );

        List<Ad> ranked = new CandidateRanker()
                .rank(ads, "request-equal");

        assertEquals(
                new BigDecimal("10.00"),
                auctionService.calculatePrice(ranked, 0)
        );
    }

    // 3. 单候选高于保留价
    @Test
    void testSingleCandidatePaysReservePrice() {
        List<Ad> ads = List.of(
                createAd(1, "10.00")
        );

        assertEquals(
                new BigDecimal("2.00"),
                auctionService.calculatePrice(ads, 0)
        );
    }

    // 4. 出价等于保留价
    @Test
    void testBidEqualsReservePrice() {
        List<Ad> ads = List.of(
                createAd(1, "2.00")
        );

        assertEquals(
                new BigDecimal("2.00"),
                auctionService.calculatePrice(ads, 0)
        );
    }

    // 5. 出价低于保留价，计价方法拒绝
    @Test
    void testCalculatePriceRejectsBelowReserve() {
        List<Ad> ads = List.of(
                createAd(1, "1.00")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> auctionService.calculatePrice(ads, 0)
        );
    }

    // 6. 整体竞价过滤低于保留价的广告
    @Test
    void testBidBelowReserveReturnsNoAd() {
        when(adMapper.findCandidates("110000", "GAME"))
                .thenReturn(List.of(
                        createAd(1, "1.00")
                ));

        AuctionResult result = auctionService.execute(
                "request-below-reserve",
                "110000",
                "GAME"
        );

        assertEquals(
                AuctionResult.Status.NO_AD,
                result.status()
        );

        verifyNoInteractions(budgetService);
    }

    // 7. 多候选中只有一位符合保留价
    @Test
    void testOnlyOneQualifiedCandidate() {
        when(adMapper.findCandidates("110000", "GAME"))
                .thenReturn(List.of(
                        createAd(1, "10.00"),
                        createAd(2, "1.00")
                ));

        when(budgetService.tryDeduct(
                eq("request-one-qualified"),
                eq(1L),
                any(BigDecimal.class),
                eq(new BigDecimal("2.00"))
        )).thenReturn(DeductResult.SUCCESS);

        AuctionResult result = auctionService.execute(
                "request-one-qualified",
                "110000",
                "GAME"
        );

        assertEquals(AuctionResult.Status.WON, result.status());
        assertEquals(1L, result.winnerAdId());
        assertEquals(
                new BigDecimal("2.00"),
                result.clearingPrice()
        );

        verify(budgetService, times(1))
                .tryDeduct(
                        anyString(),
                        anyLong(),
                        any(BigDecimal.class),
                        any(BigDecimal.class)
                );
    }

    // 8. 最高出价者获胜
    @Test
    void testHighestBidWins() {
        when(adMapper.findCandidates("110000", "GAME"))
                .thenReturn(List.of(
                        createAd(1, "10.00"),
                        createAd(2, "8.00")
                ));

        when(budgetService.tryDeduct(
                eq("request-001"),
                eq(1L),
                any(BigDecimal.class),
                eq(new BigDecimal("8.01"))
        )).thenReturn(DeductResult.SUCCESS);

        AuctionResult result = auctionService.execute(
                "request-001",
                "110000",
                "GAME"
        );

        assertEquals(AuctionResult.Status.WON, result.status());
        assertEquals(1L, result.winnerAdId());
        assertEquals(
                new BigDecimal("8.01"),
                result.clearingPrice()
        );

        verify(budgetService, times(1))
                .tryDeduct(
                        anyString(),
                        anyLong(),
                        any(BigDecimal.class),
                        any(BigDecimal.class)
                );
    }

    // 9. 预算不足后尝试下一名
    @Test
    void testBudgetInsufficientFallback() {
        when(adMapper.findCandidates("110000", "GAME"))
                .thenReturn(List.of(
                        createAd(1, "10.00"),
                        createAd(2, "8.00"),
                        createAd(3, "6.00")
                ));

        when(budgetService.tryDeduct(
                anyString(),
                eq(1L),
                any(BigDecimal.class),
                eq(new BigDecimal("8.01"))
        )).thenReturn(DeductResult.INSUFFICIENT);

        when(budgetService.tryDeduct(
                anyString(),
                eq(2L),
                any(BigDecimal.class),
                eq(new BigDecimal("6.01"))
        )).thenReturn(DeductResult.SUCCESS);

        AuctionResult result = auctionService.execute(
                "request-002",
                "110000",
                "GAME"
        );

        assertEquals(AuctionResult.Status.WON, result.status());
        assertEquals(2L, result.winnerAdId());
        assertEquals(
                new BigDecimal("6.01"),
                result.clearingPrice()
        );

        verify(budgetService, times(2))
                .tryDeduct(
                        anyString(),
                        anyLong(),
                        any(BigDecimal.class),
                        any(BigDecimal.class)
                );
    }

    // 10. 预算回退到最后一名，按保留价成交
    @Test
    void testBudgetFallbackToReservePrice() {
        when(adMapper.findCandidates("110000", "GAME"))
                .thenReturn(List.of(
                        createAd(1, "10.00"),
                        createAd(2, "8.00")
                ));

        when(budgetService.tryDeduct(
                eq("request-fallback"),
                eq(1L),
                any(BigDecimal.class),
                eq(new BigDecimal("8.01"))
        )).thenReturn(DeductResult.INSUFFICIENT);

        when(budgetService.tryDeduct(
                eq("request-fallback"),
                eq(2L),
                any(BigDecimal.class),
                eq(new BigDecimal("2.00"))
        )).thenReturn(DeductResult.SUCCESS);

        AuctionResult result = auctionService.execute(
                "request-fallback",
                "110000",
                "GAME"
        );

        assertEquals(AuctionResult.Status.WON, result.status());
        assertEquals(2L, result.winnerAdId());
        assertEquals(
                new BigDecimal("2.00"),
                result.clearingPrice()
        );

        verify(budgetService, times(2))
                .tryDeduct(
                        anyString(),
                        anyLong(),
                        any(BigDecimal.class),
                        any(BigDecimal.class)
                );
    }

    // 11. 没有候选广告
    @Test
    void testNoCandidates() {
        when(adMapper.findCandidates("110000", "GAME"))
                .thenReturn(List.of());

        AuctionResult result = auctionService.execute(
                "request-003",
                "110000",
                "GAME"
        );

        assertEquals(
                AuctionResult.Status.NO_AD,
                result.status()
        );

        verifyNoInteractions(budgetService);
    }

    // 12. 所有候选预算不足
    @Test
    void testAllCandidatesBudgetInsufficient() {
        when(adMapper.findCandidates("110000", "GAME"))
                .thenReturn(List.of(
                        createAd(1, "10.00"),
                        createAd(2, "8.00")
                ));

        when(budgetService.tryDeduct(
                anyString(),
                anyLong(),
                any(BigDecimal.class),
                any(BigDecimal.class)
        )).thenReturn(DeductResult.INSUFFICIENT);

        AuctionResult result = auctionService.execute(
                "request-all-insufficient",
                "110000",
                "GAME"
        );

        assertEquals(
                AuctionResult.Status.NO_AD,
                result.status()
        );

        verify(budgetService, times(2))
                .tryDeduct(
                        anyString(),
                        anyLong(),
                        any(BigDecimal.class),
                        any(BigDecimal.class)
                );
    }

    // 13. 预算结果未知，停止回退
    @Test
    void testUnknownBudgetResultStopsAuction() {
        when(adMapper.findCandidates("110000", "GAME"))
                .thenReturn(List.of(
                        createAd(1, "10.00"),
                        createAd(2, "8.00")
                ));

        when(budgetService.tryDeduct(
                anyString(),
                anyLong(),
                any(BigDecimal.class),
                any(BigDecimal.class)
        )).thenReturn(DeductResult.UNKNOWN);

        AuctionResult result = auctionService.execute(
                "request-004",
                "110000",
                "GAME"
        );

        assertEquals(
                AuctionResult.Status.PENDING,
                result.status()
        );

        verify(budgetService, times(1))
                .tryDeduct(
                        anyString(),
                        anyLong(),
                        any(BigDecimal.class),
                        any(BigDecimal.class)
                );
    }

    // 14. 空列表调用计价方法时拒绝
    @Test
    void testCalculatePriceWithEmptyList() {
        assertThrows(
                IllegalArgumentException.class,
                () -> auctionService.calculatePrice(List.of(), 0)
        );
    }
}
