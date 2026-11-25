package com.just.AiAutomationTranslater.translatorservice.tts.dto;

import com.just.AiAutomationTranslater.translatorservice.enums.LanguageTarget;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TtsRequest {
    @NotBlank(message = "Text is required")
    @Size(max = 1000, message = "Text exceeds maximum character limit")
    private String text;

    @NotNull(message = "Language is required")
    private LanguageTarget language;

    // Optional: Flutter can override the default voice if needed
    private String voiceId;
}