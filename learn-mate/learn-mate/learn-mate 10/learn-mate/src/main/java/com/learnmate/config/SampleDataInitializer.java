package com.learnmate.config;

import com.learnmate.model.*;
import com.learnmate.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class SampleDataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(SampleDataInitializer.class);

    private final UserService userService;
    private final SubjectService subjectService;
    private final SchoolClassService schoolClassService;
    private final FeeService feeService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed-sample-data.enabled:true}")
    private boolean sampleDataEnabled;

    public SampleDataInitializer(UserService userService, SubjectService subjectService, 
                                SchoolClassService schoolClassService, FeeService feeService,
                                PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.subjectService = subjectService;
        this.schoolClassService = schoolClassService;
        this.feeService = feeService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!sampleDataEnabled) {
            log.info("Sample data seeding is disabled");
            return;
        }

        try {
            log.info("=== Initializing Sample Data ===");
            
            // Check if data already exists
            if (!userService.getUsersByRole(Role.STUDENT).isEmpty()) {
                log.info("Sample data already exists, skipping initialization");
                return;
            }
            
            // Create sample subjects
            createSampleSubjects();
            
            // Create sample school classes
            createSampleSchoolClasses();
            
            // Create sample students
            createSampleStudents();
            
            // Create sample fees
            createSampleFees();
            
            log.info("=== Sample Data Initialization Complete ===");
        } catch (Exception e) {
            log.error("Error initializing sample data: {}", e.getMessage(), e);
        }
    }

    private void createSampleSubjects() {
        String[] subjectNames = {"Mathematics", "English", "Science", "Physics", "Chemistry", "Biology"};
        
        for (String subjectName : subjectNames) {
            try {
                Subject subject = new Subject();
                subject.setName(subjectName);
                subjectService.createSubject(subject);
                log.info("Created subject: {}", subjectName);
            } catch (Exception e) {
                log.error("Error creating subject {}: {}", subjectName, e.getMessage());
            }
        }
    }

    private void createSampleSchoolClasses() {
        String[] classNames = {"Grade 6", "Grade 7", "Grade 8", "Grade 9", "Grade 10"};
        
        for (String className : classNames) {
            try {
                SchoolClass schoolClass = new SchoolClass();
                schoolClass.setName(className);
                schoolClassService.createSchoolClass(schoolClass);
                log.info("Created school class: {}", className);
            } catch (Exception e) {
                log.error("Error creating school class {}: {}", className, e.getMessage());
            }
        }
    }

    private void createSampleStudents() {
        List<Subject> subjects = subjectService.getAllSubjects();
        List<SchoolClass> classes = schoolClassService.getAllSchoolClasses();
        
        if (subjects.isEmpty() || classes.isEmpty()) {
            log.warn("No subjects or classes available for student creation");
            return;
        }
        
        // Create sample students
        String[] studentNames = {"student1", "student2", "student3", "John Doe", "Jane Smith", "Mike Johnson"};
        String[] emails = {"student1@gmail.com", "student2@gmail.com", "student3@gmail.com", 
                          "john.doe@gmail.com", "jane.smith@gmail.com", "mike.johnson@gmail.com"};
        
        for (int i = 0; i < studentNames.length; i++) {
            try {
                User student = new User();
                student.setUsername(studentNames[i]);
                student.setPassword(passwordEncoder.encode("password123"));
                student.setEmail(emails[i]);
                student.setName(studentNames[i]);
                student.setContact("+1-555-000" + (i + 1));
                student.setRole(Role.STUDENT);
                student.setActive(true);
                
                // Assign to a class
                student.setSchoolClass(classes.get(i % classes.size()));
                
                // Enroll in subjects (3-4 subjects per student)
                Set<Subject> studentSubjects = new HashSet<>();
                int numSubjects = 3 + (i % 2); // 3 or 4 subjects
                for (int j = 0; j < numSubjects && j < subjects.size(); j++) {
                    studentSubjects.add(subjects.get(j));
                }
                student.setSubjects(studentSubjects);
                
                userService.save(student);
                log.info("Created student: {} with {} subjects", studentNames[i], studentSubjects.size());
            } catch (Exception e) {
                log.error("Error creating student {}: {}", studentNames[i], e.getMessage());
            }
        }
    }

    private void createSampleFees() {
        List<Subject> subjects = subjectService.getAllSubjects();
        List<User> students = userService.getUsersByRole(Role.STUDENT);
        
        if (subjects.isEmpty() || students.isEmpty()) {
            log.warn("No subjects or students available for fee creation");
            return;
        }
        
        // Create fees for each student's enrolled subjects
        for (User student : students) {
            if (student.getSubjects() != null) {
                for (Subject subject : student.getSubjects()) {
                    try {
                        Fee fee = new Fee();
                        fee.setStudent(student);
                        fee.setSubject(subject);
                        fee.setAmount(getFeeAmountForSubject(subject.getName()));
                        fee.setDueDate(LocalDate.now().plusDays(30)); // Due in 30 days
                        fee.setStatus("PENDING");
                        
                        feeService.createFee(fee);
                        log.info("Created fee for student {} in subject {}: ${}", 
                                student.getName(), subject.getName(), fee.getAmount());
                    } catch (Exception e) {
                        log.error("Error creating fee for student {} in subject {}: {}", 
                                student.getName(), subject.getName(), e.getMessage());
                    }
                }
            }
        }
    }

    private double getFeeAmountForSubject(String subjectName) {
        switch (subjectName.toLowerCase()) {
            case "mathematics":
                return 150.00;
            case "english":
                return 120.00;
            case "science":
                return 180.00;
            case "physics":
                return 200.00;
            case "chemistry":
                return 190.00;
            case "biology":
                return 170.00;
            default:
                return 100.00;
        }
    }
}
