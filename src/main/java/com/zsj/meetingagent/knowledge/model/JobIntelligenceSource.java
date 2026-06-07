package com.zsj.meetingagent.knowledge.model;

/**
 * 岗位情报搜索来源。
 * 保存网页标题、链接、摘要和相关度，便于出题时引用并追踪证据来源。
 */
public record JobIntelligenceSource(
        String title,
        String url,
        String content,
        double score
) {
}
