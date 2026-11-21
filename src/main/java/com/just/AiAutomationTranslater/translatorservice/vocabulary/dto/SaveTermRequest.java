package com.just.AiAutomationTranslater.translatorservice.vocabulary.dto;

import com.just.AiAutomationTranslater.translatorservice.enums.LanguageTarget;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class SaveTermRequest {
    @NotBlank
    private String termWord;
    @NotNull
    private LanguageTarget sourceLanguage;
    @NotNull
    private LanguageTarget targetLanguage;
    private String exampleSentence;
    private Set<String> termDefinitions;
    private String exampleDefinition;
}