package com.zsj.meetingagent.interview.rule.node;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import com.zsj.meetingagent.interview.rule.FollowUpRuleContext;
import org.springframework.util.StringUtils;

/**
 * 缺失考点判断节点。
 * 回答缺少职责、技术方案、问题定位或量化结果时，触发针对性追问。
 */
@LiteflowComponent("missingPointCheckNode")
public class MissingPointCheckNode extends NodeComponent {

    @Override
    public void process() {
        FollowUpRuleContext context = getContextBean(FollowUpRuleContext.class);
        String answer = context.answer() == null ? "" : context.answer();
        if (!StringUtils.hasText(answer) || answer.length() < 30) {
            context.propose("缺失考点判断节点", "请不要只回答结论，补充一个完整例子：背景是什么、你做了什么、用了什么技术、结果如何？", "回答长度过短，缺少可评估信息。", false);
            return;
        }
        if (!containsAny(answer, "负责", "实现", "设计", "优化", "排查", "解决")) {
            context.propose("缺失考点判断节点", "请补充你在这个项目中的具体职责，以及你亲自完成的开发、设计或排查工作。", "回答没有体现个人职责和真实贡献。", false);
            return;
        }
        if (!containsAny(answer, "指标", "耗时", "QPS", "错误率", "提升", "降低", "数据")) {
            context.propose("缺失考点判断节点", "请补充一个量化结果，例如响应时间、错误率、查询耗时、QPS 或上线后的效果变化。", "回答缺少量化结果，结果意识不足。", false);
            return;
        }
        context.addTrace("缺失考点判断节点", false, "回答已覆盖职责、过程或量化信息。");
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
