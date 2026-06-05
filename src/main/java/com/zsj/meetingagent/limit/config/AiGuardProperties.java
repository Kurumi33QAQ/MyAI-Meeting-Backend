package com.zsj.meetingagent.limit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * AI 调用稳定性治理配置。
 * 统一管理 Single-flight、限流、等待回放、调用超时和降级文案，避免治理参数散落在业务代码中。
 */
@ConfigurationProperties(prefix = "app.ai-guard")
public class AiGuardProperties {

    private boolean enabled = true;

    private boolean redisRequired = false;

    private String namespace = "meetingagent:ai";

    private Duration lockTtl = Duration.ofSeconds(30);

    private Duration resultTtl = Duration.ofMinutes(10);

    private Duration waitTimeout = Duration.ofSeconds(6);

    private Duration pollInterval = Duration.ofMillis(100);

    private Duration callTimeout = Duration.ofSeconds(25);

    private int userLimitPerMinute = 20;

    private String fallbackAnswer = "AI 服务暂时不稳定，本次先给出降级回复：请稍后重试，或把问题拆得更具体后再次提交。";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRedisRequired() {
        return redisRequired;
    }

    public void setRedisRequired(boolean redisRequired) {
        this.redisRequired = redisRequired;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Duration getLockTtl() {
        return lockTtl;
    }

    public void setLockTtl(Duration lockTtl) {
        this.lockTtl = lockTtl;
    }

    public Duration getResultTtl() {
        return resultTtl;
    }

    public void setResultTtl(Duration resultTtl) {
        this.resultTtl = resultTtl;
    }

    public Duration getWaitTimeout() {
        return waitTimeout;
    }

    public void setWaitTimeout(Duration waitTimeout) {
        this.waitTimeout = waitTimeout;
    }

    public Duration getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(Duration pollInterval) {
        this.pollInterval = pollInterval;
    }

    public Duration getCallTimeout() {
        return callTimeout;
    }

    public void setCallTimeout(Duration callTimeout) {
        this.callTimeout = callTimeout;
    }

    public int getUserLimitPerMinute() {
        return userLimitPerMinute;
    }

    public void setUserLimitPerMinute(int userLimitPerMinute) {
        this.userLimitPerMinute = userLimitPerMinute;
    }

    public String getFallbackAnswer() {
        return fallbackAnswer;
    }

    public void setFallbackAnswer(String fallbackAnswer) {
        this.fallbackAnswer = fallbackAnswer;
    }
}
