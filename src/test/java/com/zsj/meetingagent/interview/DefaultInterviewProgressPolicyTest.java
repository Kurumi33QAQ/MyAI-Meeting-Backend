package com.zsj.meetingagent.interview;

import com.zsj.meetingagent.interview.adaptive.DefaultInterviewProgressPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 自适应面试题量策略测试。
 * 验证 8 道基础、12 道标准、15 道上限以及待追问优先规则。
 */
class DefaultInterviewProgressPolicyTest {

    private final DefaultInterviewProgressPolicy policy = new DefaultInterviewProgressPolicy();

    @Test
    void startsWithEightQuestionsAndUsesFifteenQuestionPool() {
        assertThat(policy.initialQuestionCount()).isEqualTo(8);
        assertThat(policy.questionPoolSize()).isEqualTo(15);
    }

    @Test
    void mediumPerformanceExpandsFromEightToTwelve() {
        var decision = policy.decide(true, 8, 72, 8, 15, false);

        assertThat(decision.complete()).isFalse();
        assertThat(decision.targetQuestionCount()).isEqualTo(12);
    }

    @Test
    void weakPerformanceExpandsFromEightToFifteen() {
        var decision = policy.decide(true, 8, 40, 8, 15, false);

        assertThat(decision.complete()).isFalse();
        assertThat(decision.targetQuestionCount()).isEqualTo(15);
    }

    @Test
    void pendingFollowUpMustBeAnsweredBeforeCompletion() {
        var decision = policy.decide(true, 12, 90, 12, 15, true);

        assertThat(decision.complete()).isFalse();
        assertThat(decision.targetQuestionCount()).isEqualTo(12);
        assertThat(decision.reason()).contains("先完成追问");
    }
}
