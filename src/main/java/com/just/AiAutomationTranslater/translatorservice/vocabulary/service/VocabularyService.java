package com.just.AiAutomationTranslater.translatorservice.vocabulary.service;

import com.just.AiAutomationTranslater.translatorservice.res.Response;
import com.just.AiAutomationTranslater.translatorservice.vocabulary.dto.SaveTermRequest;
import com.just.AiAutomationTranslater.translatorservice.vocabulary.dto.TermDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface VocabularyService {

    Response<TermDTO> getTermById(Long id);
    Response<Page<TermDTO>> getAllTerms(Pageable pageable);
    Response<TermDTO> updateTerm(TermDTO termDTO);
    Response<Void> deleteTermById(Long id);
    Response<TermDTO> findByTermWord(String termWord);
    Response<TermDTO> confirmTermSave(SaveTermRequest request);
    Optional<TermDTO> findByNormalizedTerm(String normalizationTerm);
}