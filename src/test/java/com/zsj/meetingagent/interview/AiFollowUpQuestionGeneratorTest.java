package com.zsj.meetingagent.interview;

import com.zsj.meetingagent.ai.config.AiModelProperties;
import com.zsj.meetingagent.ai.dto.AiChatRequest;
import com.zsj.meetingagent.ai.service.AiChatService;
import com.zsj.meetingagent.ai.vo.AiChatResponse;
import com.zsj.meetingagent.interview.followup.FollowUpQuestionRequest;
import com.zsj.meetingagent.interview.followup.impl.AiFollowUpQuestionGenerator;
import com.zsj.meetingagent.interview.prompt.InterviewPromptBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AI 追问生成器测试。
 * 验证具体问题可以通过，而泛化问题和模型异常会回退到后端规则问题。
 */
class AiFollowUpQuestionGeneratorTest {

    @Test
    void acceptsConcreteModelQuestion() {
        AiChatService aiChatService = mock(AiChatService.class);
        when(aiChatService.chat(any(AiChatRequest.class))).thenReturn(new AiChatResponse(
                "Redis 黑名单的 key、过期时间分别如何设计，用户注销后怎样保证 JWT 立即失效？",
                "test-model",
                "mock",
                1,
                false
        ));
        AiFollowUpQuestionGenerator generator = generator(aiChatService);

        String result = generator.generate(request());

        assertThat(result)
                .contains("Redis")
                .contains("JWT")
                .endsWith("？");
    }

    @Test
    void rejectsGenericQuestionAndUsesRuleFallback() {
        AiChatService aiChatService = mock(AiChatService.class);
        when(aiChatService.chat(any(AiChatRequest.class))).thenReturn(new AiChatResponse(
                "请补充你没有展开的技术细节。",
                "test-model",
                "mock",
                1,
                false
        ));
        AiFollowUpQuestionGenerator generator = generator(aiChatService);

        String result = generator.generate(request());

        assertThat(result)
                .isEqualTo("请具体说明 Redis 黑名单的 key 和过期时间如何设计？")
                .doesNotContain("没有展开");
    }

    @Test
    void rejectsQuestionThatIgnoresTechnologyMentionedInAnswer() {
        AiChatService aiChatService = mock(AiChatService.class);
        when(aiChatService.chat(any(AiChatRequest.class))).thenReturn(new AiChatResponse(
                "你能具体说明这个方案是如何实现的吗？",
                "test-model",
                "mock",
                1,
                false
        ));
        AiFollowUpQuestionGenerator generator = generator(aiChatService);

        assertThat(generator.generate(request()))
                .isEqualTo("请具体说明 Redis 黑名单的 key 和过期时间如何设计？");
    }

    @Test
    void normalizesTrailingPeriodBeforeQuestionMark() {
        AiChatService aiChatService = mock(AiChatService.class);
        when(aiChatService.chat(any(AiChatRequest.class))).thenReturn(new AiChatResponse(
                "你在解析 JWT 时是否设计了 Redis 黑名单，key 如何命名以及过期时间如何设置。",
                "test-model",
                "mock",
                1,
                false
        ));
        AiFollowUpQuestionGenerator generator = generator(aiChatService);

        assertThat(generator.generate(request()))
                .endsWith("？")
                .doesNotContain("。？");
    }

    @Test
    void rejectsThirdPersonCompanyInterviewerNarration() {
        AiChatService aiChatService = mock(AiChatService.class);
        when(aiChatService.chat(any(AiChatRequest.class))).thenReturn(new AiChatResponse(
                "如果字节跳动的面试官继续追问岗位核心能力，请结合项目说明问题现象、排查过程和验证结果？",
                "test-model",
                "mock",
                1,
                false
        ));
        AiFollowUpQuestionGenerator generator = generator(aiChatService);

        assertThat(generator.generate(stringSetRequest()))
                .isEqualTo("你提到 Redis 使用了 String 和 Set，请分别说明它们保存什么业务数据、key 如何设计，以及为什么没有选择 Hash 或 ZSet？");
    }

    @Test
    void fallsBackWhenModelCallFails() {
        AiChatService aiChatService = mock(AiChatService.class);
        when(aiChatService.chat(any(AiChatRequest.class))).thenThrow(new IllegalStateException("模型不可用"));
        AiFollowUpQuestionGenerator generator = generator(aiChatService);

        assertThat(generator.generate(request()))
                .isEqualTo("请具体说明 Redis 黑名单的 key 和过期时间如何设计？");
    }

    private AiFollowUpQuestionGenerator generator(AiChatService aiChatService) {
        AiModelProperties properties = new AiModelProperties();
        properties.setDefaultModel("test-model");
        return new AiFollowUpQuestionGenerator(aiChatService, new InterviewPromptBuilder(), properties);
    }

    private FollowUpQuestionRequest request() {
        return new FollowUpQuestionRequest(
                "session-1",
                "负责商城登录鉴权和 Redis 缓存模块。",
                "Java 后端开发实习生",
                "示例公司",
                "熟悉 Java、Redis 和数据库设计",
                "请说明登录鉴权方案",
                "我使用 JWT、Spring Security 和 Redis 黑名单完成登录鉴权。",
                "鉴权链路、退出登录、token 失效",
                "追问黑名单设计",
                "需要补充具体实现和边界条件。",
                "请具体说明 Redis 黑名单的 key 和过期时间如何设计？"
        );
    }

    private FollowUpQuestionRequest stringSetRequest() {
        return new FollowUpQuestionRequest(
                "session-2",
                "负责电商项目缓存、权限和订单模块，使用 Spring Boot、MySQL、Redis。",
                "Java 后端开发实习生",
                "字节跳动",
                "关注数据结构、缓存设计和性能分析",
                "你在项目中如何使用 Redis 优化数据访问？",
                "我没有压测过，用了 String、Set 的数据结构。",
                "Redis 数据结构选型、key 设计、性能验证",
                "继续追问 Redis 数据结构的业务用途和选型原因",
                "回答提到了 Redis String 和 Set，但没有说明业务数据、key 设计和选型原因。",
                "你提到 Redis 使用了 String 和 Set，请分别说明它们保存什么业务数据、key 如何设计，以及为什么没有选择 Hash 或 ZSet？"
        );
    }
}
