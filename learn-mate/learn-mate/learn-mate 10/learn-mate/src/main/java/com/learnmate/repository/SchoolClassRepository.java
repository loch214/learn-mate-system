package com.learnmate.repository;

import com.learnmate.model.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {
    List<SchoolClass> findByNameContainingIgnoreCase(String name);
    List<SchoolClass> findAllByOrderByNameAsc();
}
