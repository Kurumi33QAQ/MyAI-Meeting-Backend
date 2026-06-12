package com.zsj.meetingagent.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.ai.mock-enabled=true"
})
@AutoConfigureMockMvc
class SystemReadinessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void readinessCanBeLoadedWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/system/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.service").value("meeting-agent-backend"))
                .andExpect(jsonPath("$.data.dependencies.mysql.status").value("UP"))
                .andExpect(jsonPath("$.data.dependencies.mongodb.status").value("UP"))
                .andExpect(jsonPath("$.data.dependencies.redis.status").value("UP"))
                .andExpect(jsonPath("$.data.dependencies.ai.mockEnabled").value(true))
                .andExpect(jsonPath("$.data.dependencies.evaluation.caseCount", is(greaterThan(0))))
                .andExpect(jsonPath("$.data.dependencies.pgvector.enabled").value(false))
                .andExpect(jsonPath("$.data.dependencies.ocr.enabled").value(false))
                .andExpect(jsonPath("$.data.dependencies.media.asrProvider").value("local"))
                .andExpect(jsonPath("$.data.dependencies.media.ttsProvider").value("local"));
    }
}
