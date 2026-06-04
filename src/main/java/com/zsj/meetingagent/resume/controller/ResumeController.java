package com.zsj.meetingagent.resume.controller;

import com.zsj.meetingagent.auth.security.LoginUserContext;
import com.zsj.meetingagent.common.result.ApiResponse;
import com.zsj.meetingagent.resume.dto.ResumeTextRequest;
import com.zsj.meetingagent.resume.service.ResumeService;
import com.zsj.meetingagent.resume.vo.ResumeResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 简历接口控制器。
 * 提供本项目新风格的简历文本录入、文件上传和简历查询接口。
 */
@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping("/text")
    public ApiResponse<ResumeResponse> uploadText(@Valid @RequestBody ResumeTextRequest request) {
        return ApiResponse.success(resumeService.uploadText(LoginUserContext.currentUsername(), request));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ResumeResponse> uploadFile(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(resumeService.uploadFile(LoginUserContext.currentUsername(), file));
    }

    @GetMapping("/{resumeId}")
    public ApiResponse<ResumeResponse> getResume(@PathVariable String resumeId) {
        return ApiResponse.success(resumeService.getResume(LoginUserContext.currentUsername(), resumeId));
    }
}
