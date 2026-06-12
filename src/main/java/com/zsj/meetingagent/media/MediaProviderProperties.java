package com.zsj.meetingagent.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 媒体供应商配置。
 * ASR/TTS 默认使用本地降级实现；配置为 openai 后调用真实 OpenAI Compatible 音频接口。
 */
@ConfigurationProperties(prefix = "app.media")
public class MediaProviderProperties {

    private Asr asr = new Asr();

    private Tts tts = new Tts();

    public Asr getAsr() {
        return asr;
    }

    public void setAsr(Asr asr) {
        this.asr = asr;
    }

    public Tts getTts() {
        return tts;
    }

    public void setTts(Tts tts) {
        this.tts = tts;
    }

    public static class Asr {
        private String provider = "local";
        private String model = "whisper-1";
        private String language = "zh";

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }
    }

    public static class Tts {
        private String provider = "local";
        private String model = "tts-1";
        private String voice = "alloy";
        private String responseFormat = "wav";

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getVoice() {
            return voice;
        }

        public void setVoice(String voice) {
            this.voice = voice;
        }

        public String getResponseFormat() {
            return responseFormat;
        }

        public void setResponseFormat(String responseFormat) {
            this.responseFormat = responseFormat;
        }
    }
}
