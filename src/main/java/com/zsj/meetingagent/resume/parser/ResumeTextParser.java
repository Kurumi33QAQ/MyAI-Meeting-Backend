package com.zsj.meetingagent.resume.parser;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 简历文本解析器。
 * 当前先做轻量规则解析，提取可读摘要；结构化 chunk 和更细粒度字段会在 RAG 阶段增强。
 */
@Component
public class ResumeTextParser {

    public String buildSummary(String content) {
        String normalized = normalize(content);
        List<String> hits = new ArrayList<>();
        collectIfContains(normalized, hits, "Java");
        collectIfContains(normalized, hits, "Spring");
        collectIfContains(normalized, hits, "Spring Boot");
        collectIfContains(normalized, hits, "MySQL");
        collectIfContains(normalized, hits, "Redis");
        collectIfContains(normalized, hits, "MongoDB");
        collectIfContains(normalized, hits, "项目");
        String prefix = normalized.substring(0, Math.min(normalized.length(), 180));
        if (hits.isEmpty()) {
            return prefix;
        }
        return "关键词：" + String.join("、", hits) + "。摘要：" + prefix;
    }

    private void collectIfContains(String content, List<String> hits, String keyword) {
        if (content.toLowerCase().contains(keyword.toLowerCase()) && !hits.contains(keyword)) {
            hits.add(keyword);
        }
    }

    private String normalize(String content) {
        return content == null ? "" : content.replaceAll("\\s+", " ").trim();
    }
}
