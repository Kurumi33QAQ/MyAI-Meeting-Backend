package com.zsj.meetingagent.rag.service.impl;

import com.zsj.meetingagent.rag.model.StructuredChunk;
import com.zsj.meetingagent.rag.service.StructuredChunkService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认结构化 chunk 服务。
 * 阶段 7 先按业务章节切分，避免简单按固定字数切断项目经历和 JD 要求。
 */
@Service
public class DefaultStructuredChunkService implements StructuredChunkService {

    private static final int MAX_CHUNK_LENGTH = 700;

    @Override
    public List<StructuredChunk> chunkResume(String resumeText) {
        Map<String, List<String>> sections = new LinkedHashMap<>();
        sections.put("基本信息", List.of("基本信息", "个人信息", "姓名", "联系方式"));
        sections.put("教育经历", List.of("教育经历", "教育背景", "学校", "专业"));
        sections.put("技能栈", List.of("技能", "技术栈", "专业技能", "掌握"));
        sections.put("项目经历", List.of("项目经历", "项目经验", "项目"));
        sections.put("实习经历", List.of("实习经历", "工作经历", "实践经历"));
        sections.put("竞赛经历", List.of("竞赛", "比赛", "获奖"));
        sections.put("自我评价", List.of("自我评价", "个人评价", "优势"));
        return chunkBySection("RESUME", resumeText, sections, "简历");
    }

    @Override
    public List<StructuredChunk> chunkJobDescription(String jobTitle, String companyName, String jobDescription) {
        String text = """
                公司：%s
                岗位：%s
                岗位描述：
                %s
                """.formatted(blankToDefault(companyName, "未填写"), blankToDefault(jobTitle, "未填写"), blankToDefault(jobDescription, ""));
        Map<String, List<String>> sections = new LinkedHashMap<>();
        sections.put("岗位职责", List.of("岗位职责", "工作职责", "职责", "负责"));
        sections.put("任职要求", List.of("任职要求", "岗位要求", "要求", "能力"));
        sections.put("技术栈", List.of("技术栈", "Java", "Spring", "MySQL", "Redis", "MongoDB"));
        sections.put("加分项", List.of("加分", "优先", "熟悉", "经验"));
        return chunkBySection("JOB_DESCRIPTION", text, sections, "岗位JD");
    }

    @Override
    public List<StructuredChunk> chunkInterviewQuestionBank(String question, String referenceAnswer, String evaluationPoints) {
        List<StructuredChunk> chunks = new ArrayList<>();
        String content = """
                题目：%s
                参考答案：%s
                考察点：%s
                """.formatted(question, referenceAnswer, evaluationPoints);
        chunks.add(new StructuredChunk(
                "QUESTION_BANK",
                "面试题",
                0,
                1,
                content,
                shorten(question, 120),
                "question-bank",
                "{\"documentType\":\"QUESTION_BANK\",\"sectionName\":\"面试题\"}"
        ));
        return chunks;
    }

    private List<StructuredChunk> chunkBySection(
            String documentType,
            String text,
            Map<String, List<String>> sectionKeywords,
            String fallbackTag
    ) {
        List<String> paragraphs = splitParagraphs(text);
        List<StructuredChunk> chunks = new ArrayList<>();
        int order = 1;
        for (Map.Entry<String, List<String>> entry : sectionKeywords.entrySet()) {
            List<String> matched = paragraphs.stream()
                    .filter(paragraph -> containsAny(paragraph, entry.getValue()))
                    .toList();
            String content = String.join("\n", matched);
            if (StringUtils.hasText(content)) {
                addSectionChunks(chunks, documentType, entry.getKey(), order, content, fallbackTag);
                order++;
            }
        }
        if (chunks.isEmpty() && StringUtils.hasText(text)) {
            addSectionChunks(chunks, documentType, "全文摘要", order, text, fallbackTag);
        }
        return chunks;
    }

    private void addSectionChunks(
            List<StructuredChunk> chunks,
            String documentType,
            String sectionName,
            int sectionOrder,
            String content,
            String tag
    ) {
        List<String> pieces = splitByLength(content, MAX_CHUNK_LENGTH);
        for (int index = 0; index < pieces.size(); index++) {
            String piece = pieces.get(index).trim();
            if (!piece.isBlank()) {
                chunks.add(new StructuredChunk(
                        documentType,
                        sectionName,
                        index,
                        sectionOrder,
                        piece,
                        shorten(piece, 160),
                        tag + "," + sectionName,
                        "{\"documentType\":\"%s\",\"sectionName\":\"%s\",\"chunkIndex\":%d,\"sectionOrder\":%d}"
                                .formatted(documentType, sectionName, index, sectionOrder)
                ));
            }
        }
    }

    private List<String> splitParagraphs(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        return text.lines()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private List<String> splitByLength(String content, int maxLength) {
        if (content.length() <= maxLength) {
            return List.of(content);
        }
        List<String> pieces = new ArrayList<>();
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + maxLength, content.length());
            pieces.add(content.substring(start, end));
            start = end;
        }
        return pieces;
    }

    private boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    private String blankToDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private String shorten(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.substring(0, Math.min(trimmed.length(), maxLength));
    }
}
