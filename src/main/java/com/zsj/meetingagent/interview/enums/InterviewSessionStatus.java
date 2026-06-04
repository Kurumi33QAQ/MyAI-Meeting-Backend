package com.zsj.meetingagent.interview.enums;

/**
 * 模拟面试会话状态。
 * 用状态机约束流程顺序，避免还没生成题目就提交答案、已完成后继续答题等非法操作。
 */
public enum InterviewSessionStatus {
    CREATED,
    QUESTION_GENERATED,
    ANSWERING,
    COMPLETED
}
