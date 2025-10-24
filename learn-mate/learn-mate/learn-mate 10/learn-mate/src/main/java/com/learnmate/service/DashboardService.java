package com.learnmate.service;

import com.learnmate.model.*;
import com.learnmate.repository.AttendanceRepository;
import com.learnmate.repository.ExamRepository;
import com.learnmate.repository.NotificationRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {
    private final UserService userService;
    private final AttendanceRepository attendanceRepository;
    private final ExamRepository examRepository;
    private final NotificationRepository notificationRepository;

    public DashboardService(UserService userService, 
                          AttendanceRepository attendanceRepository,
                          ExamRepository examRepository,
                          NotificationRepository notificationRepository) {
        this.userService = userService;
        this.attendanceRepository = attendanceRepository;
        this.examRepository = examRepository;
        this.notificationRepository = notificationRepository;
    }

    public Map<String, Object> getAdminDashboard() {
        Map<String, Object> data = new HashMap<>();
        try {
            data.put("totalStudents", userService.getUsersByRole(Role.STUDENT).size());
        } catch (Exception e) {
            data.put("totalStudents", 0);
        }
        data.put("attendanceTrends", calculateAttendanceTrends());
        data.put("feeStatus", calculateFeeStatus());
        return data;
    }

    public Map<String, Object> getTeacherDashboard(User teacher) {
        Map<String, Object> data = new HashMap<>();
        data.put("teacherName", teacher.getName());
        data.put("totalClasses", 3); // Placeholder
        return data;
    }

    public Map<String, Object> getStudentDashboard(User student) {
        Map<String, Object> data = new HashMap<>();
        
        // Calculate attendance rate
        double attendanceRate = calculateAttendanceRate(student);
        data.put("attendanceRate", String.format("%.1f%%", attendanceRate));
        
        // Calculate upcoming exams count
        int upcomingExamsCount = calculateUpcomingExamsCount(student);
        data.put("upcomingExams", upcomingExamsCount);
        
        // Get latest notification
        String lastNotification = getLastNotification(student);
        data.put("lastNotification", lastNotification);
        
        return data;
    }

    public Map<String, Object> getParentDashboard(User parent) {
        Map<String, Object> data = new HashMap<>();
        
        // For parent dashboard, we need to get their child's data
        // This is a simplified version - in reality, you'd need to get the child user
        data.put("attendanceRate", "95%"); // Placeholder
        data.put("upcomingExams", 3); // Placeholder
        data.put("lastNotification", "No recent notifications"); // Placeholder
        
        return data;
    }

    private double calculateAttendanceRate(User student) {
        List<Attendance> attendances = attendanceRepository.findByStudent(student);
        
        if (attendances.isEmpty()) {
            return 0.0;
        }
        
        long presentDays = attendances.stream()
            .filter(Attendance::isPresent)
            .count();
        
        long totalDays = attendances.size();
        
        return totalDays == 0 ? 0.0 : (presentDays * 100.0) / totalDays;
    }

    private int calculateUpcomingExamsCount(User student) {
        if (student.getSchoolClass() == null) {
            return 0;
        }
        
        List<Exam> allExams = examRepository.findBySchoolClass(student.getSchoolClass());
        LocalDate today = LocalDate.now();
        
        return (int) allExams.stream()
            .filter(exam -> exam.getDate() != null && exam.getDate().isAfter(today))
            .count();
    }

    private String getLastNotification(User user) {
        List<Notification> notifications = notificationRepository.findByTargetUserOrderByCreatedAtDesc(user);
        
        if (notifications.isEmpty()) {
            return "No recent notifications";
        }
        
        Notification lastNotification = notifications.get(0);
        return lastNotification.getTitle() != null ? lastNotification.getTitle() : "New notification";
    }

    private List<String> calculateAttendanceTrends() {
        // Placeholder data for now
        return List.of("85% attendance this week", "90% attendance last week", "Trend: Improving");
    }

    private Map<String, Integer> calculateFeeStatus() {
        // Placeholder data for now
        return Map.of("Paid", 75, "Pending", 25, "Overdue", 5);
    }
}

