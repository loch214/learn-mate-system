package com.learnmate.controller;

import com.learnmate.model.AnswerSheet;
import com.learnmate.model.Exam;
import com.learnmate.model.Mark;
import com.learnmate.model.Role;
import com.learnmate.model.Subject;
import com.learnmate.model.User;
import com.learnmate.model.SchoolClass;
import com.learnmate.service.AnswerSheetService;
import com.learnmate.service.ExamService;
import com.learnmate.service.FileStorageService;
import com.learnmate.service.MarkService;
import com.learnmate.service.NotificationService;
import com.learnmate.service.SchoolClassService;
import com.learnmate.service.SubjectService;
import com.learnmate.service.UserService;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/exams")
public class ExamController extends BaseController {
    private final ExamService examService;
    private final SubjectService subjectService;
    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final SchoolClassService schoolClassService;
    private final AnswerSheetService answerSheetService;
    private final MarkService markService;
    private final NotificationService notificationService;

    public ExamController(ExamService examService, SubjectService subjectService, UserService userService,
                         FileStorageService fileStorageService, SchoolClassService schoolClassService,
                         AnswerSheetService answerSheetService, MarkService markService,
                         NotificationService notificationService) {
        this.examService = examService;
        this.subjectService = subjectService;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
        this.schoolClassService = schoolClassService;
        this.answerSheetService = answerSheetService;
        this.markService = markService;
        this.notificationService = notificationService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT', 'PARENT', 'ADMIN')")
    public String listExams(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        
        if (hasRole("TEACHER")) {
            // For teachers, show only their exams
            model.addAttribute("exams", examService.getExamsByTeacher(currentUser));
        } else if (hasRole("STUDENT")) {
            // For students, show only exams for their class and subjects they're enrolled in
            SchoolClass studentClass = currentUser.getSchoolClass();
            if (studentClass != null) {
                // Get exams for the student's class
                List<Exam> classExams = examService.getExamsBySchoolClass(studentClass);
                
                // Filter exams to only show subjects the student is enrolled in
                List<Exam> filteredExams = classExams.stream()
                    .filter(exam -> currentUser.getSubjects().contains(exam.getSubject()))
                    .collect(java.util.stream.Collectors.toList());
                
                model.addAttribute("exams", filteredExams);
            } else {
                model.addAttribute("exams", java.util.Collections.emptyList());
            }
        } else {
            // For parents and admins, show all exams
            model.addAttribute("exams", examService.getAllExams());
        }
        
        // Add data for search dropdowns
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
        return "exams/list";
    }

    @GetMapping("/create")
    @PreAuthorize("hasRole('TEACHER')")
    public String createExamForm(Model model) {
        model.addAttribute("exam", new Exam());
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
        return "exams/create";
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('TEACHER')")
    public String createExam(@ModelAttribute Exam exam, BindingResult result, 
                            @RequestParam(value = "file", required = false) MultipartFile file,
                            @RequestParam(value = "subjectId", required = false) Long subjectId,
                            @RequestParam(value = "schoolClassId", required = false) Long schoolClassId,
                            @AuthenticationPrincipal UserDetails userDetails, Model model,
                            RedirectAttributes redirectAttributes) {
        
        // Debug logging
        System.out.println("=== EXAM CREATION DEBUG ===");
        System.out.println("Subject ID: " + subjectId);
        System.out.println("School Class ID: " + schoolClassId);
        System.out.println("Exam object: " + exam);
        
        // Set the subject if subjectId is provided
        if (subjectId != null) {
            Subject subject = subjectService.getSubjectById(subjectId).orElseThrow();
            exam.setSubject(subject);
            System.out.println("Subject set: " + subject.getName());
        } else {
            System.out.println("No subject ID provided!");
        }
        
        // Set the school class if schoolClassId is provided
        if (schoolClassId != null) {
            SchoolClass schoolClass = schoolClassService.getSchoolClassById(schoolClassId).orElseThrow();
            exam.setSchoolClass(schoolClass);
            exam.setGrade(schoolClass.getName()); // Set grade from class name
            System.out.println("School class set: " + schoolClass.getName());
        } else {
            System.out.println("No school class ID provided!");
        }
        
        // Set the teacher BEFORE validation
        User teacher = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        exam.setTeacher(teacher);
        System.out.println("Teacher set: " + teacher.getName());
        
        // Set default title if not provided
        if (exam.getTitle() == null || exam.getTitle().trim().isEmpty()) {
            exam.setTitle(exam.getSubject().getName() + " Exam - " + exam.getGrade());
        }
        
        // Set default description if not provided
        if (exam.getDescription() == null || exam.getDescription().trim().isEmpty()) {
            exam.setDescription("Exam for " + exam.getGrade() + " - " + exam.getSubject().getName());
        }
        
        // Manual validation
        if (exam.getSubject() == null) {
            result.rejectValue("subject", "error.exam", "Subject is required");
        }
        if (exam.getSchoolClass() == null) {
            result.rejectValue("schoolClass", "error.exam", "Class is required");
        }
        if (exam.getTeacher() == null) {
            result.rejectValue("teacher", "error.exam", "Teacher is required");
        }
        if (exam.getDate() == null) {
            result.rejectValue("date", "error.exam", "Date is required");
        }
        if (exam.getMaxMarks() == null) {
            result.rejectValue("maxMarks", "error.exam", "Max marks is required");
        }
        if (exam.getPassMark() == null) {
            result.rejectValue("passMark", "error.exam", "Pass mark is required");
        }
        
        System.out.println("BindingResult errors: " + result.hasErrors());
        if (result.hasErrors()) {
            result.getAllErrors().forEach(error -> System.out.println("Error: " + error.getDefaultMessage()));
            model.addAttribute("subjects", subjectService.getAllSubjects());
            model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
            return "exams/create";
        }
        
        // Handle file upload if provided
        if (file != null && !file.isEmpty()) {
            try {
                String fileName = fileStorageService.storeFile(file, FileStorageService.FileType.EXAM);
                exam.setFilePath(fileName);
            } catch (Exception e) {
                model.addAttribute("error", "Failed to upload file: " + e.getMessage());
                model.addAttribute("subjects", subjectService.getAllSubjects());
                model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
                return "exams/create";
            }
        }
        
        try {
            examService.createExam(exam);

            notificationService.createNotificationForNewExam(exam);

            redirectAttributes.addFlashAttribute("success", "Exam created successfully!");
            return "redirect:/exams/list";
        } catch (Exception e) {
            System.out.println("Error creating exam: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Failed to create exam: " + e.getMessage());
            model.addAttribute("subjects", subjectService.getAllSubjects());
            model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
            return "exams/create";
        }
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public String editExamForm(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails, 
                              Model model, RedirectAttributes redirectAttributes) {
        Exam exam = examService.getExamById(id).orElseThrow();
        
        // Security check - ensure teacher can only edit their own exams
        User currentTeacher = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        if (!exam.getTeacher().getId().equals(currentTeacher.getId())) {
            redirectAttributes.addFlashAttribute("error", "You can only edit your own exams!");
            return "redirect:/exams/list";
        }
        
        model.addAttribute("exam", exam);
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
        return "exams/edit";
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public String updateExam(@PathVariable Long id, @ModelAttribute Exam exam, BindingResult result, 
                            @RequestParam(value = "file", required = false) MultipartFile file,
                            @RequestParam(value = "subjectId", required = false) Long subjectId,
                            @RequestParam(value = "schoolClassId", required = false) Long schoolClassId,
                            @AuthenticationPrincipal UserDetails userDetails, Model model,
                            RedirectAttributes redirectAttributes) {
        
        // Security check - ensure teacher can only update their own exams
        Exam existingExam = examService.getExamById(id).orElseThrow();
        User currentTeacher = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        if (!existingExam.getTeacher().getId().equals(currentTeacher.getId())) {
            redirectAttributes.addFlashAttribute("error", "You can only edit your own exams!");
            return "redirect:/exams/list";
        }
        
        // Set the subject if subjectId is provided
        if (subjectId != null) {
            Subject subject = subjectService.getSubjectById(subjectId).orElseThrow();
            exam.setSubject(subject);
        }
        
        // Set the school class if schoolClassId is provided
        if (schoolClassId != null) {
            SchoolClass schoolClass = schoolClassService.getSchoolClassById(schoolClassId).orElseThrow();
            exam.setSchoolClass(schoolClass);
            exam.setGrade(schoolClass.getName()); // Set grade from class name
        }
        
        // Manual validation
        if (exam.getSubject() == null) {
            result.rejectValue("subject", "error.exam", "Subject is required");
        }
        if (exam.getSchoolClass() == null) {
            result.rejectValue("schoolClass", "error.exam", "Class is required");
        }
        if (exam.getDate() == null) {
            result.rejectValue("date", "error.exam", "Date is required");
        }
        if (exam.getMaxMarks() == null) {
            result.rejectValue("maxMarks", "error.exam", "Max marks is required");
        }
        if (exam.getPassMark() == null) {
            result.rejectValue("passMark", "error.exam", "Pass mark is required");
        }
        
        if (result.hasErrors()) {
            model.addAttribute("subjects", subjectService.getAllSubjects());
            model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
            return "exams/edit";
        }
        
        // Handle file upload if a new file is provided
        if (file != null && !file.isEmpty()) {
            try {
                String fileName = fileStorageService.storeFile(file, FileStorageService.FileType.EXAM);
                exam.setFilePath(fileName);
            } catch (Exception e) {
                model.addAttribute("error", "Failed to upload file: " + e.getMessage());
                model.addAttribute("subjects", subjectService.getAllSubjects());
                model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
                model.addAttribute("exam", exam);
                return "exams/edit";
            }
        } else {
            // Preserve existing file path if no new file is uploaded
            exam.setFilePath(existingExam.getFilePath());
        }
        
        exam.setId(id);
        exam.setTeacher(existingExam.getTeacher()); // Preserve the teacher
        
        try {
            examService.updateExam(exam);
            redirectAttributes.addFlashAttribute("success", "Exam updated successfully!");
            return "redirect:/exams/list";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to update exam: " + e.getMessage());
            model.addAttribute("subjects", subjectService.getAllSubjects());
            model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
            return "exams/edit";
        }
    }

    @GetMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String deleteExam(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        Exam exam = examService.getExamById(id).orElseThrow();
        
        // Security check - teachers can only delete their own exams
        if (hasRole("TEACHER")) {
            User currentTeacher = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
            if (!exam.getTeacher().getId().equals(currentTeacher.getId())) {
                redirectAttributes.addFlashAttribute("error", "You can only delete your own exams!");
                return "redirect:/exams/list";
            }
        }
        
        examService.deleteExam(id);
        redirectAttributes.addFlashAttribute("success", "Exam deleted successfully!");
        return "redirect:/exams/list";
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT', 'PARENT', 'ADMIN')")
    public String searchExams(@RequestParam(required = false) Long subjectId, Model model) {
        try {
            if (subjectId == null) {
                model.addAttribute("error", "Please select a subject to search.");
                model.addAttribute("exams", java.util.Collections.emptyList());
            } else {
                Subject subject = subjectService.getSubjectById(subjectId).orElseThrow();
                model.addAttribute("exams", examService.getExamsBySubject(subject));
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error searching exams: " + e.getMessage());
            model.addAttribute("exams", java.util.Collections.emptyList());
        }
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
        return "exams/list";
    }


    @GetMapping("/search-by-student-name")
    @PreAuthorize("hasRole('PARENT')")
    public String searchExamsByStudentName(@RequestParam(required = false) String studentName, Model model) {
        try {
            if (studentName == null || studentName.trim().isEmpty()) {
                model.addAttribute("error", "Please enter a student name to search.");
                model.addAttribute("exams", java.util.Collections.emptyList());
            } else {
                // For parents, search by student name (this would need a custom service method)
                // For now, return all exams - in a real implementation, you'd filter by parent's children's classes
                model.addAttribute("exams", examService.getAllExams());
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error searching exams by student name: " + e.getMessage());
            model.addAttribute("exams", java.util.Collections.emptyList());
        }
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
        return "exams/list";
    }

    @GetMapping("/download/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT', 'PARENT', 'ADMIN')")
    public ResponseEntity<Resource> downloadExam(@PathVariable Long id) {
        try {
            Exam exam = examService.getExamById(id).orElseThrow();
            
            if (exam.getFilePath() == null || exam.getFilePath().isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Path filePath = fileStorageService.getFileStorageLocationPublic(FileStorageService.FileType.EXAM)
                    .resolve(exam.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, 
                                "attachment; filename=\"exam_" + exam.getId() + "_" + exam.getSubject().getName() + ".pdf\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/download-answer/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT', 'PARENT', 'ADMIN')")
    public ResponseEntity<Resource> downloadAnswerSheet(@PathVariable Long id) {
        try {
            AnswerSheet answerSheet = answerSheetService.getAnswerSheetById(id).orElseThrow();
            
            if (answerSheet.getFilePath() == null || answerSheet.getFilePath().isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Path filePath = fileStorageService.getFileStorageLocationPublic(FileStorageService.FileType.ANSWER_SHEET)
                    .resolve(answerSheet.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, 
                                "attachment; filename=\"answer_" + answerSheet.getId() + "_" + answerSheet.getStudent().getName() + ".pdf\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/upload-answer/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public String uploadAnswerForm(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails, 
                                  Model model, RedirectAttributes redirectAttributes) {
        Exam exam = examService.getExamById(id).orElseThrow();
        User student = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        
        // Check if student is enrolled in the subject and belongs to the class
        if (!student.getSubjects().contains(exam.getSubject()) || 
            !exam.getSchoolClass().equals(student.getSchoolClass())) {
            redirectAttributes.addFlashAttribute("error", "You are not authorized to upload answers for this exam!");
            return "redirect:/exams/list";
        }
        
        // Check if answer sheet already exists
        Optional<AnswerSheet> existingAnswerSheet = answerSheetService.getAnswerSheetByExamAndStudent(exam, student);
        
        model.addAttribute("exam", exam);
        model.addAttribute("answerSheet", existingAnswerSheet.orElse(new AnswerSheet()));
        return "exams/upload-answer";
    }

    @PostMapping("/upload-answer/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public String uploadAnswer(@PathVariable Long id, @RequestParam("file") MultipartFile file,
                              @AuthenticationPrincipal UserDetails userDetails, Model model,
                              RedirectAttributes redirectAttributes) {
        try {
            Exam exam = examService.getExamById(id).orElseThrow();
            User student = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
            
            // Check if student is enrolled in the subject and belongs to the class
            if (!student.getSubjects().contains(exam.getSubject()) || 
                !exam.getSchoolClass().equals(student.getSchoolClass())) {
                redirectAttributes.addFlashAttribute("error", "You are not authorized to upload answers for this exam!");
                return "redirect:/exams/list";
            }
            
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please select a file to upload!");
                return "redirect:/exams/upload-answer/" + id;
            }
            
            // Store the file
            String fileName = fileStorageService.storeFile(file, FileStorageService.FileType.ANSWER_SHEET);
            
            // Check if answer sheet already exists
            Optional<AnswerSheet> existingAnswerSheet = answerSheetService.getAnswerSheetByExamAndStudent(exam, student);
            AnswerSheet answerSheet;
            
            if (existingAnswerSheet.isPresent()) {
                // Update existing answer sheet
                answerSheet = existingAnswerSheet.get();
                answerSheet.setFilePath(fileName);
                answerSheet.setSubmittedAt(java.time.LocalDateTime.now());
                answerSheet.setStatus("SUBMITTED");
            } else {
                // Create new answer sheet
                answerSheet = new AnswerSheet();
                answerSheet.setExam(exam);
                answerSheet.setStudent(student);
                answerSheet.setFilePath(fileName);
            }
            
            answerSheetService.createAnswerSheet(answerSheet);
            redirectAttributes.addFlashAttribute("success", "Answer sheet uploaded successfully!");
            return "redirect:/exams/list";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload answer sheet: " + e.getMessage());
            return "redirect:/exams/upload-answer/" + id;
        }
    }

    @GetMapping("/review-answers/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public String reviewAnswers(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails,
                               Model model, RedirectAttributes redirectAttributes) {
        try {
            Exam exam = examService.getExamById(id).orElseThrow();
            User teacher = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
            
            // Security check - ensure teacher can only review their own exams
            if (!exam.getTeacher().getId().equals(teacher.getId())) {
                redirectAttributes.addFlashAttribute("error", "You can only review answers for your own exams!");
                return "redirect:/exams/list";
            }
            
            // Debug logging
            System.out.println("Exam found: " + exam.getTitle());
            System.out.println("Exam subject: " + (exam.getSubject() != null ? exam.getSubject().getName() : "NULL"));
            System.out.println("Exam school class: " + (exam.getSchoolClass() != null ? exam.getSchoolClass().getName() : "NULL"));
            
            // Get all students in the exam's class who are enrolled in the subject
            List<User> students = new java.util.ArrayList<>();
            if (exam.getSchoolClass() != null && exam.getSubject() != null) {
                students = userService.getUsersByRole(Role.STUDENT).stream()
                    .filter(student -> exam.getSchoolClass().equals(student.getSchoolClass()))
                    .filter(student -> student.getSubjects() != null && student.getSubjects().contains(exam.getSubject()))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // Get answer sheets for this exam
            List<AnswerSheet> answerSheets = answerSheetService.getAnswerSheetsByExam(exam);
            
            // Get existing marks for this exam
            List<Mark> existingMarks = markService.getMarksByExam(exam);
            
            System.out.println("Students found: " + students.size());
            System.out.println("Answer sheets found: " + answerSheets.size());
            System.out.println("Existing marks found: " + existingMarks.size());
            
            // Debug: Print existing marks details
            for (Mark mark : existingMarks) {
                System.out.println("Mark for student " + mark.getStudent().getName() + ": " + mark.getScore());
            }
            
            model.addAttribute("exam", exam);
            model.addAttribute("students", students);
            model.addAttribute("answerSheets", answerSheets);
            model.addAttribute("existingMarks", existingMarks);
            return "exams/review-answers";
        } catch (Exception e) {
            System.out.println("Error in reviewAnswers: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error loading review page: " + e.getMessage());
            return "redirect:/exams/list";
        }
    }

    @PostMapping("/grade-answer/{answerSheetId}")
    @PreAuthorize("hasRole('TEACHER')")
    public String gradeAnswer(@PathVariable Long answerSheetId, 
                             @RequestParam("score") Integer score,
                             @RequestParam(value = "comments", required = false) String comments,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        try {
            AnswerSheet answerSheet = answerSheetService.getAnswerSheetById(answerSheetId).orElseThrow();
            User teacher = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
            
            // Security check - ensure teacher can only grade their own exams
            if (!answerSheet.getExam().getTeacher().getId().equals(teacher.getId())) {
                redirectAttributes.addFlashAttribute("error", "You can only grade answers for your own exams!");
                return "redirect:/exams/list";
            }
            
            answerSheet.setStatus("GRADED");
            answerSheet.setComments(comments);
            answerSheetService.updateAnswerSheet(answerSheet);
            
            // Create or update mark record
            Mark existingMark = markService.getMarkByExamAndStudent(answerSheet.getExam(), answerSheet.getStudent()).orElse(null);
            if (existingMark != null) {
                // Update existing mark
                existingMark.setScore(score);
                existingMark.setPublished(true);
                markService.updateMark(existingMark);
            } else {
                // Create new mark
                Mark mark = new Mark();
                mark.setExam(answerSheet.getExam());
                mark.setStudent(answerSheet.getStudent());
                mark.setScore(score);
                mark.setPublished(true);
                markService.createMark(mark);
            }
            
            redirectAttributes.addFlashAttribute("success", "Answer graded successfully!");
            return "redirect:/exams/review-answers/" + answerSheet.getExam().getId();
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to grade answer: " + e.getMessage());
            return "redirect:/exams/list";
        }
    }

    @PostMapping("/grade-exam/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public String gradeExamAnswerSheets(@PathVariable Long id,
                                       @RequestParam Map<String, String> allParams,
                                       @AuthenticationPrincipal UserDetails userDetails,
                                       RedirectAttributes redirectAttributes) {
        try {
            Exam exam = examService.getExamById(id).orElseThrow();
            User teacher = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
            
            // Security check - ensure teacher can only grade their own exams
            if (!exam.getTeacher().getId().equals(teacher.getId())) {
                redirectAttributes.addFlashAttribute("error", "You can only grade your own exams!");
                return "redirect:/exams/list";
            }
            
            // Get all students in the exam's class who are enrolled in the subject
            List<User> students = userService.getUsersByRole(Role.STUDENT).stream()
                .filter(student -> exam.getSchoolClass().equals(student.getSchoolClass()))
                .filter(student -> student.getSubjects().contains(exam.getSubject()))
                .collect(java.util.stream.Collectors.toList());
            
            int gradedCount = 0;
            
            // Process grades for each student
            for (User student : students) {
                String scoreKey = "score_" + student.getId();
                String commentsKey = "comments_" + student.getId();
                String scoreValue = allParams.get(scoreKey);
                String commentsValue = allParams.get(commentsKey);
                
                if (scoreValue != null && !scoreValue.trim().isEmpty()) {
                    try {
                        int score = Integer.parseInt(scoreValue);
                        if (score >= 0 && score <= exam.getMaxMarks()) {
                            // Get or create answer sheet
                            AnswerSheet answerSheet = answerSheetService.getAnswerSheetByExamAndStudent(exam, student)
                                .orElse(null);
                            
                            if (answerSheet != null) {
                                // Update answer sheet
                                answerSheet.setStatus("GRADED");
                                answerSheet.setComments(commentsValue);
                                answerSheetService.updateAnswerSheet(answerSheet);
                            }
                            
                            // Create or update mark record
                            Mark existingMark = markService.getMarkByExamAndStudent(exam, student).orElse(null);
                            if (existingMark != null) {
                                // Update existing mark
                                existingMark.setScore(score);
                                existingMark.setPublished(true);
                                markService.updateMark(existingMark);
                            } else {
                                // Create new mark
                                Mark mark = new Mark();
                                mark.setExam(exam);
                                mark.setStudent(student);
                                mark.setScore(score);
                                mark.setPublished(true);
                                markService.createMark(mark);
                            }
                            
                            gradedCount++;
                        }
                    } catch (NumberFormatException e) {
                        // Skip invalid scores
                    }
                }
            }
            
            // Send notification to students about marks being published
            // if (gradedCount > 0 && exam.getSchoolClass() != null && exam.getSubject() != null) {
            //     notificationService.notifyMarksSubmitted(
            //         exam.getTitle(), 
            //         exam.getSubject().getName(), 
            //         exam.getSchoolClass()
            //     );
            // }
            
            redirectAttributes.addFlashAttribute("success", 
                "Successfully graded " + gradedCount + " students for " + exam.getSubject().getName() + " - " + exam.getSchoolClass().getName());
            
            return "redirect:/exams/review-answers/" + exam.getId();
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to grade students: " + e.getMessage());
            return "redirect:/exams/list";
        }
    }

    @PostMapping("/edit-mark/{examId}")
    @PreAuthorize("hasRole('TEACHER')")
    public String editMark(@PathVariable Long examId,
                          @RequestParam("studentId") Long studentId,
                          @RequestParam("score") Integer score,
                          @RequestParam(value = "comments", required = false) String comments,
                          @AuthenticationPrincipal UserDetails userDetails,
                          RedirectAttributes redirectAttributes) {
        System.out.println("=== EDIT MARK REQUEST RECEIVED ===");
        System.out.println("Exam ID: " + examId);
        System.out.println("Student ID: " + studentId);
        System.out.println("New Score: " + score);
        System.out.println("Comments: " + comments);
        try {
            Exam exam = examService.getExamById(examId).orElseThrow();
            User teacher = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
            
            // Security check - ensure teacher can only edit marks for their own exams
            if (!exam.getTeacher().getId().equals(teacher.getId())) {
                redirectAttributes.addFlashAttribute("error", "You can only edit marks for your own exams!");
                return "redirect:/exams/list";
            }
            
            User student = userService.getUserById(studentId).orElseThrow();
            
            // Find existing mark
            Mark existingMark = markService.getMarkByExamAndStudent(exam, student).orElse(null);
            
            if (existingMark != null) {
                // Update existing mark
                System.out.println("=== UPDATING EXISTING MARK ===");
                System.out.println("Student: " + student.getName());
                System.out.println("Old Score: " + existingMark.getScore());
                System.out.println("New Score: " + score);
                System.out.println("Old Comments: " + existingMark.getComments());
                System.out.println("New Comments: " + comments);
                
                existingMark.setScore(score);
                existingMark.setComments(comments);
                Mark updatedMark = markService.updateMark(existingMark);
                
                System.out.println("Updated Mark Score: " + updatedMark.getScore());
                System.out.println("Updated Mark Comments: " + updatedMark.getComments());
                redirectAttributes.addFlashAttribute("success", "Mark updated successfully for " + student.getName());
            } else {
                // Create new mark
                Mark mark = new Mark();
                mark.setExam(exam);
                mark.setStudent(student);
                mark.setScore(score);
                mark.setComments(comments);
                mark.setPublished(true);
                markService.createMark(mark);
                redirectAttributes.addFlashAttribute("success", "Mark created successfully for " + student.getName());
            }
            
            return "redirect:/exams/review-answers/" + examId;
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update mark: " + e.getMessage());
            return "redirect:/exams/review-answers/" + examId;
        }
    }
}


