package com.learnmate.service;

import com.learnmate.model.Attendance;
import com.learnmate.model.SchoolClass;
import com.learnmate.model.Subject;
import com.learnmate.model.Timetable;
import com.learnmate.model.User;
import com.learnmate.repository.AttendanceRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class AttendanceService {
    private final AttendanceRepository attendanceRepository;
    // private final NotificationService notificationService;

    public AttendanceService(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    public List<Attendance> getAllAttendances() {
        return attendanceRepository.findAll();
    }

    public Optional<Attendance> getAttendanceById(Long id) {
        return attendanceRepository.findById(id);
    }

    public Attendance createAttendance(Attendance attendance) {
        Attendance saved = attendanceRepository.save(attendance);
        checkLowAttendance(attendance.getStudent());
        return saved;
    }

    public Attendance updateAttendance(Attendance attendance) {
        return attendanceRepository.save(attendance);
    }

    public boolean deleteAttendance(Long id) {
        if (id == null) {
            return false;
        }

        if (!attendanceRepository.existsById(id)) {
            return false;
        }

        attendanceRepository.deleteById(id);
        return true;
    }

    public List<Attendance> getAttendancesByStudent(User student) {
        return attendanceRepository.findByStudent(student);
    }

    public List<Attendance> getAttendancesByTimetableAndDate(Timetable timetable, LocalDate date) {
        return attendanceRepository.findByTimetableAndDate(timetable, date);
    }

    public List<Attendance> getAttendancesBySchoolClassAndDate(SchoolClass schoolClass, LocalDate date) {
        return attendanceRepository.findBySchoolClassAndDate(schoolClass, date);
    }

    @Transactional
    public void deleteAttendancesBySchoolClassAndDate(SchoolClass schoolClass, LocalDate date) {
        attendanceRepository.deleteBySchoolClassAndDate(schoolClass, date);
    }

    public List<Attendance> getAttendancesByDate(LocalDate date) {
        return attendanceRepository.findByDate(date);
    }

    public List<Attendance> getAttendancesByTeacher(User teacher) {
        return attendanceRepository.findByTeacher(teacher);
    }

    public List<Attendance> getAttendancesBySchoolClassSubjectAndDate(SchoolClass schoolClass, Subject subject, LocalDate date) {
        return attendanceRepository.findBySchoolClassAndSubjectAndDate(schoolClass, subject, date);
    }

    public List<Attendance> getAttendancesForClasses(Collection<SchoolClass> classes) {
        if (classes == null || classes.isEmpty()) {
            return List.of();
        }

        Set<Long> seenIds = new LinkedHashSet<>();
        List<Attendance> combined = new ArrayList<>();

        for (SchoolClass schoolClass : classes) {
            if (schoolClass == null || schoolClass.getId() == null) {
                continue;
            }

            List<Attendance> classHistory = getClassHistory(schoolClass);
            for (Attendance attendance : classHistory) {
                Long attendanceId = attendance.getId();
                if (attendanceId == null || seenIds.add(attendanceId)) {
                    combined.add(attendance);
                }
            }
        }

        return combined;
    }

    public List<Attendance> getClassHistory(SchoolClass schoolClass) {
        if (schoolClass == null || schoolClass.getId() == null) {
            return List.of();
        }

        return attendanceRepository.findClassHistory(schoolClass);
    }

    @Transactional
    public boolean updateAttendanceStatus(Long id, boolean present, String notes) {
        if (id == null) {
            return false;
        }

        return attendanceRepository.findById(id).map(attendance -> {
            attendance.setPresent(present);
            attendance.setNotes(notes != null && !notes.isBlank() ? notes.trim() : null);
            attendanceRepository.save(attendance);
            return true;
        }).orElse(false);
    }

    @Transactional
    public void deleteAttendancesBySchoolClassSubjectAndDate(SchoolClass schoolClass, Subject subject, LocalDate date) {
        attendanceRepository.deleteBySchoolClassAndSubjectAndDate(schoolClass, subject, date);
    }

    private void checkLowAttendance(User student) {
        List<Attendance> attendances = getAttendancesByStudent(student);
        long absentCount = attendances.stream().filter(a -> !a.isPresent()).count();
        if (absentCount > 5) { // Example threshold
            // notificationService.createAlert("Low attendance for " + student.getName(), "ADMIN");
        }
    }
}

