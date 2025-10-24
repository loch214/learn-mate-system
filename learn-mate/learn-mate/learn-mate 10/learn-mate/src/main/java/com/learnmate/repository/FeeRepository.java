package com.learnmate.repository;

import com.learnmate.model.Fee;
import com.learnmate.model.Subject;
import com.learnmate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeeRepository extends JpaRepository<Fee, Long> {
    List<Fee> findByStudent(User student);
    List<Fee> findByStudentAndSubject(User student, Subject subject);
    Optional<Fee> findByStudentAndSubjectAndStatus(User student, Subject subject, String status);

    // For cases where duplicates might exist, fetch a list and let service choose
    List<Fee> findByStudentAndSubjectAndStatusOrderByDueDateAsc(User student, Subject subject, String status);

    List<Fee> findByStudentAndSchoolClass(User student, com.learnmate.model.SchoolClass schoolClass);
    List<Fee> findByStudentAndSubjectAndSchoolClass(User student, Subject subject, com.learnmate.model.SchoolClass schoolClass);
    Optional<Fee> findByStudentAndSubjectAndSchoolClassAndStatus(User student, Subject subject, com.learnmate.model.SchoolClass schoolClass, String status);
}
