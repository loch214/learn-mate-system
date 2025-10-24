package com.learnmate.service;

import com.learnmate.model.AnswerSheet;
import com.learnmate.model.Exam;
import com.learnmate.model.User;
import com.learnmate.repository.AnswerSheetRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AnswerSheetService {
    private final AnswerSheetRepository answerSheetRepository;

    public AnswerSheetService(AnswerSheetRepository answerSheetRepository) {
        this.answerSheetRepository = answerSheetRepository;
    }

    public AnswerSheet createAnswerSheet(AnswerSheet answerSheet) {
        answerSheet.setSubmittedAt(LocalDateTime.now());
        answerSheet.setStatus("SUBMITTED");
        return answerSheetRepository.save(answerSheet);
    }

    public AnswerSheet updateAnswerSheet(AnswerSheet answerSheet) {
        return answerSheetRepository.save(answerSheet);
    }

    public Optional<AnswerSheet> getAnswerSheetById(Long id) {
        return answerSheetRepository.findById(id);
    }

    public List<AnswerSheet> getAnswerSheetsByExam(Exam exam) {
        return answerSheetRepository.findByExam(exam);
    }

    public List<AnswerSheet> getAnswerSheetsByStudent(User student) {
        return answerSheetRepository.findByStudent(student);
    }

    public Optional<AnswerSheet> getAnswerSheetByExamAndStudent(Exam exam, User student) {
        return answerSheetRepository.findByExamAndStudent(exam, student);
    }

    public List<AnswerSheet> getAnswerSheetsByExamAndStatus(Exam exam, String status) {
        return answerSheetRepository.findByExamAndStatus(exam, status);
    }

    public void deleteAnswerSheet(Long id) {
        answerSheetRepository.deleteById(id);
    }
}


