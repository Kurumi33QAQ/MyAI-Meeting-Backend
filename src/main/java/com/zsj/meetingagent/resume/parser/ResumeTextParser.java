package com.zsj.meetingagent.resume.parser;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 简历文本解析器。
 * 负责从简历正文中提取中文友好的摘要，优先保留项目经历和核心技术，避免面试出题只看到个人信息。
 */
@Component
public class ResumeTextParser {

    public String buildSummary(String content) {
        String normalized = normalize(content);
        List<String> hits = new ArrayList<>();
        collectIfContains(normalized, hits, "Java");
        collectIfContains(normalized, hits, "Spring");
        collectIfContains(normalized, hits, "Spring Boot");
        collectIfContains(normalized, hits, "Spring Security");
        collectIfContains(normalized, hits, "JWT");
        collectIfContains(normalized, hits, "MySQL");
        collectIfContains(normalized, hits, "Redis");
        collectIfContains(normalized, hits, "RabbitMQ");
        collectIfContains(normalized, hits, "WebSocket");
        collectIfContains(normalized, hits, "MongoDB");
        collectIfContains(normalized, hits, "项目");
        String basic = normalized.substring(0, Math.min(normalized.length(), 140));
        String project = extractAround(normalized, List.of("项目经验", "项目经历", "MyMallPlatform", "项目简介"), 420);
        String skills = extractAround(normalized, List.of("个人技能", "专业技能", "技能栈"), 260);
        String prefix = joinNonBlank("；", basic, project, skills);
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

    private String extractAround(String content, List<String> anchors, int maxLength) {
        for (String anchor : anchors) {
            int index = content.indexOf(anchor);
            if (index >= 0) {
                int end = Math.min(content.length(), index + maxLength);
                return content.substring(index, end).trim();
            }
        }
        return "";
    }

    private String joinNonBlank(String delimiter, String... values) {
        List<String> nonBlank = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank() && !nonBlank.contains(value)) {
                nonBlank.add(value);
            }
        }
        return String.join(delimiter, nonBlank);
    }
}
