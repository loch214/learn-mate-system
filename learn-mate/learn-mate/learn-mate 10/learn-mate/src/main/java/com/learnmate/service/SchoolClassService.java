package com.learnmate.service;

import com.learnmate.model.SchoolClass;
import com.learnmate.repository.SchoolClassRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SchoolClassService {
    private final SchoolClassRepository schoolClassRepository;

    public SchoolClassService(SchoolClassRepository schoolClassRepository) {
        this.schoolClassRepository = schoolClassRepository;
    }

    public List<SchoolClass> getAllSchoolClasses() {
        return schoolClassRepository.findAllByOrderByNameAsc();
    }

    public Optional<SchoolClass> getSchoolClassById(Long id) {
        return schoolClassRepository.findById(id);
    }

    public SchoolClass createSchoolClass(SchoolClass schoolClass) {
        return schoolClassRepository.save(schoolClass);
    }

    public SchoolClass updateSchoolClass(SchoolClass schoolClass) {
        return schoolClassRepository.save(schoolClass);
    }

    public void deleteSchoolClass(Long id) {
        schoolClassRepository.deleteById(id);
    }

    public List<SchoolClass> searchSchoolClassesByName(String name) {
        return schoolClassRepository.findByNameContainingIgnoreCase(name);
    }
    
    public void removeDuplicateClasses() {
        try {
            List<SchoolClass> allClasses = schoolClassRepository.findAll();
            System.out.println("Starting duplicate class cleanup. Total classes: " + allClasses.size());
            
            // First, remove exact duplicates (same name)
            java.util.Map<String, java.util.List<SchoolClass>> classesByName = allClasses.stream()
                .collect(java.util.stream.Collectors.groupingBy(SchoolClass::getName));
            
            for (java.util.Map.Entry<String, java.util.List<SchoolClass>> entry : classesByName.entrySet()) {
                String className = entry.getKey();
                java.util.List<SchoolClass> classes = entry.getValue();
                
                if (classes.size() > 1) {
                    System.out.println("Found " + classes.size() + " duplicate classes for: " + className);
                    // Keep the first one, delete the rest
                    for (int i = 1; i < classes.size(); i++) {
                        System.out.println("Deleting duplicate class: " + classes.get(i).getId() + " - " + className);
                        schoolClassRepository.deleteById(classes.get(i).getId());
                    }
                }
            }
            
            // Refresh the list after removing exact duplicates
            allClasses = schoolClassRepository.findAll();
            
            // Process "Class X" entries
            List<SchoolClass> classEntries = allClasses.stream()
                .filter(clazz -> clazz.getName().startsWith("Class "))
                .collect(java.util.stream.Collectors.toList());
                
            System.out.println("Found " + classEntries.size() + " Class entries to process");
            
            for (SchoolClass classEntry : classEntries) {
                String gradeName = classEntry.getName().replace("Class ", "Grade ");
                boolean gradeExists = allClasses.stream()
                    .anyMatch(clazz -> clazz.getName().equals(gradeName));
                    
                if (gradeExists) {
                    System.out.println("Removing Class entry: " + classEntry.getName() + " (Grade entry exists: " + gradeName + ")");
                    schoolClassRepository.deleteById(classEntry.getId());
                } else {
                    System.out.println("Converting Class entry to Grade: " + classEntry.getName() + " -> " + gradeName);
                    classEntry.setName(gradeName);
                    schoolClassRepository.save(classEntry);
                }
            }
            
            System.out.println("Duplicate class cleanup completed. Remaining classes: " + schoolClassRepository.count());
            
        } catch (Exception e) {
            System.err.println("Error during duplicate class cleanup: " + e.getMessage());
            e.printStackTrace();
        }
    }
}


