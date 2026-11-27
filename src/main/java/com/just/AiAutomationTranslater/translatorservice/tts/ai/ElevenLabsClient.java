package com.just.AiAutomationTranslater.translatorservice.tts.ai;

import com.just.AiAutomationTranslater.translatorservice.exceptions.TranslationAiException;
import com.just.AiAutomationTranslater.translatorservice.tts.dto.ElevenLabsPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
@Slf4j
public class ElevenLabsClient implements TtsAiClient {

    private final RestClient restClient;
    private final String modelId;

    public ElevenLabsClient(
            @Value("${elevenlabs.api-key}") String apiKey,
            @Value("${elevenlabs.base-url:https://api.elevenlabs.io/v1}") String baseUrl,
            @Value("${elevenlabs.model-id:eleven_v4}") String modelId) {

        this.modelId = modelId;

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("xi-api-key", apiKey)
                .build();
    }

    @Override
    public byte[] synthesize(String text, String voiceId) {
        ElevenLabsPayload payload = ElevenLabsPayload.builder()
                .text(text)
                .modelId(modelId)
                .voiceSettings(ElevenLabsPayload.VoiceSettings.builder()
                        .stability(0.5)
                        .similarityBoost(0.75)
                        .build())
                .build();

        try {
            return restClient.post()
                    .uri("/text-to-speech/{voiceId}?output_format=mp3_44100_128", voiceId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.valueOf("audio/mpeg"))
                    .body(payload)
                    .retrieve()
                    .body(byte[].class);
        } catch (Exception e) {
            log.error("ElevenLabs TTS request failed for voice: {}", voiceId, e);
            throw new TranslationAiException("Speech synthesis temporarily unavailable");
        }
    }
}
