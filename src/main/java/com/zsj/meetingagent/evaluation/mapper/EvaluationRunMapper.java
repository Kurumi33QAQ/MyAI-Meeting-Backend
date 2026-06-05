package com.zsj.meetingagent.evaluation.mapper;

import com.zsj.meetingagent.evaluation.entity.EvaluationRun;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

/**
 * 评测任务 Mapper。
 * 负责保存一次 evaluation run 的汇总指标和报告路径。
 */
@Mapper
public interface EvaluationRunMapper {

    @Insert("""
            INSERT INTO evaluation_run (
                run_id, username, dataset_name, total_cases,
                baseline_summary_json, rag_without_rerank_summary_json,
                rag_with_rerank_summary_json, self_check_rag_summary_json,
                report_json_path, report_markdown_path,
                created_at, completed_at, deleted
            ) VALUES (
                #{run.runId}, #{run.username}, #{run.datasetName}, #{run.totalCases},
                #{run.baselineSummaryJson}, #{run.ragWithoutRerankSummaryJson},
                #{run.ragWithRerankSummaryJson}, #{run.selfCheckRagSummaryJson},
                #{run.reportJsonPath}, #{run.reportMarkdownPath},
                #{run.createdAt}, #{run.completedAt}, #{run.deleted}
            )
            """)
    int insert(@Param("run") EvaluationRun run);

    @Select("""
            SELECT
                id,
                run_id AS runId,
                username,
                dataset_name AS datasetName,
                total_cases AS totalCases,
                baseline_summary_json AS baselineSummaryJson,
                rag_without_rerank_summary_json AS ragWithoutRerankSummaryJson,
                rag_with_rerank_summary_json AS ragWithRerankSummaryJson,
                self_check_rag_summary_json AS selfCheckRagSummaryJson,
                report_json_path AS reportJsonPath,
                report_markdown_path AS reportMarkdownPath,
                created_at AS createdAt,
                completed_at AS completedAt,
                deleted
            FROM evaluation_run
            WHERE run_id = #{runId}
              AND username = #{username}
              AND deleted = 0
            LIMIT 1
            """)
    Optional<EvaluationRun> findByRunIdAndUsername(
            @Param("runId") String runId,
            @Param("username") String username
    );
}
