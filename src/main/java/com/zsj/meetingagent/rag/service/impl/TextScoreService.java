package com.zsj.meetingagent.rag.service.impl;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 文本相关性评分工具。
 * 阶段 7 先用轻量关键词和中文 bigram 做本地召回，后续可替换为 Embedding 相似度。
 */
@Component
public class TextScoreService {

    public double score(String query, String candidate) {
        Set<String> queryTerms = terms(query);
        Set<String> candidateTerms = terms(candidate);
        if (queryTerms.isEmpty() || candidateTerms.isEmpty()) {
            return 0.0;
        }
        long hitCount = queryTerms.stream().filter(candidateTerms::contains).count();
        return hitCount * 1.0 / queryTerms.size();
    }

    private Set<String> terms(String text) {
        Set<String> result = new HashSet<>();
        if (!StringUtils.hasText(text)) {
            return result;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        for (String token : normalized.split("[^\\p{IsHan}\\p{Alnum}]+")) {
            if (token.length() >= 2) {
                result.add(token);
            }
            addChineseBigrams(token, result);
        }
        return result;
    }

    private void addChineseBigrams(String token, Set<String> result) {
        StringBuilder chinese = new StringBuilder();
        for (int index = 0; index < token.length(); index++) {
            char current = token.charAt(index);
            if (Character.UnicodeScript.of(current) == Character.UnicodeScript.HAN) {
                chinese.append(current);
            } else {
                flushChineseBigrams(chinese, result);
            }
        }
        flushChineseBigrams(chinese, result);
    }

    private void flushChineseBigrams(StringBuilder chinese, Set<String> result) {
        if (chinese.length() < 2) {
            chinese.setLength(0);
            return;
        }
        for (int index = 0; index < chinese.length() - 1; index++) {
            result.add(chinese.substring(index, index + 2));
        }
        chinese.setLength(0);
    }
}
