package com.learnmate.repository;

import com.learnmate.model.AnswerSheet;
import com.learnmate.model.Exam;
import com.learnmate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerSheetRepository extends JpaRepository<AnswerSheet, Long> {
    List<AnswerSheet> findByExam(Exam exam);
    List<AnswerSheet> findByStudent(User student);
    Optional<AnswerSheet> findByExamAndStudent(Exam exam, User student);
    List<AnswerSheet> findByExamAndStatus(Exam exam, String status);
}
