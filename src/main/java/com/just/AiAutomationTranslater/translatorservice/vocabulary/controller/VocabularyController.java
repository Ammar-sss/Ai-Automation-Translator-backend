package com.just.AiAutomationTranslater.translatorservice.vocabulary.controller;

import com.just.AiAutomationTranslater.translatorservice.res.Response;
import com.just.AiAutomationTranslater.translatorservice.vocabulary.dto.SaveTermRequest;
import com.just.AiAutomationTranslater.translatorservice.vocabulary.dto.TermDTO;
import com.just.AiAutomationTranslater.translatorservice.vocabulary.service.VocabularyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/vocabulary")
public class VocabularyController {

    private final VocabularyService vocabularyService;

    @PostMapping
    public ResponseEntity<Response<TermDTO>> confirmTermSave(@Valid @RequestBody SaveTermRequest request) {
        return ResponseEntity.ok(vocabularyService.confirmTermSave(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response<TermDTO>> getTermById(@Valid @PathVariable Long id) {
        return ResponseEntity.ok(vocabularyService.getTermById(id));
    }

    @GetMapping
    public ResponseEntity<Response<Page<TermDTO>>> getAllTerms(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(vocabularyService.getAllTerms(pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response<Void>> deleteTermById(@Valid @PathVariable Long id) {
        return ResponseEntity.ok(vocabularyService.deleteTermById(id));
    }
}

