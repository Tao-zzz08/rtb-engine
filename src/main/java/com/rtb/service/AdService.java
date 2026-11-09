
package com.rtb.service;

import com.rtb.dto.AdCandidateResponse;
import com.rtb.entity.Ad;
import com.rtb.mapper.AdMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdService {

    private final AdMapper adMapper;

    public AdService(AdMapper adMapper) {
        this.adMapper = adMapper;
    }

    public List<AdCandidateResponse> findCandidates(
            String region,
            String categoryCode) {

        List<Ad> ads = adMapper.findCandidates(
                region, categoryCode
        );

        return ads.stream()
                .map(ad -> new AdCandidateResponse(
                        ad.getId(),
                        ad.getAdvertiserId(),
                        ad.getTitle(),
                        ad.getCreativeUrl(),
                        ad.getCategoryCode(),
                        ad.getBid()
                ))
                .toList();
    }
}
