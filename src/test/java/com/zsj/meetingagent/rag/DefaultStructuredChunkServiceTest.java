package com.zsj.meetingagent.rag;

import com.zsj.meetingagent.rag.service.impl.DefaultStructuredChunkService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 结构化 chunk 服务测试。
 * 验证简历项目章节会保留标题后的连续正文，给 RAG 检索和面试出题提供完整上下文。
 */
class DefaultStructuredChunkServiceTest {

    private final DefaultStructuredChunkService chunkService = new DefaultStructuredChunkService();

    @Test
    void resumeProjectChunkKeepsFollowingTechnicalDetails() {
        String resumeText = """
                个人简历
                姓名：张顺军
                项目经验
                2026.1-2026.4 MyMallPlatform 商城后台管理与用户端系统 Java 后端核心开发
                开发技术：Java 21、Spring Boot、Spring Security、JWT、MySQL、Redis、RabbitMQ、WebSocket
                权限认证模块：基于 Spring Security + JWT 实现登录认证与 RBAC 权限控制。
                秒杀活动模块：通过 RabbitMQ 消息队列实现秒杀下单异步化。
                通知与客服模块：基于 WebSocket 实现买家通知和客服消息实时推送。
                个人技能
                熟悉 Java、MySQL、Redis 和后端接口开发。
                """;

        var chunks = chunkService.chunkResume(resumeText);

        assertThat(chunks)
                .anySatisfy(chunk -> assertThat(chunk.content())
                        .contains("MyMallPlatform")
                        .contains("RabbitMQ")
                        .contains("WebSocket")
                        .contains("RBAC"));
    }
}
