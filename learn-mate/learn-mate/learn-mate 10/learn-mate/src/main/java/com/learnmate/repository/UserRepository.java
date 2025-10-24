package com.learnmate.repository;

import com.learnmate.model.Role;
import com.learnmate.model.SchoolClass;
import com.learnmate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findByRole(Role role);
    List<User> findByNameContainingIgnoreCase(String name);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findBySchoolClass(SchoolClass schoolClass);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.schoolClass LEFT JOIN FETCH u.subjects WHERE u.role = :role")
    List<User> findByRoleWithRelationships(Role role);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.schoolClass LEFT JOIN FETCH u.subjects WHERE u.role = :role AND u.schoolClass = :schoolClass AND :subject MEMBER OF u.subjects")
    List<User> findByRoleAndSchoolClassAndSubject(Role role, SchoolClass schoolClass, com.learnmate.model.Subject subject);
    
    @Query("SELECT u FROM User u WHERE u.schoolClass.id = :classId AND u.role = :role")
    List<User> findBySchoolClassIdAndRole(Long classId, Role role);
    
    @Query("SELECT DISTINCT u FROM User u JOIN u.subjects s WHERE u.schoolClass.id = :classId AND u.role = :role AND s.id IN :subjectIds")
    List<User> findBySchoolClassIdAndRoleAndSubjectIds(Long classId, Role role, List<Long> subjectIds);

    List<User> findAllByIdInAndRole(List<Long> ids, Role role);
    
    @Query("SELECT DISTINCT s FROM User s WHERE s.role = :role AND s.id NOT IN (SELECT DISTINCT c.id FROM User p JOIN p.children c WHERE p.role = 'PARENT')")
    List<User> findStudentsNotLinkedToAnyParent(Role role);

    @Query("SELECT p FROM User p LEFT JOIN FETCH p.children WHERE p.username = :username")
    Optional<User> findParentWithChildrenByUsername(@Param("username") String username);
}
