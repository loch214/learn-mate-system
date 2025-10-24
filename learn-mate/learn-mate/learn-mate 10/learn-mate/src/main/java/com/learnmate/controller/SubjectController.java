package com.learnmate.controller;

import com.learnmate.model.Subject;
import com.learnmate.service.SubjectService;
import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/subjects")
public class SubjectController {
    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @GetMapping("/list")
    public String listSubjects(Model model) {
        model.addAttribute("subjects", subjectService.getAllSubjects());
        return "subjects/list";
    }

    @GetMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public String createSubjectForm(Model model) {
        model.addAttribute("subject", new Subject());
        return "subjects/create";
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public String createSubject(@Valid @ModelAttribute Subject subject, BindingResult result) {
        if (result.hasErrors()) {
            return "subjects/create";
        }
        subjectService.createSubject(subject);
        return "redirect:/subjects/list";
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String editSubjectForm(@PathVariable Long id, Model model) {
        Subject subject = subjectService.getSubjectById(id).orElseThrow();
        model.addAttribute("subject", subject);
        return "subjects/edit";
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateSubject(@PathVariable Long id, @Valid @ModelAttribute Subject subject, BindingResult result) {
        if (result.hasErrors()) {
            return "subjects/edit";
        }
        subject.setId(id);
        subjectService.updateSubject(subject);
        return "redirect:/subjects/list";
    }

    @GetMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteSubject(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return "redirect:/subjects/list";
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public String searchSubjects(@RequestParam String name, Model model) {
        model.addAttribute("subjects", subjectService.getSubjectsByName(name));
        return "subjects/list";
    }
}


