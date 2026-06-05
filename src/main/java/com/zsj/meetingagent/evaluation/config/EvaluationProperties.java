package com.zsj.meetingagent.evaluation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Evaluation 模块配置。
 * 默认测试集和报告目录都集中放在这里，避免散落在 Service 代码中硬编码。
 */
@ConfigurationProperties(prefix = "app.evaluation")
public class EvaluationProperties {

    private String defaultDatasetPath = "classpath:evaluation/eval_cases.json";

    private String reportDir = "reports/evaluation";

    private double answerHitThreshold = 0.35;

    private double citationHitThreshold = 0.25;

    private double selfCheckMinConfidence = 0.2;

    public String getDefaultDatasetPath() {
        return defaultDatasetPath;
    }

    public void setDefaultDatasetPath(String defaultDatasetPath) {
        this.defaultDatasetPath = defaultDatasetPath;
    }

    public String getReportDir() {
        return reportDir;
    }

    public void setReportDir(String reportDir) {
        this.reportDir = reportDir;
    }

    public double getAnswerHitThreshold() {
        return answerHitThreshold;
    }

    public void setAnswerHitThreshold(double answerHitThreshold) {
        this.answerHitThreshold = answerHitThreshold;
    }

    public double getCitationHitThreshold() {
        return citationHitThreshold;
    }

    public void setCitationHitThreshold(double citationHitThreshold) {
        this.citationHitThreshold = citationHitThreshold;
    }

    public double getSelfCheckMinConfidence() {
        return selfCheckMinConfidence;
    }

    public void setSelfCheckMinConfidence(double selfCheckMinConfidence) {
        this.selfCheckMinConfidence = selfCheckMinConfidence;
    }
}
