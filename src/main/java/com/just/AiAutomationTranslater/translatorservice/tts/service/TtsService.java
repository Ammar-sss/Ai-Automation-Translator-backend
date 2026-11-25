package com.just.AiAutomationTranslater.translatorservice.tts.service;

import com.just.AiAutomationTranslater.translatorservice.tts.dto.TtsRequest;

public interface TtsService {
    byte[] generateSpeech(TtsRequest request);
}