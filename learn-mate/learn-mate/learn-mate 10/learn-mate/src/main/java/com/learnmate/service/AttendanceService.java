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
import java.util.stream.Collectors;

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

        Set<Long> classIds = classes.stream()
            .filter(Objects::nonNull)
            .map(SchoolClass::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (classIds.isEmpty()) {
            return List.of();
        }

        List<SchoolClass> sanitizedClasses = classes.stream()
            .filter(Objects::nonNull)
            .filter(clazz -> clazz.getId() != null)
            .distinct()
            .toList();

        List<Attendance> directMatches = sanitizedClasses.isEmpty()
            ? List.of()
            : attendanceRepository.findBySchoolClassIn(sanitizedClasses);

        Set<Long> seenAttendanceIds = directMatches.stream()
            .map(Attendance::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        List<Attendance> viaStudentClass = attendanceRepository.findAll().stream()
            .filter(attendance -> attendance.getStudent() != null && attendance.getStudent().getSchoolClass() != null)
            .filter(attendance -> {
                SchoolClass studentClass = attendance.getStudent().getSchoolClass();
                Long classId = studentClass.getId();
                return classId != null && classIds.contains(classId);
            })
            .filter(attendance -> {
                Long attendanceId = attendance.getId();
                return attendanceId == null || !seenAttendanceIds.contains(attendanceId);
            })
            .collect(Collectors.toList());

        if (viaStudentClass.isEmpty()) {
            return directMatches;
        }

        List<Attendance> combined = new ArrayList<>(directMatches.size() + viaStudentClass.size());
        combined.addAll(directMatches);
        combined.addAll(viaStudentClass);
        return combined;
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

