package com.zsj.meetingagent.media.tts;

import com.zsj.meetingagent.auth.security.LoginUserContext;
import com.zsj.meetingagent.common.exception.BusinessException;
import com.zsj.meetingagent.common.result.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * TTS 语音合成接口。
 * 新路径服务本项目接口风格，旧路径兼容现有前端的讯飞 TTS service 封装。
 */
@RestController
public class TtsController {

    private final TtsService ttsService;

    public TtsController(TtsService ttsService) {
        this.ttsService = ttsService;
    }

    @PostMapping({
            "/api/media/tts/tasks",
            "/api/xunzhi/v1/xunfei/tts/tasks"
    })
    public ApiResponse<TtsTaskResponse> createTask(@Valid @RequestBody TtsRequest request) {
        return ApiResponse.success(ttsService.createTask(LoginUserContext.currentUsername(), request));
    }

    @PostMapping({
            "/api/media/tts/synthesize",
            "/api/xunzhi/v1/xunfei/tts/synthesize"
    })
    public ApiResponse<TtsTaskResponse> synthesize(@Valid @RequestBody TtsRequest request) {
        return createTask(request);
    }

    @GetMapping({
            "/api/media/tts/tasks/{taskId}",
            "/api/xunzhi/v1/xunfei/tts/tasks/{taskId}"
    })
    public ApiResponse<TtsTaskResponse> getTask(@PathVariable String taskId) {
        return ApiResponse.success(ttsService.getTask(LoginUserContext.currentUsername(), taskId)
                .orElseThrow(() -> new BusinessException("TTS_TASK_NOT_FOUND", "TTS 任务不存在或无权访问")));
    }

    @GetMapping("/api/media/tts/tasks/{taskId}/audio")
    public ResponseEntity<byte[]> getTaskAudio(@PathVariable String taskId) {
        byte[] audioBytes = ttsService.getPublicAudioBytes(taskId)
                .orElseThrow(() -> new BusinessException("TTS_TASK_NOT_FOUND", "TTS 任务不存在或无权访问"));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"tts-" + taskId + ".wav\"")
                .contentType(MediaType.parseMediaType("audio/wav"))
                .body(audioBytes);
    }
}
