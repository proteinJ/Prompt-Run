package com.promptrungame.prompt_run.repository;

import com.promptrungame.prompt_run.domain.QuizEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<QuizEntity, Long> {
    @Query(value = "SELECT * FROM quiz_question ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<QuizEntity> findRandomQuiz();
}