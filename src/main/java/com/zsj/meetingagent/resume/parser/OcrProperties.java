package com.zsj.meetingagent.resume.parser;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * OCR 配置。
 * 扫描版 PDF 需要外部 OCR 工具才能提取文字，当前实现优先支持本机 Tesseract CLI。
 */
@ConfigurationProperties(prefix = "app.resume.ocr")
public class OcrProperties {

    private boolean enabled = false;

    private String command = "tesseract";

    private String language = "chi_sim+eng";

    private int dpi = 220;

    private int maxPages = 5;

    private Duration timeout = Duration.ofSeconds(20);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getDpi() {
        return dpi;
    }

    public void setDpi(int dpi) {
        this.dpi = dpi;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }
}
