package com.just.AiAutomationTranslater.translatorservice.vocabulary.repository;

import com.just.AiAutomationTranslater.translatorservice.enums.LanguageTarget;
import com.just.AiAutomationTranslater.translatorservice.vocabulary.entity.Term;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TermRepository extends JpaRepository<Term, Long> {

    Page<Term> findAllByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"termDefinitions"})
    Optional<Term> findByIdAndUserId(Long id, Long userId);

    Optional<Term> findByTermWordAndUserId(String term, Long userId);

    boolean existsByTermWordAndUserId(String wordTerm, Long userId);

    boolean existsByTermWordAndUserIdAndSourceLanguageAndTargetLanguage(
            String termWord, Long userId, LanguageTarget source, LanguageTarget target);

    Optional<Term> findByTermWordAndUserIdAndSourceLanguageAndTargetLanguage(
            String termWord, Long userId, LanguageTarget source, LanguageTarget target);
}
