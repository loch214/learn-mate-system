package com.learnmate.controller;

import com.learnmate.model.Fee;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/fees")
public class FeeController {
    private final FeeService feeService;
    private final UserService userService;
    private final SubjectService subjectService;
    private final SchoolClassService schoolClassService;

    public FeeController(FeeService feeService, UserService userService, SubjectService subjectService, SchoolClassService schoolClassService) {
        this.feeService = feeService;
        this.userService = userService;
        this.subjectService = subjectService;
        this.schoolClassService = schoolClassService;
    }

    @GetMapping("/list")
    public String listFees(Model model, @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        if (userDetails != null) {
            User current = userService.getUserByUsername(userDetails.getUsername()).orElse(null);
            if (current != null && current.getRole() == Role.PARENT) {
                // For parents, show only their children's fees
                java.util.Set<User> children = current.getChildren();
                java.util.List<Fee> fees = new java.util.ArrayList<>();
                if (children != null) {
                    for (User child : children) {
                        fees.addAll(feeService.getFeesByStudentAndGrade(child));
                    }
                }
                model.addAttribute("fees", fees);
                return "fees/list";
            }
        }
        model.addAttribute("fees", feeService.getAllFees());
        return "fees/list";
    }

    @GetMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public String createFeeForm(Model model) {
        model.addAttribute("fee", new Fee());
        model.addAttribute("students", userService.getUsersByRole(Role.STUDENT));
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
        return "fees/create";
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public String createFee(@Valid @ModelAttribute Fee fee,
                            BindingResult result,
                            @RequestParam Long studentId,
                            @RequestParam Long subjectId,
                            @RequestParam Long schoolClassId,
                            Model model) {
        try {
            // Resolve associations explicitly to avoid binder conversion issues
            userService.getUserById(studentId).ifPresent(fee::setStudent);
            subjectService.getSubjectById(subjectId).ifPresent(fee::setSubject);
            schoolClassService.getSchoolClassById(schoolClassId).ifPresent(fee::setSchoolClass);
        } catch (Exception ignored) {}

        if (result.hasErrors() || fee.getStudent() == null || fee.getSubject() == null || fee.getSchoolClass() == null) {
            if (fee.getStudent() == null) {
                model.addAttribute("error", "Student is required");
            }
            if (fee.getSubject() == null) {
                model.addAttribute("error", "Subject is required");
            }
            if (fee.getSchoolClass() == null) {
                model.addAttribute("error", "Grade/Class is required");
            }
            model.addAttribute("students", userService.getUsersByRole(Role.STUDENT));
            model.addAttribute("subjects", subjectService.getAllSubjects());
            model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
            return "fees/create";
        }
        feeService.createFee(fee);
        return "redirect:/fees/list";
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String editFeeForm(@PathVariable Long id, Model model) {
        Fee fee = feeService.getFeeById(id).orElseThrow();
        model.addAttribute("fee", fee);
        model.addAttribute("students", userService.getUsersByRole(Role.STUDENT));
        model.addAttribute("subjects", subjectService.getAllSubjects());
        return "fees/edit";
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateFee(@PathVariable Long id,
                            @RequestParam Double amount,
                            @RequestParam String dueDate,
                            @RequestParam Long subjectId,
                            Model model) {
        Fee existing = feeService.getFeeById(id).orElseThrow();
        try {
            Subject subject = subjectService.getSubjectById(subjectId).orElseThrow();
            existing.setSubject(subject);
        } catch (Exception e) {
            model.addAttribute("fee", existing);
            model.addAttribute("students", userService.getUsersByRole(Role.STUDENT));
            model.addAttribute("subjects", subjectService.getAllSubjects());
            model.addAttribute("error", "Subject is required");
            return "fees/edit";
        }
        existing.setAmount(amount != null ? amount : existing.getAmount());
        try {
            existing.setDueDate(java.time.LocalDate.parse(dueDate));
        } catch (Exception e) {
            model.addAttribute("fee", existing);
            model.addAttribute("students", userService.getUsersByRole(Role.STUDENT));
            model.addAttribute("subjects", subjectService.getAllSubjects());
            model.addAttribute("error", "Invalid due date");
            return "fees/edit";
        }
        feeService.updateFee(existing);
        return "redirect:/fees/list";
    }

    @GetMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteFee(@PathVariable Long id) {
        feeService.deleteFee(id);
        return "redirect:/fees/list";
    }

    @PostMapping("/verify/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String verifyFee(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            feeService.verifyPayment(id);
            redirectAttributes.addFlashAttribute("success", "Payment verified successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to verify payment: " + e.getMessage());
        }
        return "redirect:/fees/list";
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    public String searchFees(@RequestParam Long studentId, Model model, @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        try {
            User student = userService.getUserById(studentId).orElseThrow(() -> new RuntimeException("Student not found"));
            if (userDetails != null) {
                User current = userService.getUserByUsername(userDetails.getUsername()).orElse(null);
                if (current != null && current.getRole() == Role.PARENT) {
                    // Ensure this student belongs to the parent
                    if (current.getChildren() == null || !current.getChildren().contains(student)) {
                        throw new SecurityException("You can only view your own children's fees");
                    }
                }
            }
            model.addAttribute("fees", feeService.getFeesByStudentAndGrade(student));
            model.addAttribute("searchStudentId", studentId);
            model.addAttribute("searchStudentName", student.getName());
        } catch (SecurityException se) {
            model.addAttribute("error", se.getMessage());
            model.addAttribute("fees", List.of());
        } catch (Exception e) {
            model.addAttribute("error", "Student not found with ID: " + studentId);
            model.addAttribute("fees", List.of());
        }
        return "fees/list";
    }

    @PostMapping("/parent-pay")
    @PreAuthorize("hasRole('PARENT')")
    public String parentPay(@RequestParam Long studentId,
                            @RequestParam Long subjectId,
                            @RequestParam(required = false) Double amount,
                            @RequestParam(required = false) String slipDate,
                            @RequestParam(required = false, name = "slip") org.springframework.web.multipart.MultipartFile slip,
                            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        try {
            User parent = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
            User student = userService.getUserById(studentId).orElseThrow();
            if (parent.getChildren() == null || !parent.getChildren().contains(student)) {
                throw new SecurityException("You can only pay fees for your own children");
            }
            Subject subject = subjectService.getSubjectById(subjectId).orElseThrow();

            if (amount == null || amount <= 0) {
                redirectAttributes.addFlashAttribute("error", "Payment amount is required and must be greater than 0.");
                return "redirect:/fees/list";
            }
            if (slip == null || slip.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please upload the bank slip (image or PDF).");
                return "redirect:/fees/list";
            }
            if (slip.getContentType() == null || !(slip.getContentType().startsWith("image/") || slip.getContentType().equals("application/pdf"))) {
                redirectAttributes.addFlashAttribute("error", "Only image or PDF slips are accepted.");
                return "redirect:/fees/list";
            }

            java.time.LocalDate parsedSlipDate = null;
            if (slipDate != null && !slipDate.isBlank()) {
                parsedSlipDate = java.time.LocalDate.parse(slipDate);
            } else {
                redirectAttributes.addFlashAttribute("error", "Slip date is required.");
                return "redirect:/fees/list";
            }

            var pendingOpt = feeService.getFeeByStudentAndSubjectAndStatus(student, subject, "PENDING");
            if (pendingOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No pending fee found for this subject.");
                return "redirect:/fees/list";
            }

            double expectedAmount = pendingOpt.get().getAmount();
            if (Math.abs(expectedAmount - amount) > 0.009) {
                redirectAttributes.addFlashAttribute("error", "Entered amount does not match the required fee ($" + expectedAmount + ").");
                return "redirect:/fees/list";
            }

            feeService.paySubjectFee(student, subject, amount, parsedSlipDate, slip);
            redirectAttributes.addFlashAttribute("success", "Payment submitted for verification.");
        } catch (SecurityException se) {
            redirectAttributes.addFlashAttribute("error", se.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to submit payment: " + e.getMessage());
        }
        return "redirect:/fees/list";
    }

    @GetMapping("/create-subject-fee")
    @PreAuthorize("hasRole('ADMIN')")
    public String createSubjectFeeForm(Model model) {
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
        return "fees/create-subject-fee";
    }

    @PostMapping("/create-subject-fee")
    @PreAuthorize("hasRole('ADMIN')")
    public String createSubjectFee(@RequestParam Long subjectId,
                                   @RequestParam Long schoolClassId,
                                   @RequestParam Double amount,
                                   @RequestParam String dueDate,
                                   RedirectAttributes redirectAttributes) {
        try {
            Subject subject = subjectService.getSubjectById(subjectId).orElseThrow();
            SchoolClass schoolClass = schoolClassService.getSchoolClassById(schoolClassId).orElseThrow();
            List<User> students = userService.getUsersByRole(Role.STUDENT);

            int createdCount = 0;
            for (User student : students) {
                if (student.getSubjects() != null && student.getSubjects().contains(subject)
                        && student.getSchoolClass() != null && student.getSchoolClass().equals(schoolClass)) {
                    feeService.createSubjectFee(student, subject, schoolClass, amount, java.time.LocalDate.parse(dueDate));
                    createdCount++;
                }
            }

            redirectAttributes.addFlashAttribute("success",
                    "Created fees for " + createdCount + " students in " + schoolClass.getName() + " enrolled in " + subject.getName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create subject fees: " + e.getMessage());
        }

        return "redirect:/fees/create-subject-fee";
    }
}


