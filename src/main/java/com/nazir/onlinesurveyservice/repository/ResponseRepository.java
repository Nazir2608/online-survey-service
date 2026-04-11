package com.nazir.onlinesurveyservice.repository;

import com.nazir.onlinesurveyservice.domain.entity.Response;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResponseRepository extends JpaRepository<Response, UUID> {

    Page<Response> findBySurveyId(UUID surveyId, Pageable pageable);

    boolean existsBySurveyIdAndRespondentId(UUID surveyId, UUID respondentId);

    long countBySurveyId(UUID surveyId);

    Optional<Response> findByIdAndSurveyId(UUID id, UUID surveyId);

    /** Daily response counts for trend chart */
    @Query("""
            SELECT CAST(r.submittedAt AS DATE) AS day, COUNT(r) AS total
            FROM Response r
            WHERE r.survey.id = :surveyId
              AND r.submittedAt >= :since
            GROUP BY CAST(r.submittedAt AS DATE)
            ORDER BY CAST(r.submittedAt AS DATE)
            """)
    List<Object[]> countByDay(@Param("surveyId") UUID surveyId, @Param("since") Instant since);
}
