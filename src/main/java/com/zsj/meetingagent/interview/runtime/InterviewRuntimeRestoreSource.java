package com.zsj.meetingagent.interview.runtime;

/**
 * 面试运行态恢复来源。
 * 用来标记本次恢复来自 Redis 热态、MongoDB 冷快照，还是根据会话和题目重新构建。
 */
public enum InterviewRuntimeRestoreSource {

    HOT_REDIS("Redis 热态缓存"),
    COLD_MONGO("MongoDB 冷快照"),
    REBUILT_FROM_SESSION("会话和题目重建");

    private final String description;

    InterviewRuntimeRestoreSource(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
