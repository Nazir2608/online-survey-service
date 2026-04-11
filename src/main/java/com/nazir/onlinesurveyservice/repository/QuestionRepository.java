package com.nazir.onlinesurveyservice.repository;

import com.nazir.onlinesurveyservice.domain.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

    List<Question> findBySurveyIdOrderByOrderIndexAsc(UUID surveyId);

    Optional<Question> findByIdAndSurveyId(UUID id, UUID surveyId);

    int countBySurveyId(UUID surveyId);

    @Modifying
    @Query("UPDATE Question q SET q.orderIndex = :orderIndex WHERE q.id = :id")
    void updateOrderIndex(@Param("id") UUID id, @Param("orderIndex") int orderIndex);
}
