package com.zsj.meetingagent.interview.mapper;

import com.zsj.meetingagent.interview.entity.InterviewRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Optional;

/**
 * 面试记录 MyBatis Mapper。
 * 负责保存面试会话的结构化状态和报告摘要，供列表、报告和后续简历项目展示使用。
 */
@Mapper
public interface InterviewRecordMapper {

    @Insert("""
            INSERT INTO interview_record (
                session_id, username, resume_id, job_title, status, question_count, answered_count,
                total_score, report_summary, created_at, updated_at, deleted
            ) VALUES (
                #{record.sessionId}, #{record.username}, #{record.resumeId}, #{record.jobTitle}, #{record.status},
                #{record.questionCount}, #{record.answeredCount}, #{record.totalScore}, #{record.reportSummary},
                #{record.createdAt}, #{record.updatedAt}, #{record.deleted}
            )
            """)
    int insert(@Param("record") InterviewRecord record);

    @Select("""
            SELECT
                id,
                session_id AS sessionId,
                username,
                resume_id AS resumeId,
                job_title AS jobTitle,
                status,
                question_count AS questionCount,
                answered_count AS answeredCount,
                total_score AS totalScore,
                report_summary AS reportSummary,
                created_at AS createdAt,
                updated_at AS updatedAt,
                deleted
            FROM interview_record
            WHERE session_id = #{sessionId}
              AND username = #{username}
              AND deleted = 0
            LIMIT 1
            """)
    Optional<InterviewRecord> findBySessionIdAndUsername(@Param("sessionId") String sessionId, @Param("username") String username);

    @Update("""
            UPDATE interview_record
            SET status = #{record.status},
                question_count = #{record.questionCount},
                answered_count = #{record.answeredCount},
                total_score = #{record.totalScore},
                report_summary = #{record.reportSummary},
                updated_at = #{record.updatedAt}
            WHERE session_id = #{record.sessionId}
              AND username = #{record.username}
              AND deleted = 0
            """)
    int updateProgress(@Param("record") InterviewRecord record);
}
