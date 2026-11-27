package com.just.AiAutomationTranslater.translatorservice.tts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ElevenLabsPayload {
    private String text;

    @JsonProperty("model_id")
    private String modelId;

    @JsonProperty("voice_settings")
    private VoiceSettings voiceSettings;

    @Data
    @Builder
    public static class VoiceSettings {
        private Double stability;
        @JsonProperty("similarity_boost")
        private Double similarityBoost;
    }
}