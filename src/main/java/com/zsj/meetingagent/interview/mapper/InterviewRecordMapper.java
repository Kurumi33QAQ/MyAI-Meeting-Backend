package com.zsj.meetingagent.interview.mapper;

import com.zsj.meetingagent.interview.entity.InterviewRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
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

    @Select("""
            <script>
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
            WHERE username = #{username}
              AND deleted = 0
              <if test="sessionId != null and sessionId != ''">
                AND session_id = #{sessionId}
              </if>
              <if test="status != null and status != ''">
                AND status = #{status}
              </if>
              <if test="activeOnly != null and activeOnly">
                AND status != 'COMPLETED'
              </if>
              <if test="keyword != null and keyword != ''">
                AND (
                  job_title LIKE CONCAT('%', #{keyword}, '%')
                  OR session_id LIKE CONCAT('%', #{keyword}, '%')
                )
              </if>
              <if test="interviewDirection != null and interviewDirection != ''">
                AND job_title LIKE CONCAT('%', #{interviewDirection}, '%')
              </if>
              <if test="minScore != null">
                AND total_score &gt;= #{minScore}
              </if>
              <if test="maxScore != null">
                AND total_score &lt;= #{maxScore}
              </if>
            ORDER BY updated_at DESC, id DESC
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<InterviewRecord> findPageByUsername(
            @Param("username") String username,
            @Param("sessionId") String sessionId,
            @Param("status") String status,
            @Param("activeOnly") Boolean activeOnly,
            @Param("keyword") String keyword,
            @Param("interviewDirection") String interviewDirection,
            @Param("minScore") Integer minScore,
            @Param("maxScore") Integer maxScore,
            @Param("offset") int offset,
            @Param("size") int size
    );

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM interview_record
            WHERE username = #{username}
              AND deleted = 0
              <if test="sessionId != null and sessionId != ''">
                AND session_id = #{sessionId}
              </if>
              <if test="status != null and status != ''">
                AND status = #{status}
              </if>
              <if test="activeOnly != null and activeOnly">
                AND status != 'COMPLETED'
              </if>
              <if test="keyword != null and keyword != ''">
                AND (
                  job_title LIKE CONCAT('%', #{keyword}, '%')
                  OR session_id LIKE CONCAT('%', #{keyword}, '%')
                )
              </if>
              <if test="interviewDirection != null and interviewDirection != ''">
                AND job_title LIKE CONCAT('%', #{interviewDirection}, '%')
              </if>
              <if test="minScore != null">
                AND total_score &gt;= #{minScore}
              </if>
              <if test="maxScore != null">
                AND total_score &lt;= #{maxScore}
              </if>
            </script>
            """)
    long countByUsername(
            @Param("username") String username,
            @Param("sessionId") String sessionId,
            @Param("status") String status,
            @Param("activeOnly") Boolean activeOnly,
            @Param("keyword") String keyword,
            @Param("interviewDirection") String interviewDirection,
            @Param("minScore") Integer minScore,
            @Param("maxScore") Integer maxScore
    );

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
