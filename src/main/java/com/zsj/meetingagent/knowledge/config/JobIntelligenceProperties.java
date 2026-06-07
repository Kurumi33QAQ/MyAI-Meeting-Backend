package com.zsj.meetingagent.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 岗位情报联网搜索配置。
 * 只有用户填写岗位、公司或 JD 且配置搜索密钥时，系统才会请求外部搜索服务。
 */
@ConfigurationProperties(prefix = "app.job-intelligence")
public class JobIntelligenceProperties {

    private boolean enabled = true;

    private String provider = "tavily";

    private String baseUrl = "https://api.tavily.com";

    private String apiKey = "";

    private int maxResults = 5;

    private Duration timeout = Duration.ofSeconds(8);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }
}
