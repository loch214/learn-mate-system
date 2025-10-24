package com.learnmate.service;

import com.learnmate.model.Fee;
import com.learnmate.model.Subject;
import com.learnmate.model.User;
import com.learnmate.repository.FeeRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class FeeService {
    private final FeeRepository feeRepository;
    private final FileStorageService fileStorageService;
    // private final NotificationService notificationService;

    public FeeService(FeeRepository feeRepository, FileStorageService fileStorageService) {
        this.feeRepository = feeRepository;
        this.fileStorageService = fileStorageService;
    }

    public List<Fee> getAllFees() {
        return feeRepository.findAll();
    }

    public List<Fee> getFeesByStudent(User student) {
        return feeRepository.findByStudent(student);
    }

    public List<Fee> getFeesByStudentAndGrade(User student) {
        if (student.getSchoolClass() == null) {
            return List.of(); // Return empty list if student has no grade
        }
        return feeRepository.findByStudentAndSchoolClass(student, student.getSchoolClass());
    }

    public Optional<Fee> getFeeById(Long id) {
        return feeRepository.findById(id);
    }

    public Fee saveFee(Fee fee) {
        return feeRepository.save(fee);
    }

    public void deleteFee(Long id) {
        feeRepository.deleteById(id);
    }

    public Fee updateFeeStatus(Long id, String status) {
        Fee fee = feeRepository.findById(id).orElseThrow();
        fee.setStatus(status);
        if ("PAID".equals(status)) {
            fee.setPaymentDate(LocalDate.now());
        }
        return feeRepository.save(fee);
    }

    public Fee createFee(Fee fee) {
        return feeRepository.save(fee);
    }

    public Fee updateFee(Fee fee) {
        return feeRepository.save(fee);
    }

    public List<Fee> getFeesByStudentAndSubject(User student, Subject subject) {
        return feeRepository.findByStudentAndSubject(student, subject);
    }

    public Optional<Fee> getFeeByStudentAndSubjectAndStatus(User student, Subject subject, String status) {
        return feeRepository.findByStudentAndSubjectAndStatus(student, subject, status);
    }

    public Fee createSubjectFee(User student, Subject subject, com.learnmate.model.SchoolClass schoolClass, double amount, LocalDate dueDate) {
        Fee fee = new Fee();
        fee.setStudent(student);
        fee.setSubject(subject);
        fee.setSchoolClass(schoolClass);
        fee.setAmount(amount);
        fee.setDueDate(dueDate);
        fee.setStatus("PENDING");
        return feeRepository.save(fee);
    }

    public Fee paySubjectFee(User student, Subject subject) {
        Optional<Fee> pendingFee = feeRepository.findByStudentAndSubjectAndStatus(student, subject, "PENDING");
        Fee feeToPay;
        if (pendingFee.isPresent()) {
            feeToPay = pendingFee.get();
        } else {
            List<Fee> pendingFees = feeRepository.findByStudentAndSubjectAndStatusOrderByDueDateAsc(student, subject, "PENDING");
            if (pendingFees == null || pendingFees.isEmpty()) {
                throw new RuntimeException("No pending fee found for this subject");
            }
            // If duplicates exist, choose the earliest due date to pay
            feeToPay = pendingFees.get(0);
        }

        feeToPay.setStatus("PAID_PENDING");
        feeToPay.setPaymentDate(LocalDate.now());
        return feeRepository.save(feeToPay);
    }

    public Fee paySubjectFee(User student, Subject subject, Double submittedAmount, LocalDate slipDate,
                             org.springframework.web.multipart.MultipartFile slipFile) {
        Optional<Fee> pendingFee = feeRepository.findByStudentAndSubjectAndStatus(student, subject, "PENDING");
        Fee fee;
        if (pendingFee.isPresent()) {
            fee = pendingFee.get();
        } else {
            List<Fee> pendingFees = feeRepository.findByStudentAndSubjectAndStatusOrderByDueDateAsc(student, subject, "PENDING");
            if (pendingFees == null || pendingFees.isEmpty()) {
                throw new RuntimeException("No pending fee found for this subject");
            }
            fee = pendingFees.get(0);
        }
        fee.setSubmittedAmount(submittedAmount);
        fee.setSubmittedDate(slipDate);
        if (slipFile != null && !slipFile.isEmpty()) {
            String stored = fileStorageService.storeFile(slipFile, FileStorageService.FileType.PAYMENT_SLIP);
            fee.setPaymentSlipPath(stored);
        }
        fee.setStatus("PAID_PENDING");
        fee.setPaymentDate(LocalDate.now());
        return feeRepository.save(fee);
    }

    public Fee verifyPayment(Long feeId) {
        Fee fee = feeRepository.findById(feeId).orElseThrow();
        if (!"PAID_PENDING".equals(fee.getStatus())) {
            throw new IllegalStateException("Only payments pending verification can be verified");
        }
        fee.setStatus("PAID");
        // keep the original paymentDate as submission date
        return feeRepository.save(fee);
    }
}

