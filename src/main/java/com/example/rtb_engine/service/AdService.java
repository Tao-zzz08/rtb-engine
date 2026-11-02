
package com.example.rtb_engine.service;

import com.example.rtb_engine.dto.AdCandidateResponse;
import com.example.rtb_engine.entity.Ad;
import com.example.rtb_engine.mapper.AdMapper;
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
