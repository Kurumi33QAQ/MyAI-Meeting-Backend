package com.zsj.meetingagent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zsj.meetingagent.agent.model.InterviewOrchestrationContext;
import com.zsj.meetingagent.agent.role.QuestionDesignAgent;
import com.zsj.meetingagent.resume.vo.ResumeResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionDesignAgentTest {

    private final QuestionDesignAgent agent = new QuestionDesignAgent(new ObjectMapper());

    @Test
    void shouldUseQuestionsFromLargeModelJsonFirst() {
        String aiSuggestion = """
                ```json
                {
                  "questions": [
                    {
                      "question": "你投递的是字节跳动 Java 后端实习，请结合订单系统项目说明你如何设计高并发下单接口。",
                      "referenceAnswer": "需要说明接口拆分、事务边界、库存扣减和幂等设计。",
                      "evaluationPoints": "高并发、事务一致性、接口设计、岗位匹配度",
                      "followUpDirection": "继续追问库存超卖和重复提交处理"
                    }
                  ]
                }
                ```
                """;

        var questions = agent.designQuestions(new InterviewOrchestrationContext(
                "alice",
                "session-1",
                resume(),
                "Java 后端开发实习生",
                "字节跳动",
                "要求熟悉高并发、MySQL、Redis、分布式系统",
                1,
                List.of(),
                aiSuggestion
        ));

        assertThat(questions).hasSize(1);
        assertThat(questions.getFirst().question()).contains("字节跳动 Java 后端实习");
        assertThat(questions.getFirst().referenceAnswer()).contains("幂等设计");
        assertThat(questions.getFirst().evaluationPoints()).contains("高并发");
    }

    @Test
    void shouldFallbackToDynamicQuestionWhenLargeModelDoesNotReturnJson() {
        var questions = agent.designQuestions(new InterviewOrchestrationContext(
                "alice",
                "session-1",
                resume(),
                "支付平台 Java 后端",
                "蚂蚁集团",
                "负责支付链路稳定性、MySQL、Redis、接口性能优化",
                2,
                List.of(),
                "这里是一段非 JSON 文本"
        ));

        assertThat(questions).hasSize(2);
        assertThat(questions.getFirst().question()).contains("蚂蚁集团");
        assertThat(questions.getFirst().question()).contains("支付平台 Java 后端");
        assertThat(questions.get(1).question()).contains("支付链路稳定性");
    }

    private ResumeResponse resume() {
        return new ResumeResponse(
                "resume-1",
                "简历.pdf",
                "application/pdf",
                1024L,
                "RESUME",
                "候选人做过订单系统和缓存优化项目。",
                Instant.now(),
                Instant.now()
        );
    }
}
