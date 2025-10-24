package com.learnmate.repository;

import com.learnmate.model.Material;
import com.learnmate.model.SchoolClass;
import com.learnmate.model.Subject;
import com.learnmate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findBySubjectAndSchoolClassAndActiveTrue(Subject subject, SchoolClass schoolClass);
    List<Material> findByTeacherAndActiveTrue(User teacher);
    List<Material> findBySubjectAndActiveTrue(Subject subject);
    List<Material> findBySchoolClassAndActiveTrue(SchoolClass schoolClass);
    List<Material> findBySchoolClassAndSubjectInAndActiveTrue(SchoolClass schoolClass, Set<Subject> subjects);
    List<Material> findByActiveTrueOrderByUploadedAtDesc();
}
