package com.learnmate.controller;

import com.learnmate.model.Role;
import com.learnmate.model.SchoolClass;
import com.learnmate.service.SchoolClassService;
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

@Controller
@RequestMapping("/classes")
public class ClassController extends BaseController {
    private final SchoolClassService schoolClassService;
    private final UserService userService;
    private final SubjectService subjectService;

    public ClassController(SchoolClassService schoolClassService, UserService userService, SubjectService subjectService) {
        this.schoolClassService = schoolClassService;
        this.userService = userService;
        this.subjectService = subjectService;
    }

    @GetMapping("/list")
    public String listClasses(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        // Redirect to attendance mark page since that's where teachers select classes
        return "redirect:/attendances/mark";
    }

    @GetMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public String createClassForm(Model model) {
        model.addAttribute("schoolClass", new SchoolClass());
        model.addAttribute("teachers", userService.getUsersByRole(Role.TEACHER));
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("students", userService.getUsersByRole(Role.STUDENT));
        return "classes/create";
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public String createClass(@Valid @ModelAttribute SchoolClass schoolClass, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("teachers", userService.getUsersByRole(Role.TEACHER));
            model.addAttribute("subjects", subjectService.getAllSubjects());
            model.addAttribute("students", userService.getUsersByRole(Role.STUDENT));
            return "classes/create";
        }
        schoolClassService.createSchoolClass(schoolClass);
        return "redirect:/classes/list";
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String editClassForm(@PathVariable Long id, Model model) {
        SchoolClass schoolClass = schoolClassService.getSchoolClassById(id).orElseThrow();
        model.addAttribute("schoolClass", schoolClass);
        model.addAttribute("teachers", userService.getUsersByRole(Role.TEACHER));
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("students", userService.getUsersByRole(Role.STUDENT));
        return "classes/edit";
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateClass(@PathVariable Long id, @Valid @ModelAttribute SchoolClass schoolClass, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("teachers", userService.getUsersByRole(Role.TEACHER));
            model.addAttribute("subjects", subjectService.getAllSubjects());
            model.addAttribute("students", userService.getUsersByRole(Role.STUDENT));
            return "classes/edit";
        }
        schoolClass.setId(id);
        schoolClassService.updateSchoolClass(schoolClass);
        return "redirect:/classes/list";
    }

    @GetMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteClass(@PathVariable Long id) {
        schoolClassService.deleteSchoolClass(id);
        return "redirect:/classes/list";
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public String searchClasses(@RequestParam String name, Model model) {
        model.addAttribute("classes", schoolClassService.searchSchoolClassesByName(name));
        return "classes/list";
    }
    
    @GetMapping("/cleanup-duplicates")
    @PreAuthorize("hasRole('ADMIN')")
    public String cleanupDuplicates(RedirectAttributes redirectAttributes) {
        try {
            schoolClassService.removeDuplicateClasses();
            redirectAttributes.addFlashAttribute("success", "Duplicate classes cleaned up successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error cleaning up duplicates: " + e.getMessage());
        }
        return "redirect:/attendances/mark";
    }
}

