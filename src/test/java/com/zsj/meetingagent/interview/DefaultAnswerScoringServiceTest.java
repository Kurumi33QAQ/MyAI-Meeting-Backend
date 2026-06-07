package com.zsj.meetingagent.interview;

import com.zsj.meetingagent.interview.scoring.DefaultAnswerScoringService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 面试回答评分规则测试。
 * 验证拒答不会再获得默认及格分，同时保留对完整技术回答的正向区分。
 */
class DefaultAnswerScoringServiceTest {

    private final DefaultAnswerScoringService scoringService = new DefaultAnswerScoringService();

    @Test
    void refusalAnswerGetsZeroScore() {
        var result = scoringService.score("请介绍项目", "项目职责、技术方案、结果", "不知道");

        assertThat(result.score()).isZero();
        assertThat(result.feedback()).contains("没有提供可评分的有效信息");
    }

    @Test
    void detailedAnswerGetsClearlyHigherScore() {
        var result = scoringService.score(
                "你如何优化接口性能？",
                "问题定位、技术取舍、验证过程、量化结果",
                "我负责订单查询接口，因为慢 SQL 导致响应较慢，所以先通过日志和压测定位，再增加 MySQL 联合索引并使用 Redis 缓存，最终将耗时从 320ms 降低到 85ms。"
        );

        assertThat(result.score()).isGreaterThanOrEqualTo(85);
    }
}
