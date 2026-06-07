package com.zsj.meetingagent.knowledge.model;

import java.util.List;

/**
 * 岗位情报搜索报告。
 * 区分未填写岗位、未配置搜索服务、搜索成功和搜索失败，避免把降级结果伪装成联网数据。
 */
public record JobIntelligenceReport(
        boolean attempted,
        boolean successful,
        String query,
        String message,
        List<JobIntelligenceSource> sources
) {

    public static JobIntelligenceReport noJobContext() {
        return new JobIntelligenceReport(false, false, "", "用户未填写岗位信息，本次仅根据简历生成题目。", List.of());
    }

    public static JobIntelligenceReport disabled(String message) {
        return new JobIntelligenceReport(false, false, "", message, List.of());
    }

    public static JobIntelligenceReport failed(String query, String message) {
        return new JobIntelligenceReport(true, false, query, message, List.of());
    }
}
