package com.learnmate.controller;

import com.learnmate.model.User;
import com.learnmate.service.DashboardService;
import com.learnmate.service.NotificationService;
import com.learnmate.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collection;
import java.util.Map;

/**
 * Central Dashboard Controller for role-based routing
 * Routes users to appropriate dashboards based on their role
 */
@Controller
@RequestMapping("/dashboard")
public class DashboardController extends BaseController {
    
    private final DashboardService dashboardService;
    private final UserService userService;
    private final NotificationService notificationService;

    public DashboardController(DashboardService dashboardService, UserService userService, NotificationService notificationService) {
        this.dashboardService = dashboardService;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    /**
     * Main dashboard routing - redirects to role-specific dashboard
     */
    @GetMapping("")
    public String dashboard(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        
        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();
            
            switch (role) {
                case "ROLE_ADMIN":
                    return "redirect:/dashboard/admin";
                case "ROLE_TEACHER":
                    return "redirect:/dashboard/teacher";
                case "ROLE_STUDENT":
                    return "redirect:/dashboard/student";
                case "ROLE_PARENT":
                    return "redirect:/dashboard/parent";
                default:
                    // Default fallback - should never happen with proper security
                    return "redirect:/login?error=invalid_role";
            }
        }
        
        // Fallback if no valid role found
        return "redirect:/login?error=no_role";
    }

    /**
     * Admin Dashboard - Only accessible by ADMIN role
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboard(Model model, Authentication authentication) {
        User currentUser = resolveCurrentUser(authentication);
        
        // Add admin-specific data to model
        model.addAttribute("totalUsers", 150); // Mock data - replace with service calls
        model.addAttribute("activeStudents", 120);
        model.addAttribute("totalTeachers", 25);
        model.addAttribute("totalClasses", 15);
        model.addAttribute("pendingFees", 8);
        
        // Add notification statistics
        model.addAttribute("totalNotifications", notificationService.getTotalNotificationCount());
        model.addAttribute("unreadNotifications", notificationService.getUnreadNotificationCount(currentUser));
        model.addAttribute("todayNotifications", notificationService.getTodayNotificationCount());
        
        return "dashboard/admin_dashboard";
    }

    /**
     * Teacher Dashboard - Only accessible by TEACHER role
     */
    @GetMapping("/teacher")
    @PreAuthorize("hasRole('TEACHER')")
    public String teacherDashboard(Model model, Authentication authentication) {
        // Add teacher-specific data to model
        model.addAttribute("totalStudents", 45); // Mock data - replace with service calls
        model.addAttribute("todayClasses", 6);
        model.addAttribute("pendingAttendance", 3);
        model.addAttribute("upcomingExams", 2);
        model.addAttribute("ungraded", 12);
        model.addAttribute("notifications", 4);
        
        return "dashboard/teacher_dashboard_new";
    }

    /**
     * Student Dashboard - Only accessible by STUDENT role
     */
    @GetMapping("/student")
    @PreAuthorize("hasRole('STUDENT')")
    public String studentDashboard(Model model, Authentication authentication) {
        User currentUser = resolveCurrentUser(authentication);
        
        // Get real data from DashboardService
        Map<String, Object> dashboardData = dashboardService.getStudentDashboard(currentUser);
        
        // Add student-specific data to model
        model.addAttribute("todayClasses", 4); // Mock data - replace with service calls
        model.addAttribute("upcomingExams", dashboardData.get("upcomingExams"));
        model.addAttribute("attendanceRate", dashboardData.get("attendanceRate"));
        model.addAttribute("lastNotification", dashboardData.get("lastNotification"));
        model.addAttribute("pendingAssignments", 3);
        model.addAttribute("notifications", 2);
        
        return "dashboard/student_dashboard_new";
    }

    /**
     * Parent Dashboard - Only accessible by PARENT role
     */
    @GetMapping("/parent")
    @PreAuthorize("hasRole('PARENT')")
    public String parentDashboard(Model model, Authentication authentication) {
        User currentUser = resolveCurrentUser(authentication);
        
        // Get real data from DashboardService
        Map<String, Object> dashboardData = dashboardService.getParentDashboard(currentUser);
        
        // Add parent-specific data to model
        model.addAttribute("overallGrade", "A"); // Mock data - replace with service calls
        model.addAttribute("attendanceRate", dashboardData.get("attendanceRate"));
        model.addAttribute("feeStatus", "Paid");
        model.addAttribute("upcomingExams", dashboardData.get("upcomingExams"));
        model.addAttribute("classRank", "5th");
        model.addAttribute("classTeacher", "Ms. Johnson");
        model.addAttribute("lastNotification", dashboardData.get("lastNotification"));
        
        return "dashboard/parent_dashboard";
    }

    /**
     * Alternative routing method for backwards compatibility
     * Handles requests to /dashboard/default and routes based on role
     */
    @GetMapping("/default")
    public String defaultDashboard(Authentication authentication) {
        return dashboard(authentication); // Delegate to main dashboard method
    }

    private User resolveCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            throw new IllegalStateException("Unable to resolve authenticated user");
        }
        return userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
    }
}
