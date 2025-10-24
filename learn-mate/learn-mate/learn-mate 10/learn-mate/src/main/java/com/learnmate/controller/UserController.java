package com.learnmate.controller;

import com.learnmate.model.Role;
import com.learnmate.model.SchoolClass;
import com.learnmate.model.Subject;
import com.learnmate.model.User;
import com.learnmate.service.FeeService;
import com.learnmate.service.SchoolClassService;
import com.learnmate.service.SubjectService;
import com.learnmate.service.UserService;
import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final SchoolClassService schoolClassService;
    private final SubjectService subjectService;
    private final FeeService feeService;

    public UserController(UserService userService, SchoolClassService schoolClassService, SubjectService subjectService, FeeService feeService) {
        this.userService = userService;
        this.schoolClassService = schoolClassService;
        this.subjectService = subjectService;
        this.feeService = feeService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "users/list";
    }

    @GetMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public String createUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", Role.values());
        return "users/create";
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public String createUser(@Valid @ModelAttribute User user, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("roles", Role.values());
            return "users/create";
        }
        if (userService.existsByUsername(user.getUsername())) {
            model.addAttribute("error", "Username already exists!");
            model.addAttribute("roles", Role.values());
            return "users/create";
        }
        try {
            userService.save(user);
        } catch (Exception e) {
            model.addAttribute("error", "Failed to register user: " + e.getMessage());
            model.addAttribute("roles", Role.values());
            return "users/create";
        }
        return "redirect:/users/list";
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id).orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", user);
        model.addAttribute("roles", Role.values());
        return "users/edit";
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateUser(@PathVariable Long id, @Valid @ModelAttribute User user, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("roles", Role.values());
            return "users/edit";
        }
        user.setId(id);
        if (userService.existsByUsername(user.getUsername()) && !userService.getUserByUsername(user.getUsername()).get().getId().equals(id)) {
            model.addAttribute("error", "Username already exists!");
            model.addAttribute("roles", Role.values());
            return "users/edit";
        }
        try {
            userService.save(user);
        } catch (Exception e) {
            model.addAttribute("error", "Failed to update user: " + e.getMessage());
            model.addAttribute("roles", Role.values());
            return "users/edit";
        }
        return "redirect:/users/list";
    }

    @GetMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "redirect:/users/list";
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public String profile(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        model.addAttribute("user", user);
        
        // Add fee information for students
        if (user.getRole() == Role.STUDENT && user.getSubjects() != null) {
            model.addAttribute("subjectFees", feeService.getFeesByStudent(user));
        }
        // Add children and their fees for parents
        if (user.getRole() == Role.PARENT) {
            java.util.Set<User> children = user.getChildren();
            model.addAttribute("children", children);
            java.util.Map<Long, java.util.List<com.learnmate.model.Fee>> childFees = new java.util.HashMap<>();
            if (children != null) {
                for (User child : children) {
                    childFees.put(child.getId(), feeService.getFeesByStudent(child));
                }
            }
            model.addAttribute("childFees", childFees);
        }
        
        return "users/profile";
    }

    @GetMapping("/profile/edit")
    @PreAuthorize("isAuthenticated()")
    public String editProfile(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("DEBUG: Profile edit requested by: " + userDetails.getUsername());
            
            // Fetch the complete, existing user entity from the database (WITH its ID)
            User user = userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            System.out.println("DEBUG: User found: " + user.getName() + ", Role: " + user.getRole());
            System.out.println("DEBUG: User ID: " + user.getId());
            
            // Add the complete user object (WITH its ID) to the model
            model.addAttribute("user", user);
            return "profile/edit_form";
        } catch (Exception e) {
            System.out.println("DEBUG: Error in editProfile: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "An error occurred while loading your profile: " + e.getMessage());
            return "profile/edit_form";
        }
    }

    @PostMapping("/profile/edit")
    @PreAuthorize("isAuthenticated()")
    public String updateProfile(@Valid @ModelAttribute("user") User user, BindingResult result, 
                               @AuthenticationPrincipal UserDetails userDetails, 
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "profile/edit_form";
        }
        
        // Security check - ensure user can only edit their own profile
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        if (!currentUser.getId().equals(user.getId())) {
            throw new SecurityException("Users can only edit their own profile");
        }
        
        try {
            // Pass the complete user object (with its ID from hidden field) to the service
            userService.updateUserProfile(user);
            
            // Add success message for user feedback
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update profile: " + e.getMessage());
        }
        
        // Redirect to the user's profile view to show the changes
        return "redirect:/users/profile";
    }

    @GetMapping("/profile/change-password")
    @PreAuthorize("isAuthenticated()")
    public String changePasswordForm() {
        return "users/change-password";
    }

    @PostMapping("/profile/change-password")
    @PreAuthorize("isAuthenticated()")
    public String changePassword(@RequestParam String currentPassword, 
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Model model) {
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match!");
            return "users/change-password";
        }
        
        User user = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        if (!userService.checkPassword(currentPassword, user.getPassword())) {
            model.addAttribute("error", "Current password is incorrect!");
            return "users/change-password";
        }
        
        try {
            userService.changePassword(user, newPassword);
            model.addAttribute("success", "Password changed successfully!");
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "users/change-password";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", new Role[]{Role.STUDENT, Role.TEACHER, Role.PARENT});
        model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("students", userService.getAvailableStudentsForParent());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute User user, BindingResult result, 
                             @RequestParam(value = "schoolClassId", required = false) Long schoolClassId,
                             @RequestParam(value = "subjectIds", required = false) List<Long> subjectIds,
                             @RequestParam(value = "teacherSubjectIds", required = false) List<Long> teacherSubjectIds,
                             @RequestParam(value = "childIds", required = false) List<Long> childIds,
                             Model model, RedirectAttributes redirectAttributes) {
        
        // Handle validation errors
        if (result.hasErrors()) {
            model.addAttribute("roles", new Role[]{Role.STUDENT, Role.TEACHER, Role.PARENT});
            model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
            model.addAttribute("subjects", subjectService.getAllSubjects());
            model.addAttribute("students", userService.getAvailableStudentsForParent());
            return "register";
        }
        
        // Custom validation for role-specific requirements
        if (user.getRole() == Role.STUDENT && (subjectIds == null || subjectIds.isEmpty())) {
            result.rejectValue("subjects", "error.subjects", "Students must select at least one subject.");
            model.addAttribute("roles", new Role[]{Role.STUDENT, Role.TEACHER, Role.PARENT});
            model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
            model.addAttribute("subjects", subjectService.getAllSubjects());
            model.addAttribute("students", userService.getAvailableStudentsForParent());
            return "register";
        }
        
        if (user.getRole() == Role.TEACHER && (teacherSubjectIds == null || teacherSubjectIds.isEmpty())) {
            result.rejectValue("subjects", "error.subjects", "Teachers must select at least one subject to teach.");
            model.addAttribute("roles", new Role[]{Role.STUDENT, Role.TEACHER, Role.PARENT});
            model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
            model.addAttribute("subjects", subjectService.getAllSubjects());
            model.addAttribute("students", userService.getAvailableStudentsForParent());
            return "register";
        }
        
        // Handle SchoolClass assignment for students
        if (user.getRole() == Role.STUDENT && schoolClassId != null) {
            SchoolClass schoolClass = schoolClassService.getSchoolClassById(schoolClassId).orElse(null);
            user.setSchoolClass(schoolClass);
        }
        
        // Handle Subject enrollment for students
        if (user.getRole() == Role.STUDENT && subjectIds != null && !subjectIds.isEmpty()) {
            Set<Subject> subjects = new HashSet<>();
            for (Long subjectId : subjectIds) {
                subjectService.getSubjectById(subjectId).ifPresent(subjects::add);
            }
            user.setSubjects(subjects);
        }
        
        // Handle Subject assignment for teachers
        if (user.getRole() == Role.TEACHER && teacherSubjectIds != null && !teacherSubjectIds.isEmpty()) {
            Set<Subject> subjects = new HashSet<>();
            for (Long subjectId : teacherSubjectIds) {
                subjectService.getSubjectById(subjectId).ifPresent(subjects::add);
            }
            user.setSubjects(subjects);
        }
        
        try {
            // Use the new robust registration method with proactive validation
            userService.registerNewUser(user);
            if (user.getRole() == Role.PARENT) {
                // Parent must have at least one registered child
                userService.attachChildrenToParent(user, childIds);
            }
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please log in.");
            return "redirect:/login";
        } catch (IllegalStateException e) {
            // This catches the "already exists" errors from our service
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/users/register";
        } catch (Exception e) {
            // Catch any other unexpected errors
            redirectAttributes.addFlashAttribute("errorMessage", "Registration failed: " + e.getMessage());
            return "redirect:/users/register";
        }
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public String searchUsers(@RequestParam String name, Model model) {
        model.addAttribute("users", userService.getUsersByName(name));
        return "users/list";
    }

    @PostMapping("/profile/pay-fee")
    @PreAuthorize("hasRole('STUDENT')")
    public String paySubjectFee(@RequestParam Long subjectId,
                               @RequestParam(required = false) Double amount,
                               @RequestParam(required = false) String slipDate,
                               @RequestParam(required = false, name = "slip") org.springframework.web.multipart.MultipartFile slip,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            User student = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
            Subject subject = subjectService.getSubjectById(subjectId).orElseThrow();
            
            // Check if student is enrolled in this subject
            if (!student.getSubjects().contains(subject)) {
                redirectAttributes.addFlashAttribute("error", "You are not enrolled in this subject.");
                return "redirect:/users/profile";
            }
            
            // Check if there's a pending fee for this subject
            var pendingOpt = feeService.getFeeByStudentAndSubjectAndStatus(student, subject, "PENDING");
            if (pendingOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No pending fee found for this subject.");
                return "redirect:/users/profile";
            }

            // Basic validations: amount, date, and slip required
            if (amount == null || amount <= 0) {
                redirectAttributes.addFlashAttribute("error", "Payment amount is required and must be greater than 0.");
                return "redirect:/users/profile";
            }
            if (slip == null || slip.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please upload the bank slip (image or PDF).");
                return "redirect:/users/profile";
            }
            if (slip.getContentType() == null || !(slip.getContentType().startsWith("image/") || slip.getContentType().equals("application/pdf"))) {
                redirectAttributes.addFlashAttribute("error", "Only image or PDF slips are accepted.");
                return "redirect:/users/profile";
            }

            java.time.LocalDate parsedSlipDate = null;
            if (slipDate != null && !slipDate.isBlank()) {
                parsedSlipDate = java.time.LocalDate.parse(slipDate);
            } else {
                redirectAttributes.addFlashAttribute("error", "Slip date is required.");
                return "redirect:/users/profile";
            }

            // Cross-check amount against expected fee
            double expectedAmount = pendingOpt.get().getAmount();
            if (Math.abs(expectedAmount - amount) > 0.009) {
                redirectAttributes.addFlashAttribute("error", "Entered amount does not match the required fee ($" + expectedAmount + ").");
                return "redirect:/users/profile";
            }

            feeService.paySubjectFee(student, subject, amount, parsedSlipDate, slip);
            redirectAttributes.addFlashAttribute("success", "Fee paid successfully for " + subject.getName() + "!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to pay fee: " + e.getMessage());
        }
        
        return "redirect:/users/profile";
    }
}


