package com.zsj.meetingagent.interview.followup;

/**
 * 面试追问生成器。
 * LiteFlow 负责判断是否需要追问，本接口负责把裁决方向转换为具体、可直接作答的问题。
 */
public interface FollowUpQuestionGenerator {

    String generate(FollowUpQuestionRequest request);
}
