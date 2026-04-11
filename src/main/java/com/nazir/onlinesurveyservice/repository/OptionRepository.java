package com.nazir.onlinesurveyservice.repository;

import com.nazir.onlinesurveyservice.domain.entity.Option;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OptionRepository extends JpaRepository<Option, UUID> {

    List<Option> findByQuestionIdOrderByOrderIndexAsc(UUID questionId);

    void deleteByQuestionId(UUID questionId);
}
