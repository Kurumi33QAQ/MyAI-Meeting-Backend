package com.zsj.meetingagent.rag.vo;

import java.util.List;

/**
 * 证据检索响应。
 * 暴露召回数量、最终证据和最高置信度，方便前端调试和后续低置信度拒答。
 */
public record RetrieveEvidenceResponse(
        String query,
        int recalledCount,
        int selectedCount,
        double confidence,
        List<EvidenceResponse> evidenceList
) {
}
