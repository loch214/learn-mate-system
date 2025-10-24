package com.learnmate.controller;

import com.learnmate.model.User;
import com.learnmate.service.ReportService;
import com.learnmate.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/parent/reports")
public class ParentReportController extends BaseController {

    private final ReportService reportService;
    private final UserService userService;

    public ParentReportController(ReportService reportService, UserService userService) {
        this.reportService = reportService;
        this.userService = userService;
    }

    @GetMapping("/marks/pdf")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<byte[]> downloadMarksPdf(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Unable to resolve authenticated parent user");
        }

        String username = authentication.getName();
        User parent = userService.getParentWithChildrenByUsername(username)
                .orElseGet(() -> userService.getUserByUsername(username).orElseThrow());

        byte[] pdf = reportService.generateParentMarksReportPDF(parent);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=academic-report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
