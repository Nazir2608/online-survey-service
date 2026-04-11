package com.nazir.onlinesurveyservice.repository;

import com.nazir.onlinesurveyservice.domain.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AnswerRepository extends JpaRepository<Answer, UUID> {

    List<Answer> findByResponseId(UUID responseId);

    /** For MCQ analytics: count how many times each option was chosen */
    @Query("""
            SELECT o.id, o.text, COUNT(a) AS choiceCount
            FROM Answer a
            JOIN a.selectedOptions o
            WHERE a.question.id = :questionId
            GROUP BY o.id, o.text
            ORDER BY choiceCount DESC
            """)
    List<Object[]> countOptionSelections(@Param("questionId") UUID questionId);

    /** For RATING/NUMBER analytics: avg, min, max */
    @Query("""
            SELECT AVG(a.ratingValue), MIN(a.ratingValue), MAX(a.ratingValue)
            FROM Answer a
            WHERE a.question.id = :questionId
              AND a.ratingValue IS NOT NULL
            """)
    Object[] ratingStats(@Param("questionId") UUID questionId);
}
