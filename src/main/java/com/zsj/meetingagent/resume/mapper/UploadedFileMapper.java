package com.zsj.meetingagent.resume.mapper;

import com.zsj.meetingagent.resume.entity.UploadedFile;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

/**
 * 上传文件 MyBatis Mapper。
 * 负责把简历文本和文件元信息保存到 MySQL，方便后续面试记录通过 resumeId 关联。
 */
@Mapper
public interface UploadedFileMapper {

    @Insert("""
            INSERT INTO uploaded_file (
                file_id, username, file_name, content_type, file_size, document_type,
                text_content, summary, created_at, updated_at, deleted
            ) VALUES (
                #{file.fileId}, #{file.username}, #{file.fileName}, #{file.contentType}, #{file.fileSize},
                #{file.documentType}, #{file.textContent}, #{file.summary}, #{file.createdAt}, #{file.updatedAt}, #{file.deleted}
            )
            """)
    int insert(@Param("file") UploadedFile file);

    @Select("""
            SELECT
                id,
                file_id AS fileId,
                username,
                file_name AS fileName,
                content_type AS contentType,
                file_size AS fileSize,
                document_type AS documentType,
                text_content AS textContent,
                summary,
                created_at AS createdAt,
                updated_at AS updatedAt,
                deleted
            FROM uploaded_file
            WHERE file_id = #{fileId}
              AND username = #{username}
              AND deleted = 0
            LIMIT 1
            """)
    Optional<UploadedFile> findByFileIdAndUsername(@Param("fileId") String fileId, @Param("username") String username);
}
