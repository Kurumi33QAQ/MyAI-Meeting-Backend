package com.zsj.meetingagent.interview.runtime;

import com.zsj.meetingagent.interview.entity.InterviewQuestionSnapshotDocument;
import com.zsj.meetingagent.interview.entity.InterviewSessionDocument;

import java.util.List;

/**
 * 面试运行态服务接口。
 * 负责把面试过程写入 Redis 热态和 MongoDB 冷快照，并在刷新或重启后恢复会话进度。
 */
public interface InterviewRuntimeService {

    InterviewRuntimeState recordSnapshot(
            InterviewSessionDocument session,
            List<InterviewQuestionSnapshotDocument> questions,
            String stepType,
            String content
    );

    InterviewRuntimeState recover(String username, String sessionId);
}
