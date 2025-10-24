package com.learnmate.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class BaseController {
    
    @ModelAttribute
    public void addUserRoleToModel(Model model, Authentication authentication, HttpServletRequest request) {
        boolean isAdmin = false;
        boolean isTeacher = false;
        boolean isStudent = false;
        boolean isParent = false;

        if (authentication != null && authentication.getAuthorities() != null) {
            isAdmin = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            isTeacher = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_TEACHER".equals(a.getAuthority()));
            isStudent = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_STUDENT".equals(a.getAuthority()));
            isParent = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_PARENT".equals(a.getAuthority()));

            // Prefer a deterministic current role label: prioritize Admin, Teacher, Student, Parent
            String roleName = isAdmin ? "ADMIN" : (isTeacher ? "TEACHER" : (isStudent ? "STUDENT" : (isParent ? "PARENT" : "")));
            if (!roleName.isEmpty()) {
                model.addAttribute("currentUserRole", roleName);
            }
        }

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isTeacher", isTeacher);
        model.addAttribute("isStudent", isStudent);
        model.addAttribute("isParent", isParent);

        model.addAttribute("activePage", resolveActivePage(request != null ? request.getRequestURI() : null));
        }
    
    protected boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    private String resolveActivePage(String uri) {
        if (uri == null) {
            return "dashboard";
        }

        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("/users", "users");
        mapping.put("/subjects", "subjects");
        mapping.put("/classes", "classes");
        mapping.put("/timetables", "timetables");
        mapping.put("/attendances", "attendances");
        mapping.put("/exams", "exams");
        mapping.put("/marks", "marks");
        mapping.put("/materials", "materials");
        mapping.put("/fees", "fees");
        mapping.put("/notifications", "notifications");
        mapping.put("/reports", "reports");
        mapping.put("/profile", "profile");

        return mapping.entrySet().stream()
                .filter(entry -> uri.startsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("dashboard");
    }
}
