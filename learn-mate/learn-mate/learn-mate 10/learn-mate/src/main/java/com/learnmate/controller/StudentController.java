package com.learnmate.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/student")
public class StudentController {

    // Student-facing shortcuts

    @GetMapping("/materials")
    @PreAuthorize("hasRole('STUDENT')")
    public String materials(Model model) {
        // Reuse existing Materials module list view
        return "redirect:/materials/list";
    }

    // ...existing code...
}
