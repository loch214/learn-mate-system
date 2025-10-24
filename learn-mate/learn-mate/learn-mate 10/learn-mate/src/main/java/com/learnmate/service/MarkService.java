package com.learnmate.service;

import com.learnmate.model.Exam;
import com.learnmate.model.Mark;
import com.learnmate.model.SchoolClass;
import com.learnmate.model.User;
import com.learnmate.repository.MarkRepository;

import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class MarkService {
    private final MarkRepository markRepository;
    // private final NotificationService notificationService;

    public MarkService(MarkRepository markRepository) {
        this.markRepository = markRepository;
    }

    public List<Mark> getAllMarks() {
        return markRepository.findAllWithDetails();
    }

    public Optional<Mark> getMarkById(Long id) {
        return markRepository.findById(id);
    }

    public Mark createMark(Mark mark) {
        return markRepository.save(mark);
    }

    public Mark updateMark(Mark mark) {
        Mark saved = markRepository.save(mark);
        if (saved.isPublished()) {
            // notificationService.createAlert("Results published for exam " + saved.getExam().getId(), "STUDENT");
        }
        checkPoorGrades(saved.getStudent());
        return saved;
    }

    public void deleteMark(Long id) {
        markRepository.deleteById(id);
    }

    public List<Mark> getMarksByStudent(User student) {
        return markRepository.findByStudent(student);
    }

    public List<Mark> getMarksByExam(Exam exam) {
        return markRepository.findByExam(exam);
    }

    public List<Mark> getMarksForStudents(Collection<User> students) {
        if (students == null || students.isEmpty()) {
            return List.of();
        }
        return markRepository.findAllWithDetailsByStudents(students);
    }

    public List<Mark> getMarksByTeacher(User teacher) {
        return markRepository.findByExamTeacher(teacher);
    }

    public List<Mark> getMarksByExamAndSchoolClass(Exam exam, SchoolClass schoolClass) {
        return markRepository.findByExamAndStudentSchoolClass(exam, schoolClass);
    }

    public Optional<Mark> getMarkByExamAndStudent(Exam exam, User student) {
        return markRepository.findByExamAndStudent(exam, student);
    }

    public void deleteMarksByExamAndSchoolClass(Exam exam, SchoolClass schoolClass) {
        markRepository.deleteByExamAndStudentSchoolClass(exam, schoolClass);
    }

    private void checkPoorGrades(User student) {
        List<Mark> marks = getMarksByStudent(student);
        double average = marks.stream().mapToInt(Mark::getScore).average().orElse(0);
        if (average < 50) {
            // notificationService.createAlert("Poor grades for " + student.getName(), "ADMIN");
        }
    }
}


