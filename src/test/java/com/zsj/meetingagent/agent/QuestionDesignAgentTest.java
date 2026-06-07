package com.zsj.meetingagent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zsj.meetingagent.agent.model.InterviewOrchestrationContext;
import com.zsj.meetingagent.agent.role.QuestionDesignAgent;
import com.zsj.meetingagent.rag.vo.EvidenceResponse;
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
        assertThat(questions.get(1).question()).contains("服务端研发").contains("MySQL");
        assertThat(questions)
                .allSatisfy(question -> assertThat(question.question())
                        .doesNotContain("如果")
                        .doesNotContain("面试官继续追问"));
    }

    @Test
    void fallbackQuestionDirectlyInterviewsCandidateInsteadOfNarratingAnotherInterviewer() {
        var questions = agent.designQuestions(new InterviewOrchestrationContext(
                "alice",
                "session-1",
                resume(),
                "Java 后端开发实习生",
                "字节跳动",
                "要求熟悉 Redis 数据结构、缓存设计、MySQL 和高并发",
                4,
                List.of(),
                "无法解析的模型输出"
        ));

        assertThat(questions.get(3).question())
                .contains("Redis")
                .contains("业务数据")
                .contains("key")
                .doesNotContain("如果")
                .doesNotContain("面试官");
    }

    @Test
    void fallbackQuestionsUseResumeTechnologyAndJobCapabilitiesWithoutRepeatingRawJd() {
        String jobDescription = """
                职位描述
                ByteIntern：面向2027届毕业生，为符合岗位要求的同学提供转正机会。
                团队介绍：飞书是 AI 时代先进生产力平台，提供一站式工作协同、组织管理、业务提效工具和深入企业场景的 AI 能力。
                1、负责字节跳动飞书相关产品的服务端研发；
                2、负责团队服务质量、稳定性，从工具、系统上提升团队开发效率；
                3、主要语言为Golang。
                职位要求
                掌握Web后端开发技术：协议、架构、存储、缓存、安全等。
                """;

        var questions = agent.designQuestions(new InterviewOrchestrationContext(
                "buyer_001",
                "session-bytedance",
                resumeWithMallProject(),
                "后端开发实习生",
                "字节跳动-飞书",
                jobDescription,
                8,
                List.of(
                        evidence("项目经历", "MyMallPlatform 使用 Spring Boot、MySQL、Redis、RabbitMQ、WebSocket、JWT、RBAC 完成商城后台和用户端。"),
                        evidence("任职要求", "岗位关注服务端研发、稳定性、开发效率、Web 后端协议、存储、缓存和安全。")
                ),
                "不是 JSON"
        ));

        assertThat(questions).hasSize(8);
        assertThat(questions.stream().map(item -> item.question()).distinct()).hasSize(8);
        assertThat(questions.stream().map(item -> item.question()).toList())
                .anySatisfy(question -> assertThat(question).contains("MyMallPlatform"))
                .anySatisfy(question -> assertThat(question).contains("Redis").contains("key"))
                .anySatisfy(question -> assertThat(question).contains("RabbitMQ"))
                .anySatisfy(question -> assertThat(question).contains("WebSocket"))
                .anySatisfy(question -> assertThat(question).contains("稳定性"));
        assertThat(questions)
                .allSatisfy(question -> assertThat(question.question())
                        .doesNotContain("ByteIntern：面向2027届毕业生")
                        .doesNotContain("如果")
                        .doesNotContain("面试官"));
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

    private ResumeResponse resumeWithMallProject() {
        return new ResumeResponse(
                "resume-2",
                "张顺军-个人简历.pdf",
                "application/pdf",
                2048L,
                "RESUME",
                "项目经验：MyMallPlatform 商城后台管理与用户端系统，使用 Spring Boot、MySQL、Redis、RabbitMQ、WebSocket、JWT、RBAC，负责权限认证、秒杀活动、通知与客服模块。",
                Instant.now(),
                Instant.now()
        );
    }

    private EvidenceResponse evidence(String sectionName, String content) {
        return new EvidenceResponse(
                "ev-" + sectionName,
                "doc-1",
                "source-1",
                "RESUME",
                sectionName,
                content,
                content,
                "test",
                0.9,
                0.95
        );
    }
}
