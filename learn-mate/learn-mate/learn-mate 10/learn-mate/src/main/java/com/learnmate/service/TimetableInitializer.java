package com.learnmate.service;

import com.learnmate.model.*;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Component
public class TimetableInitializer implements CommandLineRunner {
    private final TimetableService timetableService;
    private final SchoolClassService schoolClassService;
    private final SubjectService subjectService;
    private final UserService userService;

    public TimetableInitializer(TimetableService timetableService, SchoolClassService schoolClassService,
                               SubjectService subjectService, UserService userService) {
        this.timetableService = timetableService;
        this.schoolClassService = schoolClassService;
        this.subjectService = subjectService;
        this.userService = userService;
    }

    @Override
    public void run(String... args) throws Exception {
        initializeDefaultTimetables();
    }

    private void initializeDefaultTimetables() {
        try {
            System.out.println("=== Initializing Default Timetables ===");
            
            List<SchoolClass> classes = schoolClassService.getAllSchoolClasses();
            List<Subject> subjects = subjectService.getAllSubjects();
            List<User> teachers = userService.getUsersByRole(Role.TEACHER);
            
            System.out.println("Classes found: " + classes.size());
            System.out.println("Subjects found: " + subjects.size());
            System.out.println("Teachers found: " + teachers.size());
            
            if (classes.isEmpty() || subjects.isEmpty() || teachers.isEmpty()) {
                System.out.println("Skipping timetable initialization - missing data");
                return;
            }
            
            // Check if timetables already exist
            if (!timetableService.getAllTimetables().isEmpty()) {
                System.out.println("Timetables already exist, skipping initialization");
                return;
            }
            
            // Create default timetables for each class
            for (SchoolClass schoolClass : classes) {
                System.out.println("Creating timetables for class: " + schoolClass.getName());
                createDefaultTimetableForClass(schoolClass, subjects, teachers);
            }
            
            System.out.println("=== Default Timetables Initialization Complete ===");
        } catch (Exception e) {
            System.out.println("Error initializing timetables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createDefaultTimetableForClass(SchoolClass schoolClass, List<Subject> subjects, List<User> teachers) {
        System.out.println("Creating timetables for class: " + schoolClass.getName());
        
        // Default time slots
        LocalTime[] timeSlots = {
            LocalTime.of(8, 0),   // 8:00 AM
            LocalTime.of(9, 0),   // 9:00 AM
            LocalTime.of(10, 0),  // 10:00 AM
            LocalTime.of(11, 0),  // 11:00 AM
            LocalTime.of(12, 0),  // 12:00 PM
            LocalTime.of(13, 0),  // 1:00 PM
            LocalTime.of(14, 0),  // 2:00 PM
            LocalTime.of(15, 0)   // 3:00 PM
        };
        
        DayOfWeek[] days = {
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        };
        
        String[] rooms = {"Room 101", "Room 102", "Room 103", "Room 104", "Room 105", "Room 201", "Room 202", "Room 203"};
        
        int teacherIndex = 0;
        int roomIndex = 0;
        
        // Create timetables for each day
        for (int dayIndex = 0; dayIndex < days.length; dayIndex++) {
            DayOfWeek day = days[dayIndex];

            // Calculate how many periods we can have per day (max 6 periods: 8:00 AM to 2:00 PM)
            int maxPeriodsPerDay = Math.min(6, subjects.size());

            // For each day, assign different subjects starting from a different offset
            // This ensures better distribution of subjects across the week
            int startSubjectIndex = (dayIndex * maxPeriodsPerDay) % subjects.size();

            for (int period = 0; period < maxPeriodsPerDay; period++) {
                // Calculate subject index, ensuring we don't repeat subjects on the same day
                int subjectIndex = (startSubjectIndex + period) % subjects.size();

                // Skip if we've already used all unique subjects for this day
                if (period >= subjects.size()) {
                    break;
                }
                
                if (teacherIndex >= teachers.size()) {
                    teacherIndex = 0; // Reset to first teacher
                }
                
                if (roomIndex >= rooms.length) {
                    roomIndex = 0; // Reset to first room
                }
                
                Subject subject = subjects.get(subjectIndex);
                User teacher = teachers.get(teacherIndex);
                
                Timetable timetable = new Timetable();
                timetable.setSchoolClass(schoolClass);
                timetable.setTeacher(teacher);
                timetable.setSubject(subject);
                timetable.setTitle(subject.getName() + " - " + schoolClass.getName());
                timetable.setDescription(subject.getName() + " class for " + schoolClass.getName());
                timetable.setDay(day);
                timetable.setStartTime(timeSlots[period]);
                timetable.setEndTime(timeSlots[period + 1]);
                timetable.setRoom(rooms[roomIndex]);
                
                try {
                    timetableService.createTimetable(timetable);
                    System.out.println("Created timetable: " + subject.getName() + " on " + day + " at " + timeSlots[period] + " for teacher " + teacher.getName());
                } catch (Exception e) {
                    System.out.println("Error creating timetable: " + e.getMessage());
                    // Skip this timetable if there's a conflict and continue with the next one
                }
                
                teacherIndex++;
                roomIndex++;
            }
        }
    }
}

