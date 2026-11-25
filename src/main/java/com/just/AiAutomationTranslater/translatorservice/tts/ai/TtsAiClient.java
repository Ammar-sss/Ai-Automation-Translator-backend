package com.just.AiAutomationTranslater.translatorservice.tts.ai;

public interface TtsAiClient {
    byte[] synthesize(String text, String voiceId);
}