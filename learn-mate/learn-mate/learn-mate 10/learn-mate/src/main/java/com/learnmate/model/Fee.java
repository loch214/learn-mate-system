package com.learnmate.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "fees")
public class Fee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User student;

    @ManyToOne
    private Subject subject;

    @ManyToOne
    private SchoolClass schoolClass;

    private double amount;

    private LocalDate dueDate;

    private String status; // PAID, PENDING, OVERDUE

    private LocalDate paymentDate;

    // Optional details provided by student when submitting bank slip
    private Double submittedAmount;

    private LocalDate submittedDate; // date on bank slip

    private String paymentSlipPath; // stored filename for the slip image

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public Double getSubmittedAmount() { return submittedAmount; }
    public void setSubmittedAmount(Double submittedAmount) { this.submittedAmount = submittedAmount; }

    public LocalDate getSubmittedDate() { return submittedDate; }
    public void setSubmittedDate(LocalDate submittedDate) { this.submittedDate = submittedDate; }

    public String getPaymentSlipPath() { return paymentSlipPath; }
    public void setPaymentSlipPath(String paymentSlipPath) { this.paymentSlipPath = paymentSlipPath; }

    public SchoolClass getSchoolClass() { return schoolClass; }
    public void setSchoolClass(SchoolClass schoolClass) { this.schoolClass = schoolClass; }
}
