package com.learnmate.config;

import com.learnmate.model.Fee;
import com.learnmate.model.Role;
import com.learnmate.model.Subject;
import com.learnmate.model.User;
import com.learnmate.service.FeeService;
import com.learnmate.service.SubjectService;
import com.learnmate.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class FeeInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(FeeInitializer.class);

    private final FeeService feeService;
    private final SubjectService subjectService;
    private final UserService userService;

    @Value("${app.seed-fees.enabled:true}")
    private boolean feeSeedEnabled;

    public FeeInitializer(FeeService feeService, SubjectService subjectService, UserService userService) {
        this.feeService = feeService;
        this.subjectService = subjectService;
        this.userService = userService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!feeSeedEnabled) {
            log.info("Fee seeding is disabled");
            return;
        }

        try {
            log.info("=== Initializing Sample Fees ===");

            List<Subject> subjects = subjectService.getAllSubjects();
            List<User> students = userService.getUsersByRole(Role.STUDENT);

            log.info("Subjects found: {}", subjects.size());
            log.info("Students found: {}", students.size());

            if (subjects.isEmpty() || students.isEmpty()) {
                log.info("Skipping fee initialization - missing subjects or students");
                return;
            }

            // Check if fees already exist
            if (!feeService.getAllFees().isEmpty()) {
                log.info("Fees already exist, skipping initialization");
                return;
            }

            // Create sample fees for each subject
            for (Subject subject : subjects) {
                log.info("Creating sample fees for subject: {}", subject.getName());
                createSampleFeesForSubject(subject, students);
            }

            log.info("=== Sample Fees Initialization Complete ===");
        } catch (Exception e) {
            log.error("Error initializing sample fees: {}", e.getMessage(), e);
        }
    }

    private void createSampleFeesForSubject(Subject subject, List<User> students) {
        // Create fees for students enrolled in this subject
        for (User student : students) {
            if (student.getSubjects() != null && student.getSubjects().contains(subject)) {
                try {
                    // Create a pending fee for this subject
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

    private double getFeeAmountForSubject(String subjectName) {
        // Set different fee amounts based on subject
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
