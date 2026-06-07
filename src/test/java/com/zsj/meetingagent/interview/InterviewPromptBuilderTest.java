package com.zsj.meetingagent.interview;

import com.zsj.meetingagent.interview.prompt.InterviewPromptBuilder;
import com.zsj.meetingagent.interview.followup.FollowUpQuestionRequest;
import com.zsj.meetingagent.resume.vo.ResumeResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 模拟面试 Prompt 测试。
 * 验证用户未填写岗位时不会被系统自动归类为 Java 后端岗位。
 */
class InterviewPromptBuilderTest {

    @Test
    void emptyJobContextUsesResumeOnlyMode() {
        ResumeResponse resume = new ResumeResponse(
                "resume-1",
                "resume.txt",
                "text/plain",
                100L,
                "RESUME",
                "参与校园二手交易平台，负责订单模块和接口测试。",
                Instant.now(),
                Instant.now()
        );

        String prompt = new InterviewPromptBuilder().buildQuestionPrompt(
                resume,
                "",
                "",
                "",
                5,
                List.of()
        );

        assertThat(prompt).contains("只根据候选人简历", "不要假设候选人应聘");
        assertThat(prompt).doesNotContain("目标岗位：", "Java 后端模拟面试官");
    }

    @Test
    void followUpPromptUsesResumeAndAnswerButForbidsThirdPersonInterviewerNarration() {
        String prompt = new InterviewPromptBuilder().buildFollowUpQuestionPrompt(
                new FollowUpQuestionRequest(
                        "session-1",
                        "负责商城登录鉴权和 Redis 缓存，使用 Spring Boot、MySQL、Redis。",
                        "Java 后端开发实习生",
                        "字节跳动",
                        "关注缓存设计、数据结构选型和性能验证",
                        "你如何使用 Redis 优化项目中的数据访问？",
                        "我没有压测，使用了 String 和 Set。",
                        "Redis 数据结构选型、key 设计、性能验证",
                        "继续深挖 String 和 Set 的业务用途",
                        "回答缺少具体业务数据、key 设计和选型依据。",
                        "你提到 Redis 使用了 String 和 Set，请分别说明它们保存什么业务数据、key 如何设计，以及为什么没有选择 Hash 或 ZSet？"
                )
        );

        assertThat(prompt)
                .contains("商城登录鉴权和 Redis 缓存")
                .contains("String 和 Set")
                .contains("字节跳动")
                .contains("直接向候选人提问")
                .contains("禁止出现“如果某公司的面试官继续追问”")
                .contains("为什么没有选择 Hash 或 ZSet");
    }
}
