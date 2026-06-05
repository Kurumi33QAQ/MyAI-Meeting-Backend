package com.zsj.meetingagent.interview.rule;

import com.zsj.meetingagent.interview.enums.InterviewSessionStatus;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * LiteFlow 追问裁决上下文。
 * 规则节点通过这个对象共享分数、回答、AI 建议、追问次数和最终决策。
 */
public class FollowUpRuleContext {

    private final String sessionId;
    private final String questionId;
    private final String question;
    private final String answer;
    private final int score;
    private final String aiFeedback;
    private final String evaluationPoints;
    private final String followUpDirection;
    private final List<String> evidenceIds;
    private final InterviewSessionStatus sessionStatus;
    private final int existingFollowUpCount;
    private final int maxFollowUpCount;
    private final List<FollowUpRuleTrace> traces = new ArrayList<>();

    private boolean suppressed;
    private String suppressedReason;
    private String candidateQuestion;
    private String candidateReason;
    private FollowUpDecision decision;

    public FollowUpRuleContext(
            String sessionId,
            String questionId,
            String question,
            String answer,
            int score,
            String aiFeedback,
            String evaluationPoints,
            String followUpDirection,
            List<String> evidenceIds,
            InterviewSessionStatus sessionStatus,
            int existingFollowUpCount,
            int maxFollowUpCount
    ) {
        this.sessionId = sessionId;
        this.questionId = questionId;
        this.question = question;
        this.answer = answer;
        this.score = score;
        this.aiFeedback = aiFeedback;
        this.evaluationPoints = evaluationPoints;
        this.followUpDirection = followUpDirection;
        this.evidenceIds = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
        this.sessionStatus = sessionStatus;
        this.existingFollowUpCount = existingFollowUpCount;
        this.maxFollowUpCount = maxFollowUpCount;
    }

    public void addTrace(String nodeName, boolean hit, String reason) {
        traces.add(new FollowUpRuleTrace(nodeName, hit, reason));
    }

    public void suppress(String nodeName, String reason) {
        this.suppressed = true;
        this.suppressedReason = reason;
        addTrace(nodeName, true, reason);
    }

    public void propose(String nodeName, String question, String reason, boolean override) {
        if (suppressed) {
            addTrace(nodeName, false, "已有保护规则阻止追问：" + suppressedReason);
            return;
        }
        if (override || !StringUtils.hasText(candidateQuestion)) {
            this.candidateQuestion = question;
            this.candidateReason = reason;
            addTrace(nodeName, true, reason);
        } else {
            addTrace(nodeName, false, "已有更高优先级追问：" + candidateReason);
        }
    }

    public void completeDecision() {
        if (suppressed) {
            addTrace("最终裁决节点", false, "保护规则生效，最终不追问：" + suppressedReason);
            this.decision = new FollowUpDecision(false, null, suppressedReason, List.copyOf(traces));
            return;
        }
        if (StringUtils.hasText(candidateQuestion)) {
            addTrace("最终裁决节点", true, "决定追问：" + candidateReason);
            this.decision = new FollowUpDecision(true, candidateQuestion, candidateReason, List.copyOf(traces));
            return;
        }
        String reason = "回答质量达到当前阈值，暂不追问，进入下一题。";
        addTrace("最终裁决节点", false, reason);
        this.decision = new FollowUpDecision(false, null, reason, List.copyOf(traces));
    }

    public String sessionId() {
        return sessionId;
    }

    public String questionId() {
        return questionId;
    }

    public String question() {
        return question;
    }

    public String answer() {
        return answer;
    }

    public int score() {
        return score;
    }

    public String aiFeedback() {
        return aiFeedback;
    }

    public String evaluationPoints() {
        return evaluationPoints;
    }

    public String followUpDirection() {
        return followUpDirection;
    }

    public List<String> evidenceIds() {
        return evidenceIds;
    }

    public InterviewSessionStatus sessionStatus() {
        return sessionStatus;
    }

    public int existingFollowUpCount() {
        return existingFollowUpCount;
    }

    public int maxFollowUpCount() {
        return maxFollowUpCount;
    }

    public boolean suppressed() {
        return suppressed;
    }

    public String candidateQuestion() {
        return candidateQuestion;
    }

    public FollowUpDecision decision() {
        return decision;
    }
}
