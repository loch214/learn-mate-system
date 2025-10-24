package com.learnmate.repository;

import com.learnmate.model.Exam;
import com.learnmate.model.Mark;
import com.learnmate.model.SchoolClass;
import com.learnmate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarkRepository extends JpaRepository<Mark, Long> {
    List<Mark> findByStudent(User student);
    List<Mark> findByExam(Exam exam);
    List<Mark> findByExamTeacher(User teacher);
    Optional<Mark> findByExamAndStudent(Exam exam, User student);
    
    @Query("SELECT m FROM Mark m " +
           "JOIN FETCH m.student s " +
           "JOIN FETCH m.exam e " +
           "LEFT JOIN FETCH e.subject subj " +
           "LEFT JOIN FETCH e.schoolClass cls")
    List<Mark> findAllWithDetails();

    @Query("SELECT DISTINCT m FROM Mark m " +
        "JOIN FETCH m.student s " +
        "JOIN FETCH m.exam e " +
        "LEFT JOIN FETCH e.subject subj " +
        "LEFT JOIN FETCH e.schoolClass cls " +
        "WHERE s IN :students")
    List<Mark> findAllWithDetailsByStudents(@Param("students") Collection<User> students);
    
    // Custom queries for SchoolClass-based marks
    @Query("SELECT m FROM Mark m WHERE m.exam = :exam AND m.student.schoolClass = :schoolClass")
    List<Mark> findByExamAndStudentSchoolClass(@Param("exam") Exam exam, @Param("schoolClass") SchoolClass schoolClass);
    
    @Modifying
    @Query("DELETE FROM Mark m WHERE m.exam = :exam AND m.student.schoolClass = :schoolClass")
    void deleteByExamAndStudentSchoolClass(@Param("exam") Exam exam, @Param("schoolClass") SchoolClass schoolClass);
}
