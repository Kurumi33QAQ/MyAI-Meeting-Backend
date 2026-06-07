package com.zsj.meetingagent.resume.service;

import com.zsj.meetingagent.resume.dto.ResumeTextRequest;
import com.zsj.meetingagent.resume.vo.ResumePreviewResponse;
import com.zsj.meetingagent.resume.vo.ResumeResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 简历服务接口。
 * 负责简历文本录入、文件上传和当前用户简历查询。
 */
public interface ResumeService {

    ResumeResponse uploadText(String username, ResumeTextRequest request);

    ResumeResponse uploadFile(String username, MultipartFile file);

    ResumeResponse getResume(String username, String resumeId);

    ResumePreviewResponse getResumePreview(String username, String resumeId);
}
