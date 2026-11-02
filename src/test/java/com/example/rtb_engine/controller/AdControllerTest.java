
package com.example.rtb_engine.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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

                // 1. 验证 HTTP 状态码
                .andExpect(status().isOk())

                // 2. 验证返回 JSON 数组
                .andExpect(content()
                        .contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$").isArray())

                // 3. 验证候选广告数量
                .andExpect(jsonPath("$.length()").value(2))

                // 4. 验证第一条广告
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("游戏广告A"))
                .andExpect(jsonPath("$[0].categoryCode").value("GAME"))
                .andExpect(jsonPath("$[0].bid").value(10.0))

                // 5. 验证第二条广告
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].title").value("游戏广告B"))
                .andExpect(jsonPath("$[1].categoryCode").value("GAME"))
                .andExpect(jsonPath("$[1].bid").value(8.0))

                // 6. 打印实际 HTTP 响应，方便调试
                .andDo(print());
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
