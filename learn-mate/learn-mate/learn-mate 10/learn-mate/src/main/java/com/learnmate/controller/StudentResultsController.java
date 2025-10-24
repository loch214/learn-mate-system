package com.learnmate.controller;

import com.learnmate.model.Mark;
import com.learnmate.model.User;
import com.learnmate.service.MarkService;
import com.learnmate.service.UserService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student-results")
public class StudentResultsController extends BaseController {
    private final MarkService markService;
    private final UserService userService;

    public StudentResultsController(MarkService markService, UserService userService) {
        this.markService = markService;
        this.userService = userService;
    }

    @GetMapping
    public String viewStudentResults(@AuthenticationPrincipal UserDetails userDetails, 
                                   @RequestParam(value = "subjectId", required = false) Long subjectId,
                                   Model model) {
        try {
            User student = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
            
            // Get all marks for the student
            List<Mark> allMarks = markService.getMarksByStudent(student);
            
            // Filter by subject if specified
            if (subjectId != null) {
                allMarks = allMarks.stream()
                    .filter(mark -> mark.getExam().getSubject().getId().equals(subjectId))
                    .collect(Collectors.toList());
            }
            
            // Group marks by subject for better organization
            Map<String, List<Mark>> marksBySubject = allMarks.stream()
                .collect(Collectors.groupingBy(mark -> mark.getExam().getSubject().getName()));
            
            // Calculate statistics
            double averageScore = allMarks.stream()
                .mapToInt(Mark::getScore)
                .average()
                .orElse(0.0);
            
            long totalExams = allMarks.size();
            long passedExams = allMarks.stream()
                .filter(mark -> mark.getScore() >= mark.getExam().getPassMark())
                .count();
            
            model.addAttribute("student", student);
            model.addAttribute("marksBySubject", marksBySubject);
            model.addAttribute("allMarks", allMarks);
            model.addAttribute("averageScore", averageScore);
            model.addAttribute("totalExams", totalExams);
            model.addAttribute("passedExams", passedExams);
            model.addAttribute("selectedSubjectId", subjectId);
            
            return "student/results";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error loading results: " + e.getMessage());
            return "student/results";
        }
    }
}


