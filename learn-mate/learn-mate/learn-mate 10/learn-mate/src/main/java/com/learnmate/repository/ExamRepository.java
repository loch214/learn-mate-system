package com.learnmate.repository;

import com.learnmate.model.Exam;
import com.learnmate.model.SchoolClass;
import com.learnmate.model.Subject;
import com.learnmate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findBySubject(Subject subject);
    List<Exam> findByTeacher(User teacher);
    List<Exam> findByGrade(String grade);
    List<Exam> findByGradeAndSubject(String grade, Subject subject);
    List<Exam> findBySchoolClass(SchoolClass schoolClass);
}
