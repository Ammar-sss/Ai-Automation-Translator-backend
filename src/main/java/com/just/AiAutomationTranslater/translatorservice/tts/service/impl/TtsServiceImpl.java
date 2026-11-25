package com.just.AiAutomationTranslater.translatorservice.tts.service.impl;

import com.just.AiAutomationTranslater.translatorservice.tts.ai.TtsAiClient;
import com.just.AiAutomationTranslater.translatorservice.tts.dto.TtsRequest;
import com.just.AiAutomationTranslater.translatorservice.tts.service.TtsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TtsServiceImpl implements TtsService {

    private final TtsAiClient ttsAiClient;

    @Value("${elevenlabs.voice-id:JBFqnCBsd6RMkjVDRZzb}")
    private String defaultVoiceId;

    @Override
    @Cacheable(
            cacheNames = "tts",
            key = "#request.language.name() + ':' + (#request.voiceId != null ? #request.voiceId : @ttsServiceImpl.defaultVoiceId) + ':' + #request.text.trim().toLowerCase()"
    )
    public byte[] generateSpeech(TtsRequest request) {
        String voiceId = (request.getVoiceId() != null && !request.getVoiceId().isBlank())
                ? request.getVoiceId()
                : defaultVoiceId;

        log.info("Generating TTS for language [{}] using voice [{}]", request.getLanguage(), voiceId);
        return ttsAiClient.synthesize(request.getText().trim(), voiceId);
    }
}
