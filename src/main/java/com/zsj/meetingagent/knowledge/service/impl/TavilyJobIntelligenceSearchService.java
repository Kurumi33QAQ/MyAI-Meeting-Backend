package com.zsj.meetingagent.knowledge.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zsj.meetingagent.knowledge.config.JobIntelligenceProperties;
import com.zsj.meetingagent.knowledge.model.JobIntelligenceReport;
import com.zsj.meetingagent.knowledge.model.JobIntelligenceSource;
import com.zsj.meetingagent.knowledge.service.JobIntelligenceSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 Tavily Search API 的岗位情报搜索实现。
 * 外部服务不可用时返回中文降级状态，不阻断简历上传和模拟面试主流程。
 */
@Service
public class TavilyJobIntelligenceSearchService implements JobIntelligenceSearchService {

    private static final Logger log = LoggerFactory.getLogger(TavilyJobIntelligenceSearchService.class);

    private final JobIntelligenceProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Autowired
    public TavilyJobIntelligenceSearchService(JobIntelligenceProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, createRestClient(properties));
    }

    TavilyJobIntelligenceSearchService(
            JobIntelligenceProperties properties,
            ObjectMapper objectMapper,
            RestClient restClient
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClient;
    }

    @Override
    public JobIntelligenceReport search(String jobTitle, String companyName, String jobDescription) {
        if (!hasJobContext(jobTitle, companyName, jobDescription)) {
            return JobIntelligenceReport.noJobContext();
        }
        if (!properties.isEnabled()) {
            return JobIntelligenceReport.disabled("岗位情报联网搜索已关闭，本次使用用户填写的岗位信息和简历出题。");
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            return JobIntelligenceReport.disabled("未配置 TAVILY_API_KEY，本次使用用户填写的岗位信息和简历出题。");
        }

        String query = buildQuery(jobTitle, companyName, jobDescription);
        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("query", query);
            requestBody.put("topic", "general");
            requestBody.put("search_depth", "basic");
            requestBody.put("include_answer", false);
            requestBody.put("include_raw_content", false);
            requestBody.put("max_results", normalizeMaxResults(properties.getMaxResults()));

            String responseBody = restClient.post()
                    .uri("/search")
                    .header("Authorization", "Bearer " + properties.getApiKey().trim())
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
            List<JobIntelligenceSource> sources = parseSources(responseBody);
            if (sources.isEmpty()) {
                return JobIntelligenceReport.failed(query, "联网搜索未返回可用岗位资料，本次继续使用简历和用户填写信息出题。");
            }
            return new JobIntelligenceReport(
                    true,
                    true,
                    query,
                    "已检索公开岗位资料 " + sources.size() + " 条，并作为出题证据。",
                    sources
            );
        } catch (Exception ex) {
            log.warn("岗位情报联网搜索失败，query={}", query, ex);
            return JobIntelligenceReport.failed(query, "岗位情报联网搜索失败，本次继续使用简历和用户填写信息出题。");
        }
    }

    private List<JobIntelligenceSource> parseSources(String responseBody) throws Exception {
        if (!StringUtils.hasText(responseBody)) {
            return List.of();
        }
        JsonNode results = objectMapper.readTree(responseBody).path("results");
        if (!results.isArray()) {
            return List.of();
        }
        List<JobIntelligenceSource> sources = new ArrayList<>();
        for (JsonNode result : results) {
            String url = result.path("url").asText("").trim();
            String content = result.path("content").asText("").trim();
            if (!StringUtils.hasText(url) || !StringUtils.hasText(content)) {
                continue;
            }
            sources.add(new JobIntelligenceSource(
                    result.path("title").asText("公开岗位资料").trim(),
                    url,
                    shorten(content, 1200),
                    result.path("score").asDouble(0.0)
            ));
        }
        return List.copyOf(sources);
    }

    private String buildQuery(String jobTitle, String companyName, String jobDescription) {
        StringBuilder query = new StringBuilder();
        if (StringUtils.hasText(companyName)) {
            query.append(companyName.trim()).append(' ');
        }
        if (StringUtils.hasText(jobTitle)) {
            query.append(jobTitle.trim()).append(' ');
        }
        query.append("招聘 岗位职责 任职要求 技术栈 面试经验 常见面试题");
        if (StringUtils.hasText(jobDescription)) {
            query.append(" JD关键词 ").append(shorten(jobDescription, 220));
        }
        return query.toString();
    }

    private boolean hasJobContext(String jobTitle, String companyName, String jobDescription) {
        return StringUtils.hasText(jobTitle)
                || StringUtils.hasText(companyName)
                || StringUtils.hasText(jobDescription);
    }

    private int normalizeMaxResults(int maxResults) {
        return Math.max(1, Math.min(maxResults, 10));
    }

    private String shorten(String value, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        return normalized.substring(0, Math.min(normalized.length(), maxLength));
    }

    private static RestClient createRestClient(JobIntelligenceProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMillis = Math.toIntExact(properties.getTimeout().toMillis());
        requestFactory.setConnectTimeout(timeoutMillis);
        requestFactory.setReadTimeout(timeoutMillis);
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
