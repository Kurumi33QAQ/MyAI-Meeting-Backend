package com.zsj.meetingagent.knowledge.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zsj.meetingagent.knowledge.config.JobIntelligenceProperties;
import com.zsj.meetingagent.knowledge.model.JobIntelligenceReport;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 岗位情报联网搜索测试。
 * 验证无岗位时不会搜索，以及搜索成功时会保留网页来源。
 */
class TavilyJobIntelligenceSearchServiceTest {

    @Test
    void noJobContextSkipsWebSearch() {
        JobIntelligenceProperties properties = new JobIntelligenceProperties();
        properties.setApiKey("test-key");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TavilyJobIntelligenceSearchService service = new TavilyJobIntelligenceSearchService(
                properties,
                new ObjectMapper(),
                builder.build()
        );

        JobIntelligenceReport report = service.search("", "", "");

        assertThat(report.attempted()).isFalse();
        assertThat(report.message()).contains("仅根据简历");
        server.verify();
    }

    @Test
    void jobContextUsesTavilyAndKeepsSourceUrl() {
        JobIntelligenceProperties properties = new JobIntelligenceProperties();
        properties.setApiKey("test-key");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("/search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(withSuccess("""
                        {
                          "results": [
                            {
                              "title": "示例公司 Java 岗位",
                              "url": "https://example.com/jobs/java",
                              "content": "岗位强调 Spring Boot、MySQL、缓存一致性和线上问题排查。",
                              "score": 0.92
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));
        TavilyJobIntelligenceSearchService service = new TavilyJobIntelligenceSearchService(
                properties,
                new ObjectMapper(),
                builder.build()
        );

        JobIntelligenceReport report = service.search(
                "Java 后端开发",
                "示例公司",
                "负责接口开发和性能优化"
        );

        assertThat(report.successful()).isTrue();
        assertThat(report.sources()).hasSize(1);
        assertThat(report.sources().getFirst().url()).isEqualTo("https://example.com/jobs/java");
        assertThat(report.query()).contains("示例公司", "Java 后端开发");
        server.verify();
    }
}
