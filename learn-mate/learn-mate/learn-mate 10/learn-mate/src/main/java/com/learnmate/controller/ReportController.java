package com.learnmate.controller;

import com.learnmate.service.ReportService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/reports")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String reports() {
        return "reports";
    }

    @GetMapping("/attendance/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> attendancePdf() {
        byte[] pdf = reportService.generateAttendanceReportPDF();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendance.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/attendance/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> attendanceCsv() {
        String csv = reportService.generateAttendanceReportCSV();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendance.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv);
    }

    @GetMapping("/marks/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> marksPdf() {
        byte[] pdf = reportService.generateMarksReportPDF();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=marks.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/marks/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> marksCsv() {
        String csv = reportService.generateMarksReportCSV();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=marks.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv);
    }

    @GetMapping("/fees/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> feesPdf() {
        byte[] pdf = reportService.generateFeesReportPDF();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fees.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/fees/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> feesCsv() {
        String csv = reportService.generateFeesReportCSV();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fees.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv);
    }
}


