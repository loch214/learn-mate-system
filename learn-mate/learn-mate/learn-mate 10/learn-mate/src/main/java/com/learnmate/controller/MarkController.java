package com.learnmate.controller;

import com.learnmate.model.AnswerSheet;
import com.learnmate.model.Exam;
import com.learnmate.model.Mark;
import com.learnmate.model.Role;
import com.learnmate.model.User;
import com.learnmate.service.AnswerSheetService;
import com.learnmate.service.ExamService;
import com.learnmate.service.MarkService;
import com.learnmate.service.SubjectService;
import com.learnmate.service.UserService;
import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/marks")
public class MarkController extends BaseController {
    private final MarkService markService;
    private final ExamService examService;
    private final UserService userService;
    private final SubjectService subjectService;
    private final AnswerSheetService answerSheetService;

    public MarkController(MarkService markService, ExamService examService, UserService userService, 
                         SubjectService subjectService, AnswerSheetService answerSheetService) {
        this.markService = markService;
        this.examService = examService;
        this.userService = userService;
        this.subjectService = subjectService;
        this.answerSheetService = answerSheetService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT', 'PARENT', 'ADMIN')")
    public String redirectToMarksList() {
        return "redirect:/marks/list";
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT', 'PARENT', 'ADMIN')")
    public String listMarks(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        // For teachers, show only marks for their exams. For others, show all marks.
        if (hasRole("TEACHER")) {
            User teacher = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
            model.addAttribute("marks", markService.getMarksByTeacher(teacher));
        } else {
            model.addAttribute("marks", markService.getAllMarks());
        }
        
        // Add data for search dropdowns
        model.addAttribute("subjects", subjectService.getAllSubjects());
        return "marks/list";
    }

    @GetMapping("/create")
    @PreAuthorize("hasRole('TEACHER')")
    public String createMarkForm(Model model) {
        model.addAttribute("mark", new Mark());
        model.addAttribute("exams", examService.getAllExams());
        model.addAttribute("students", userService.getUsersByRole(Role.STUDENT));
        return "marks/create";
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('TEACHER')")
    public String createMark(@Valid @ModelAttribute Mark mark, BindingResult result, Model model) {
        if (mark.getExam() == null || mark.getExam().getId() == null) {
            result.rejectValue("exam", "error.mark", "Please select an exam");
        }
        if (mark.getStudent() == null || mark.getStudent().getId() == null) {
            result.rejectValue("student", "error.mark", "Please select a student");
        }

        if (result.hasErrors()) {
            model.addAttribute("exams", examService.getAllExams());
            model.addAttribute("students", userService.getUsersByRole(Role.STUDENT));
            return "marks/create";
        }

        Mark preparedMark = new Mark();
        preparedMark.setExam(examService.getExamById(mark.getExam().getId()).orElseThrow());
        preparedMark.setStudent(userService.getUserById(mark.getStudent().getId()).orElseThrow());
        preparedMark.setScore(mark.getScore());
        preparedMark.setPublished(mark.isPublished());
        preparedMark.setComments(mark.getComments());

        markService.createMark(preparedMark);
        return "redirect:/marks/list";
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public String editMarkForm(@PathVariable Long id, Model model) {
        Mark mark = markService.getMarkById(id).orElseThrow();
        model.addAttribute("mark", mark);
        model.addAttribute("exams", examService.getAllExams());
        model.addAttribute("students", userService.getUsersByRole(Role.STUDENT));
        return "marks/edit";
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public String updateMark(@PathVariable Long id, @Valid @ModelAttribute Mark mark, BindingResult result, Model model) {
        if (mark.getExam() == null || mark.getExam().getId() == null) {
            result.rejectValue("exam", "error.mark", "Please select an exam");
        }
        if (mark.getStudent() == null || mark.getStudent().getId() == null) {
            result.rejectValue("student", "error.mark", "Please select a student");
        }

        if (result.hasErrors()) {
            model.addAttribute("exams", examService.getAllExams());
            model.addAttribute("students", userService.getUsersByRole(Role.STUDENT));
            return "marks/edit";
        }
        
        try {
            Mark existingMark = markService.getMarkById(id).orElseThrow(() -> new RuntimeException("Mark not found"));

            existingMark.setExam(examService.getExamById(mark.getExam().getId()).orElseThrow());
            existingMark.setStudent(userService.getUserById(mark.getStudent().getId()).orElseThrow());
            existingMark.setScore(mark.getScore());
            existingMark.setPublished(mark.isPublished());
            existingMark.setComments(mark.getComments());

            markService.updateMark(existingMark);
            return "redirect:/marks/list";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to update mark: " + e.getMessage());
            model.addAttribute("exams", examService.getAllExams());
            model.addAttribute("students", userService.getUsersByRole(Role.STUDENT));
            return "marks/edit";
        }
    }

    @GetMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String deleteMark(@PathVariable Long id) {
        markService.deleteMark(id);
        return "redirect:/marks/list";
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT', 'PARENT', 'ADMIN')")
    public String searchMarks(@RequestParam(required = false) Long studentId, Model model) {
        try {
            if (studentId == null) {
                model.addAttribute("error", "Please enter a student ID to search.");
                model.addAttribute("marks", java.util.Collections.emptyList());
            } else {
                User student = new User();
                student.setId(studentId);
                model.addAttribute("marks", markService.getMarksByStudent(student));
            }
            return "marks/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error searching marks: " + e.getMessage());
            model.addAttribute("marks", java.util.Collections.emptyList());
            return "marks/list";
        }
    }

    @GetMapping("/search-by-subject")
    @PreAuthorize("hasRole('STUDENT')")
    public String searchMarksBySubject(@RequestParam(required = false) Long subjectId, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (subjectId == null) {
                model.addAttribute("error", "Please select a subject to search.");
                model.addAttribute("marks", java.util.Collections.emptyList());
            } else {
                User student = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
                // Get marks for the student filtered by subject
                List<Mark> studentMarks = markService.getMarksByStudent(student);
                List<Mark> filteredMarks = studentMarks.stream()
                    .filter(mark -> mark.getExam().getSubject().getId().equals(subjectId))
                    .collect(java.util.stream.Collectors.toList());
                model.addAttribute("marks", filteredMarks);
            }
            return "marks/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error searching marks by subject: " + e.getMessage());
            model.addAttribute("marks", java.util.Collections.emptyList());
            return "marks/list";
        }
    }

    @GetMapping("/search-by-student-name")
    @PreAuthorize("hasRole('PARENT')")
    public String searchMarksByStudentName(@RequestParam(required = false) String studentName, Model model) {
        try {
            if (studentName == null || studentName.trim().isEmpty()) {
                model.addAttribute("error", "Please enter a student name to search.");
                model.addAttribute("marks", java.util.Collections.emptyList());
            } else {
                // For parents, search by student name (this would need a custom service method)
                // For now, return all marks - in a real implementation, you'd filter by parent's children
                model.addAttribute("marks", markService.getAllMarks());
            }
            return "marks/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error searching marks by student name: " + e.getMessage());
            model.addAttribute("marks", java.util.Collections.emptyList());
            return "marks/list";
        }
    }

    // Teacher Marks Management - Show Exam Selection
    @GetMapping("/teacher")
    @PreAuthorize("hasRole('TEACHER')")
    public String teacherMarksManagement(@RequestParam(value = "examId", required = false) Long examId,
                                       Model model, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User teacher = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
            
            // Get exams created by this teacher
            List<Exam> teacherExams = examService.getExamsByTeacher(teacher);
            model.addAttribute("teacherExams", teacherExams);
            model.addAttribute("answerSheetMap", java.util.Collections.emptyMap());
            model.addAttribute("markMap", java.util.Collections.emptyMap());
            
            // If examId is provided, show the selected exam with students and answer sheets
            if (examId != null) {
                Exam selectedExam = examService.getExamById(examId).orElseThrow();
                
                // Security check - ensure teacher can only view their own exams
                if (!selectedExam.getTeacher().getId().equals(teacher.getId())) {
                    model.addAttribute("error", "You can only view your own exams!");
                    return "marks/teacher_marks";
                }
                
                List<AnswerSheet> answerSheets = answerSheetService.getAnswerSheetsByExam(selectedExam);
                System.out.println("Answer sheets found for exam: " + answerSheets.size());
                for (AnswerSheet sheet : answerSheets) {
                    System.out.println("Answer sheet - Student: " + sheet.getStudent().getName() + 
                                     ", Status: " + sheet.getStatus() + 
                                     ", File: " + sheet.getFilePath());
                }
                
                // Get all students in the exam's class who are enrolled in the subject
                List<User> students = userService.getUsersByRoleAndSchoolClassAndSubject(
                    Role.STUDENT, selectedExam.getSchoolClass(), selectedExam.getSubject());
                System.out.println("Students found for exam (class: " + selectedExam.getSchoolClass().getName() + 
                                 ", subject: " + selectedExam.getSubject().getName() + "): " + students.size());
                
                for (User student : students) {
                    System.out.println("Student: " + student.getName() + 
                                     ", Class: " + (student.getSchoolClass() != null ? student.getSchoolClass().getName() : "null") +
                                     ", Subjects: " + (student.getSubjects() != null ? student.getSubjects().size() : "null"));
                }
                
                // Create a map of student ID to answer sheet for quick lookup
                Map<Long, AnswerSheet> answerSheetMap = answerSheets.stream()
                    .collect(Collectors.toMap(
                        sheet -> sheet.getStudent().getId(),
                        sheet -> sheet
                    ));

                Map<Long, Mark> markMap = markService.getMarksByExam(selectedExam).stream()
                    .collect(Collectors.toMap(
                        mark -> mark.getStudent().getId(),
                        mark -> mark
                    ));
                
                model.addAttribute("selectedExam", selectedExam);
                model.addAttribute("students", students);
                model.addAttribute("answerSheetMap", answerSheetMap);
                model.addAttribute("markMap", markMap);
            }
            
            return "marks/teacher_marks";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading marks management: " + e.getMessage());
            return "marks/teacher_marks";
        }
    }

    @PostMapping("/grade-exam/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public String gradeExamFromTeacherView(@PathVariable Long id,
                                         @RequestParam Map<String, String> allParams,
                                         @AuthenticationPrincipal UserDetails userDetails,
                                         RedirectAttributes redirectAttributes) {
        try {
            Exam exam = examService.getExamById(id).orElseThrow();
            User teacher = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();

            if (!exam.getTeacher().getId().equals(teacher.getId())) {
                redirectAttributes.addFlashAttribute("error", "You can only grade your own exams!");
                return "redirect:/marks/teacher";
            }

            List<User> students = userService.getUsersByRoleAndSchoolClassAndSubject(
                Role.STUDENT, exam.getSchoolClass(), exam.getSubject());

            int gradedCount = 0;

            for (User student : students) {
                String scoreValue = allParams.get("score_" + student.getId());
                String commentsValue = allParams.get("comments_" + student.getId());
                String markIdValue = allParams.get("markId_" + student.getId());

                if (scoreValue == null || scoreValue.trim().isEmpty()) {
                    continue;
                }

                try {
                    int score = Integer.parseInt(scoreValue.trim());
                    if (score < 0 || (exam.getMaxMarks() != null && score > exam.getMaxMarks())) {
                        continue;
                    }

                    AnswerSheet answerSheet = answerSheetService
                        .getAnswerSheetByExamAndStudent(exam, student)
                        .orElse(null);

                    if (answerSheet != null) {
                        answerSheet.setStatus("GRADED");
                        answerSheet.setScore(score);
                        answerSheet.setComments(commentsValue);
                        answerSheetService.updateAnswerSheet(answerSheet);
                    }

                    Mark existingMark = null;
                    if (markIdValue != null && !markIdValue.isBlank()) {
                        try {
                            Long markId = Long.parseLong(markIdValue);
                            existingMark = markService.getMarkById(markId).orElse(null);
                        } catch (NumberFormatException ignored) {
                            existingMark = null;
                        }
                    }
                    if (existingMark == null) {
                        existingMark = markService.getMarkByExamAndStudent(exam, student).orElse(null);
                    }

                    if (existingMark != null) {
                        existingMark.setScore(score);
                        existingMark.setComments(commentsValue);
                        existingMark.setPublished(true);
                        markService.updateMark(existingMark);
                    } else {
                        Mark newMark = new Mark();
                        newMark.setExam(exam);
                        newMark.setStudent(student);
                        newMark.setScore(score);
                        newMark.setComments(commentsValue);
                        newMark.setPublished(true);
                        markService.createMark(newMark);
                    }

                    gradedCount++;
                } catch (NumberFormatException ignored) {
                    // Skip invalid numeric values while keeping other students' marks safe
                }
            }

            redirectAttributes.addFlashAttribute("success",
                "Successfully graded " + gradedCount + " student" + (gradedCount == 1 ? "" : "s"));
            return "redirect:/marks/teacher?examId=" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to grade students: " + e.getMessage());
            return "redirect:/marks/teacher?examId=" + id;
        }
    }
    
    // Helper class to hold exam with its students and answer sheets
    public static class ExamWithAnswerSheets {
        private final Exam exam;
        private final List<User> students;
        private final Map<Long, AnswerSheet> answerSheetMap;
        
        public ExamWithAnswerSheets(Exam exam, List<User> students, Map<Long, AnswerSheet> answerSheetMap) {
            this.exam = exam;
            this.students = students;
            this.answerSheetMap = answerSheetMap;
        }

        public Exam getExam() { return exam; }
        public List<User> getStudents() { return students; }
        public Map<Long, AnswerSheet> getAnswerSheetMap() { return answerSheetMap; }
    }
}

