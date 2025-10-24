package com.learnmate.service;

import com.learnmate.model.SchoolClass;
import com.learnmate.model.Subject;
import com.learnmate.model.Timetable;
import com.learnmate.model.User;
import com.learnmate.repository.TimetableRepository;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class TimetableService {
    private final TimetableRepository timetableRepository;

    public TimetableService(TimetableRepository timetableRepository) {
        this.timetableRepository = timetableRepository;
    }

    public List<Timetable> getAllTimetables() {
        return timetableRepository.findAll();
    }

    public Optional<Timetable> getTimetableById(Long id) {
        return timetableRepository.findById(id);
    }

    public Timetable createTimetable(Timetable timetable) {
        // Check for time conflicts
        List<Timetable> existing = timetableRepository.findBySchoolClass(timetable.getSchoolClass());
        for (Timetable t : existing) {
            // If updating, exclude the current timetable from conflict check
            if (timetable.getId() != null && t.getId().equals(timetable.getId())) {
                continue; // Skip self-comparison
            }
            
            if (t.getDay() == timetable.getDay() &&
                    timetable.getStartTime().isBefore(t.getEndTime()) &&
                    timetable.getEndTime().isAfter(t.getStartTime())) {
                System.out.println("=== TIME CONFLICT DETECTED ===");
                System.out.println("Existing timetable ID: " + t.getId());
                System.out.println("Current timetable ID: " + timetable.getId());
                System.out.println("Day: " + t.getDay() + " vs " + timetable.getDay());
                System.out.println("Time: " + t.getStartTime() + "-" + t.getEndTime() + " vs " + timetable.getStartTime() + "-" + timetable.getEndTime());
                throw new RuntimeException("Schedule conflict");
            }
        }

        // Check for subject duplication on the same day
        if (timetable.getSubject() != null) {
            List<Timetable> subjectConflicts = timetableRepository.findBySchoolClassAndDayAndSubject(
                    timetable.getSchoolClass(), timetable.getDay(), timetable.getSubject());

            System.out.println("=== CONFLICT CHECK ===");
            System.out.println("Timetable ID: " + timetable.getId());
            System.out.println("Subject: " + timetable.getSubject().getName());
            System.out.println("Day: " + timetable.getDay());
            System.out.println("Class: " + timetable.getSchoolClass().getName());
            System.out.println("Found conflicts before filtering: " + subjectConflicts.size());
            
            // If updating, exclude the current timetable from conflict check
            if (timetable.getId() != null) {
                subjectConflicts = subjectConflicts.stream()
                        .filter(t -> !t.getId().equals(timetable.getId()))
                        .toList();
                System.out.println("Found conflicts after filtering: " + subjectConflicts.size());
            }

            if (!subjectConflicts.isEmpty()) {
                System.out.println("CONFLICT DETECTED! Blocking save.");
                throw new RuntimeException("Subject '" + timetable.getSubject().getName() +
                        "' is already scheduled on " + timetable.getDay() +
                        " for class " + timetable.getSchoolClass().getName() +
                        ". One subject can only be taught once per day for a class.");
            } else {
                System.out.println("No conflicts found. Proceeding with save.");
            }
        }

        return timetableRepository.save(timetable);
    }

    public Timetable updateTimetable(Timetable timetable) {
        return createTimetable(timetable); // Reuse conflict check
    }

    public void deleteTimetable(Long id) {
        timetableRepository.deleteById(id);
    }

    public List<Timetable> getTimetablesBySchoolClass(SchoolClass schoolClass) {
        return timetableRepository.findBySchoolClass(schoolClass);
    }

    public List<Timetable> getTimetablesByDay(DayOfWeek day) {
        return timetableRepository.findByDay(day);
    }

    public List<Timetable> getTimetablesByTeacher(User teacher) {
        return timetableRepository.findByTeacher(teacher);
    }

    public List<Timetable> getTimetablesByClassAndSubjects(SchoolClass schoolClass, Set<Subject> subjects) {
        return timetableRepository.findBySchoolClassAndSubjectIn(schoolClass, subjects);
    }
    
    public List<Timetable> getTimetablesByClassAndDay(SchoolClass schoolClass, DayOfWeek day) {
        return timetableRepository.findBySchoolClassAndDay(schoolClass, day);
    }
}

