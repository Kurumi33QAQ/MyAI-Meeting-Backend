package com.zsj.meetingagent.resume;

import com.zsj.meetingagent.resume.parser.ResumeTextParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 简历摘要解析测试。
 * 验证摘要会优先保留项目经历和核心技术，避免只截取到个人信息导致面试出题泛化。
 */
class ResumeTextParserTest {

    private final ResumeTextParser parser = new ResumeTextParser();

    @Test
    void summaryKeepsProjectExperienceAndCoreTechnologies() {
        String resumeText = """
                个人简历
                姓名：张顺军 学校：成都信息工程大学 专业：计算机科学与技术
                获奖经历
                ACM 区域赛铜牌
                项目经验
                2026.1-2026.4 MyMallPlatform 商城后台管理与用户端系统 Java 后端核心开发
                开发技术：Java 21、Spring Boot、Spring Security、JWT、MySQL、Redis、RabbitMQ、WebSocket
                权限认证模块：基于 Spring Security + JWT 实现登录认证与 RBAC 权限控制。
                秒杀活动模块：通过 RabbitMQ 消息队列实现秒杀下单异步化。
                个人技能
                熟悉 Java、MySQL、Redis 和后端接口开发。
                """;

        String summary = parser.buildSummary(resumeText);

        assertThat(summary)
                .contains("MyMallPlatform")
                .contains("RabbitMQ")
                .contains("WebSocket")
                .contains("Spring Security")
                .contains("JWT");
    }
}
