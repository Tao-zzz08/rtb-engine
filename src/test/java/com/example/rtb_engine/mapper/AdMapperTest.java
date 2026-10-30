package com.example.rtb_engine.mapper;

import com.example.rtb_engine.entity.Ad;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AdMapperTest {
    @Autowired
    private AdMapper adMapper;

    @Test
    void testFindCandidates() {

        List<Ad> ads = adMapper.findCandidates(
                "110000",
                "GAME"
        );

        assertNotNull(ads);

        for (Ad ad : ads) {
            System.out.println(
                    "广告ID: " + ad.getId()
                            + ", 标题: " + ad.getTitle()
                            + ", 出价: " + ad.getBid()
            );
        }
    }
}
