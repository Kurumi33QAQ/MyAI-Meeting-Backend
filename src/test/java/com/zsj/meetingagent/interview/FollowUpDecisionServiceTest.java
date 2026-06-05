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
    void lowScoreAnswerTriggersFollowUp() {
        FollowUpDecision decision = followUpDecisionService.decide(new FollowUpRuleContext(
                "session-1",
                "question-1",
                "请介绍你的 Java 后端项目",
                "不知道",
                60,
                "回答缺少项目细节，需要补充。",
                "项目理解、技术细节、表达结构",
                "追问技术细节",
                List.of("evidence-1"),
                InterviewSessionStatus.ANSWERING,
                0,
                1
        ));

        assertThat(decision.shouldFollowUp()).isTrue();
        assertThat(decision.followUpQuestion()).contains("具体项目场景");
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
                "项目理解、技术细节、表达结构",
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
                "项目理解、技术细节、表达结构",
                "追问技术细节",
                List.of("evidence-1"),
                InterviewSessionStatus.ANSWERING,
                1,
                1
        ));

        assertThat(decision.shouldFollowUp()).isFalse();
        assertThat(decision.traceSummary()).contains("追问次数限制节点");
    }
}
