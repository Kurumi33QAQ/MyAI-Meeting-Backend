package com.zsj.meetingagent.interview;

import com.zsj.meetingagent.interview.enums.InterviewSessionStatus;
import com.zsj.meetingagent.interview.rule.FollowUpDecision;
import com.zsj.meetingagent.interview.rule.FollowUpDecisionService;
import com.zsj.meetingagent.interview.rule.FollowUpRuleContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.ai.mock-enabled=true",
        "app.ai.default-model=gpt-4o-mini",
        "spring.ai.openai.chat.options.model=gpt-4o-mini"
})
class FollowUpDecisionServiceTest {

    @Autowired
    private FollowUpDecisionService followUpDecisionService;

    @Test
    void lowScoreAnswerTriggersSpecificFollowUp() {
        FollowUpDecision decision = followUpDecisionService.decide(new FollowUpRuleContext(
                "session-1",
                "question-1",
                "请介绍你简历里的 Java 后端项目",
                "不知道",
                60,
                "回答缺少项目细节，需要补充。",
                "项目理解、技术细节、表达结果",
                "追问技术细节",
                List.of("evidence-1"),
                InterviewSessionStatus.ANSWERING,
                0,
                1
        ));

        assertThat(decision.shouldFollowUp()).isTrue();
        assertThat(decision.followUpQuestion())
                .contains("具体项目模块")
                .contains("亲自负责")
                .doesNotContain("请详细说说");
        assertThat(decision.traceSummary()).contains("低分判断节点");
    }

    @Test
    void highQualityAnswerCanSkipFollowUp() {
        FollowUpDecision decision = followUpDecisionService.decide(new FollowUpRuleContext(
                "session-1",
                "question-1",
                "请介绍你的 Java 后端项目",
                "我负责设计 Spring Boot 订单接口，使用 MySQL 索引优化和 Redis 缓存降低查询耗时，通过压测指标把接口耗时从 600ms 降低到 180ms。",
                95,
                "回答较完整，命中了主要考察点。",
                "项目理解、技术细节、表达结果",
                "追问技术细节",
                List.of("evidence-1"),
                InterviewSessionStatus.ANSWERING,
                0,
                1
        ));

        assertThat(decision.shouldFollowUp()).isFalse();
        assertThat(decision.traceSummary()).contains("最终裁决节点");
    }

    @Test
    void followUpLimitSuppressesNewFollowUp() {
        FollowUpDecision decision = followUpDecisionService.decide(new FollowUpRuleContext(
                "session-1",
                "question-1",
                "请介绍你的 Java 后端项目",
                "不知道",
                60,
                "回答缺少项目细节，需要补充。",
                "项目理解、技术细节、表达结果",
                "追问技术细节",
                List.of("evidence-1"),
                InterviewSessionStatus.ANSWERING,
                1,
                1
        ));

        assertThat(decision.shouldFollowUp()).isFalse();
        assertThat(decision.traceSummary()).contains("追问次数限制节点");
    }

    @Test
    void mediumScoreAiSuggestionProducesTechnologySpecificFollowUp() {
        FollowUpDecision decision = followUpDecisionService.decide(new FollowUpRuleContext(
                "session-1",
                "question-1",
                "请介绍你负责的后端项目",
                "我负责登录鉴权，使用 JWT 和 Spring Security，并通过 Redis 管理 token 黑名单。",
                82,
                "技术栈较丰富，但需要补充技术选型理由和具体实现细节。",
                "个人职责、技术实现、方案取舍",
                "追问鉴权方案的完整链路",
                List.of("evidence-1"),
                InterviewSessionStatus.ANSWERING,
                0,
                1
        ));

        assertThat(decision.shouldFollowUp()).isTrue();
        assertThat(decision.followUpQuestion())
                .contains("Redis")
                .contains("可验证结果")
                .contains("响应时间")
                .doesNotContain("你没有展开的技术细节");
    }

    @Test
    void highScoreAnswerSkipsMinorAiSuggestionFollowUp() {
        FollowUpDecision decision = followUpDecisionService.decide(new FollowUpRuleContext(
                "session-1",
                "question-1",
                "请介绍你负责的后端项目",
                "我负责登录鉴权，使用 JWT 和 Spring Security，并通过 Redis 黑名单处理退出登录，接口压测后平均响应时间稳定在 180ms。",
                90,
                "回答整体完整，如果继续优化，可以补充少量边界场景。",
                "个人职责、技术实现、方案取舍",
                "追问鉴权方案的完整链路",
                List.of("evidence-1"),
                InterviewSessionStatus.ANSWERING,
                0,
                1
        ));

        assertThat(decision.shouldFollowUp()).isFalse();
        assertThat(decision.traceSummary())
                .contains("高质量阈值")
                .contains("最终裁决节点");
    }

    @Test
    void databaseWordDoesNotCountAsQuantitativeEvidence() {
        FollowUpDecision decision = followUpDecisionService.decide(new FollowUpRuleContext(
                "session-1",
                "question-1",
                "请介绍你的数据库设计工作",
                "我负责数据库设计和接口开发，并实现了订单创建、库存扣减和支付状态更新模块，但暂时没有说明优化结果。",
                80,
                "回答基本完整。",
                "数据库设计、索引、结果验证",
                "追问数据库设计结果",
                List.of("evidence-1"),
                InterviewSessionStatus.ANSWERING,
                0,
                1
        ));

        assertThat(decision.shouldFollowUp()).isTrue();
        assertThat(decision.followUpQuestion())
                .contains("数据库方案")
                .contains("可验证结果");
    }

    @Test
    void redisStringAndSetAnswerGetsDataStructureSpecificFollowUp() {
        FollowUpDecision decision = followUpDecisionService.decide(new FollowUpRuleContext(
                "session-1",
                "question-1",
                "你在项目中如何使用 Redis 优化数据访问？",
                "我没有压测过，用了 String、Set 的数据结构。",
                65,
                "回答提到了 Redis String 和 Set，但没有说明业务数据、key 设计和选型原因。",
                "Redis 数据结构选型、key 设计、性能验证",
                "继续追问 Redis 数据结构的业务用途和选型原因",
                List.of("evidence-1"),
                InterviewSessionStatus.ANSWERING,
                0,
                1
        ));

        assertThat(decision.shouldFollowUp()).isTrue();
        assertThat(decision.followUpQuestion())
                .contains("String")
                .contains("Set")
                .contains("key")
                .contains("其他结构")
                .doesNotContain("问题现象、排查过程");
    }
}
