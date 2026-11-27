package com.just.AiAutomationTranslater.translatorservice.tts.controller;

import com.just.AiAutomationTranslater.translatorservice.enums.LanguageTarget;
import com.just.AiAutomationTranslater.translatorservice.tts.dto.TtsRequest;
import com.just.AiAutomationTranslater.translatorservice.tts.service.TtsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tts")
public class TtsController {

    private final TtsService ttsService;

    // POST endpoint (standard JSON body)
    @PostMapping(value = "/synthesize", produces = "audio/mpeg")
    public ResponseEntity<byte[]> synthesize(@Valid @RequestBody TtsRequest request) {
        byte[] audioData = ttsService.generateSpeech(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"speech.mp3\"")
                .contentType(MediaType.valueOf("audio/mpeg"))
                .body(audioData);
    }

    // GET endpoint (easy direct stream for Flutter Audio Players)
    @GetMapping(value = "/stream", produces = "audio/mpeg")
    public ResponseEntity<byte[]> stream(
            @RequestParam @NotBlank @Size(max = 1000) String text,
            @RequestParam @NotNull LanguageTarget language,
            @RequestParam(required = false) @Size(max = 64) String voiceId) {

        TtsRequest request = new TtsRequest();
        request.setText(text);
        request.setLanguage(language);
        request.setVoiceId(voiceId);

        byte[] audioData = ttsService.generateSpeech(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"speech.mp3\"")
                .contentType(MediaType.valueOf("audio/mpeg"))
                .body(audioData);
    }
}