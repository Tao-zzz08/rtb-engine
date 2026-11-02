
package com.example.rtb_engine.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // 测试正常查询
    @Test
    void testGetCandidates() throws Exception {

        mockMvc.perform(get("/api/ads/candidates")
                        .param("region", "110000")
                        .param("categoryCode", "GAME"))
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$").isArray());
    }

    // 测试缺少必填参数
    @Test
    void testMissingParameter() throws Exception {

        mockMvc.perform(get("/api/ads/candidates")
                        .param("region", "110000"))
                .andExpect(status().isBadRequest());
    }

    // 测试空参数
    @Test
    void testBlankParameter() throws Exception {

        mockMvc.perform(get("/api/ads/candidates")
                        .param("region", "")
                        .param("categoryCode", "GAME"))
                .andExpect(status().isBadRequest());
    }

    // 测试不存在的广告类别
    @Test
    void testUnknownCategory() throws Exception {

        mockMvc.perform(get("/api/ads/candidates")
                        .param("region", "110000")
                        .param("categoryCode", "UNKNOWN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
