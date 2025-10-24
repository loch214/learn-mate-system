package com.learnmate.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/teacher")
public class TeacherController {

    // Teacher dashboard shortcuts routing to existing modules

    @GetMapping("/timetables")
    @PreAuthorize("hasRole('TEACHER')")
    public String timetables(Model model) {
        return "redirect:/timetables/list";
    }

    @GetMapping("/attendance/new")
    @PreAuthorize("hasRole('TEACHER')")
    public String createAttendance(Model model) {
        return "redirect:/attendances/create";
    }

    @GetMapping("/marks/new")
    @PreAuthorize("hasRole('TEACHER')")
    public String enterMarks(Model model) {
        return "redirect:/marks/create";
    }

    @GetMapping("/materials")
    @PreAuthorize("hasRole('TEACHER')")
    public String materials(Model model) {
        return "redirect:/materials/list";
    }

    @GetMapping("/notifications/new")
    @PreAuthorize("hasRole('TEACHER')")
    public String createNotification(Model model) {
        return "redirect:/notifications/create";
    }
}
