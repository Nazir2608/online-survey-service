package com.nazir.onlinesurveyservice.repository;

import com.nazir.onlinesurveyservice.domain.entity.Survey;
import com.nazir.onlinesurveyservice.domain.entity.User;
import com.nazir.onlinesurveyservice.domain.enums.SurveyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SurveyRepository extends JpaRepository<Survey, UUID> {

    Optional<Survey> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Survey> findByStatus(SurveyStatus status, Pageable pageable);

    Page<Survey> findByCreator(User creator, Pageable pageable);

    Page<Survey> findByCreatorAndStatus(User creator, SurveyStatus status, Pageable pageable);

    @Query("""
            SELECT s FROM Survey s
            WHERE s.status = 'PUBLISHED'
            AND (LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Survey> searchPublished(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT COUNT(r) FROM Response r WHERE r.survey.id = :surveyId")
    long countResponses(@Param("surveyId") UUID surveyId);
}
