package com.zsj.meetingagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 个人 AI 模拟面试 Agent 后端启动入口。
 * 使用独立包名和项目命名，避免沿用参考项目的作者风格。
 */
@SpringBootApplication
public class MeetingAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeetingAgentApplication.class, args);
    }
}
