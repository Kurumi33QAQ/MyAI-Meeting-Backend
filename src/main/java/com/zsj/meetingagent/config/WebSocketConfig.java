package com.zsj.meetingagent.config;

import com.zsj.meetingagent.websocket.speech.SpeechTranscriptionWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置。
 * 统一注册实时语音转写连接，并同时保留旧前端路径和本项目新风格路径。
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SpeechTranscriptionWebSocketHandler speechTranscriptionWebSocketHandler;
    private final CorsConfig corsConfig;

    public WebSocketConfig(
            SpeechTranscriptionWebSocketHandler speechTranscriptionWebSocketHandler,
            CorsConfig corsConfig
    ) {
        this.speechTranscriptionWebSocketHandler = speechTranscriptionWebSocketHandler;
        this.corsConfig = corsConfig;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        /*
         * 新路径体现本项目接口风格；旧路径服务现有前端 audioToTextWs.ts。
         * WebSocket 握手不走 MVC 拦截器，因此 token 校验在 Handler 内部完成。
         */
        registry.addHandler(
                        speechTranscriptionWebSocketHandler,
                        "/api/ws/speech/transcription/{userId}",
                        "/api/xunzhi/v1/xunfei/audio-to-text/{userId}"
                )
                .setAllowedOrigins(corsConfig.getAllowedOrigins().toArray(String[]::new));
    }
}
