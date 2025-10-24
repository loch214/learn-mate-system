package com.learnmate.service;

import com.learnmate.model.AnswerSheet;
import com.learnmate.model.Exam;
import com.learnmate.model.Mark;
import com.learnmate.model.SchoolClass;
import com.learnmate.model.Subject;
import com.learnmate.model.User;
import com.learnmate.repository.AnswerSheetRepository;
import com.learnmate.repository.ExamRepository;
import com.learnmate.repository.MarkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ExamService {
    private final ExamRepository examRepository;
    private final AnswerSheetRepository answerSheetRepository;
    private final MarkRepository markRepository;
    private final FileStorageService fileStorageService;
    private static final Logger LOGGER = LoggerFactory.getLogger(ExamService.class);

    public ExamService(ExamRepository examRepository,
                       AnswerSheetRepository answerSheetRepository,
                       MarkRepository markRepository,
                       FileStorageService fileStorageService) {
        this.examRepository = examRepository;
        this.answerSheetRepository = answerSheetRepository;
        this.markRepository = markRepository;
        this.fileStorageService = fileStorageService;
    }

    public List<Exam> getAllExams() {
        return examRepository.findAll();
    }

    public Optional<Exam> getExamById(Long id) {
        return examRepository.findById(id);
    }

    public Exam createExam(Exam exam) {
        return examRepository.save(exam);
    }

    public Exam updateExam(Exam exam) {
        return examRepository.save(exam);
    }

    @Transactional
    public void deleteExam(Long id) {
        Exam exam = examRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Exam not found with id " + id));

        // Remove linked answer sheets (including stored files)
        List<AnswerSheet> answerSheets = answerSheetRepository.findByExam(exam);
        if (!answerSheets.isEmpty()) {
            answerSheets.forEach(sheet -> {
                String filePath = sheet.getFilePath();
                if (filePath != null && !filePath.isBlank()) {
                    try {
                        fileStorageService.deleteFile(filePath, FileStorageService.FileType.ANSWER_SHEET);
                    } catch (RuntimeException ex) {
                        LOGGER.warn("Failed to delete answer sheet file {} for exam {}", filePath, exam.getId(), ex);
                    }
                }
            });
            answerSheetRepository.deleteAll(answerSheets);
        }

        // Remove marks tied to this exam
        List<Mark> marks = markRepository.findByExam(exam);
        if (!marks.isEmpty()) {
            markRepository.deleteAll(marks);
        }

        // Remove stored exam file if present
        String examFilePath = exam.getFilePath();
        if (examFilePath != null && !examFilePath.isBlank()) {
            try {
                fileStorageService.deleteFile(examFilePath, FileStorageService.FileType.EXAM);
            } catch (RuntimeException ex) {
                LOGGER.warn("Failed to delete exam file {} for exam {}", examFilePath, exam.getId(), ex);
            }
        }

        examRepository.delete(exam);
    }

    public List<Exam> getExamsBySubject(Subject subject) {
        return examRepository.findBySubject(subject);
    }

    public List<Exam> getExamsByTeacher(User teacher) {
        return examRepository.findByTeacher(teacher);
    }

    public List<Exam> getExamsByGradeAndSubject(String grade, Subject subject) {
        return examRepository.findByGradeAndSubject(grade, subject);
    }

    public List<Exam> getExamsByGrade(String grade) {
        return examRepository.findByGrade(grade);
    }

    public List<Exam> getExamsBySchoolClass(SchoolClass schoolClass) {
        return examRepository.findBySchoolClass(schoolClass);
    }
}


