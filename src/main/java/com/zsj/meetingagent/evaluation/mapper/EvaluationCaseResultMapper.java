package com.zsj.meetingagent.evaluation.mapper;

import com.zsj.meetingagent.evaluation.entity.EvaluationCaseResult;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 评测明细 Mapper。
 * 每条样本按评测方案写入多行，后续可以按 runId 对比各方案效果。
 */
@Mapper
public interface EvaluationCaseResultMapper {

    @Insert("""
            INSERT INTO evaluation_case_result (
                run_id, case_id, strategy, category, question, answer, ground_truth,
                cited_evidence_ids, answer_correct, hallucinated, citation_correct,
                latency_ms, created_at
            ) VALUES (
                #{result.runId}, #{result.caseId}, #{result.strategy}, #{result.category},
                #{result.question}, #{result.answer}, #{result.groundTruth},
                #{result.citedEvidenceIds}, #{result.answerCorrect}, #{result.hallucinated},
                #{result.citationCorrect}, #{result.latencyMs}, #{result.createdAt}
            )
            """)
    int insert(@Param("result") EvaluationCaseResult result);

    @Select("""
            SELECT
                id,
                run_id AS runId,
                case_id AS caseId,
                strategy,
                category,
                question,
                answer,
                ground_truth AS groundTruth,
                cited_evidence_ids AS citedEvidenceIds,
                answer_correct AS answerCorrect,
                hallucinated,
                citation_correct AS citationCorrect,
                latency_ms AS latencyMs,
                created_at AS createdAt
            FROM evaluation_case_result
            WHERE run_id = #{runId}
            ORDER BY case_id ASC, strategy ASC
            """)
    List<EvaluationCaseResult> findByRunId(@Param("runId") String runId);
}
