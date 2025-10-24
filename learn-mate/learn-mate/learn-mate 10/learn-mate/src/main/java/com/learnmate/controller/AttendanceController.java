package com.learnmate.controller;

import com.learnmate.model.Attendance;
import com.learnmate.model.SchoolClass;
import com.learnmate.model.Subject;
import com.learnmate.model.Role;
import com.learnmate.model.Timetable;
import com.learnmate.model.User;
import com.learnmate.service.AttendanceService;
import com.learnmate.service.SchoolClassService;
import com.learnmate.service.SubjectService;
import com.learnmate.service.TimetableService;
import com.learnmate.service.UserService;
import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/attendances")
public class AttendanceController extends BaseController {

    private final AttendanceService attendanceService;
    private final TimetableService timetableService;
    private final UserService userService;
    private final SchoolClassService schoolClassService;
    private final SubjectService subjectService;

    public AttendanceController(AttendanceService attendanceService,
                                TimetableService timetableService,
                                UserService userService,
                                SchoolClassService schoolClassService,
                                SubjectService subjectService) {
        this.attendanceService = attendanceService;
        this.timetableService = timetableService;
        this.userService = userService;
        this.schoolClassService = schoolClassService;
        this.subjectService = subjectService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'PARENT', 'STUDENT', 'ADMIN')")
    public String redirectToAttendanceList(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        return "redirect:/attendances/list";
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('TEACHER', 'PARENT', 'STUDENT', 'ADMIN')")
    public String listAttendances(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = null;
        try {
            user = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
            List<Attendance> attendances = loadAttendancesForUser(user);
            List<SchoolClass> classOptions = resolveClassOptions(user);
            populateAttendanceModel(model, user, attendances, classOptions, null, null);
        } catch (Exception e) {
            model.addAttribute("error", "Error loading attendances: " + e.getMessage());
            if (user != null) {
                populateAttendanceModel(model, user, List.of(), resolveClassOptions(user), null, null);
            } else {
                model.addAttribute("attendanceGroups", List.of());
                model.addAttribute("classOptions", List.of());
                model.addAttribute("attendances", List.of());
            }
        }
        return "attendances/list";
    }

    @GetMapping("/create")
    @PreAuthorize("hasRole('TEACHER')")
    public String createAttendanceForm(Model model) {
        model.addAttribute("attendance", new Attendance());
        model.addAttribute("timetables", timetableService.getAllTimetables());
        model.addAttribute("students", userService.getUsersByRole(Role.STUDENT));
        return "attendances/create";
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('TEACHER')")
    public String createAttendance(@Valid @ModelAttribute Attendance attendance, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("timetables", timetableService.getAllTimetables());
            model.addAttribute("students", userService.getUsersByRole(Role.STUDENT));
            return "attendances/create";
        }
        attendanceService.createAttendance(attendance);
        return "redirect:/attendances";
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String editAttendanceForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return attendanceService.getAttendanceById(id)
            .map(attendance -> {
                model.addAttribute("attendance", attendance);
                return "attendances/edit";
            })
            .orElseGet(() -> {
                redirectAttributes.addFlashAttribute("error", "Attendance record not found.");
                return "redirect:/attendances";
            });
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String updateAttendance(@PathVariable Long id,
                                   @RequestParam(value = "present", required = false) String presentValue,
                                   @RequestParam(value = "notes", required = false) String notes,
                                   RedirectAttributes redirectAttributes) {
        boolean present = presentValue != null && !presentValue.equalsIgnoreCase("false");
        boolean updated = attendanceService.updateAttendanceStatus(id, present, notes);
        if (updated) {
            redirectAttributes.addFlashAttribute("success", "Attendance updated successfully.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Unable to update attendance. It may have been removed or is no longer available.");
        }
        return "redirect:/attendances";
    }

    @GetMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String deleteAttendance(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        boolean deleted = attendanceService.deleteAttendance(id);
        if (deleted) {
            redirectAttributes.addFlashAttribute("success", "Attendance deleted successfully.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Unable to delete attendance. It may have already been removed.");
        }
        return "redirect:/attendances";
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('TEACHER', 'PARENT', 'STUDENT', 'ADMIN')")
    public String searchAttendances(@RequestParam(required = false) Long studentId,
                                    Model model,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        List<Attendance> baseAttendances = loadAttendancesForUser(user);
        List<SchoolClass> classOptions = resolveClassOptions(user);

        if (studentId == null) {
            model.addAttribute("error", "Please enter a student ID to search.");
            populateAttendanceModel(model, user, baseAttendances, classOptions, null, null);
            return "attendances/list";
        }

        List<Attendance> filtered = baseAttendances.stream()
            .filter(attendance -> attendance.getStudent() != null && Objects.equals(attendance.getStudent().getId(), studentId))
            .toList();

        if (filtered.isEmpty()) {
            model.addAttribute("info", "No attendance records found for the provided student ID.");
        }

        populateAttendanceModel(model, user, filtered, classOptions, null, null);
        model.addAttribute("searchStudentId", studentId);
        return "attendances/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error searching attendances: " + e.getMessage());
            return listAttendances(model, userDetails);
        }
    }

    @GetMapping("/search-by-class")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String searchAttendancesByClass(@RequestParam(required = false) Long classId,
                                           Model model,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
            List<Attendance> baseAttendances = loadAttendancesForUser(user);
            List<SchoolClass> classOptions = resolveClassOptions(user);

            if (classId == null) {
                model.addAttribute("error", "Please select a class to search.");
                populateAttendanceModel(model, user, baseAttendances, classOptions, null, null);
                return "attendances/list";
            }

            boolean accessible = classOptions.stream()
                .filter(Objects::nonNull)
                .anyMatch(clazz -> clazz.getId() != null && Objects.equals(clazz.getId(), classId));

            if (!accessible) {
                model.addAttribute("error", "You do not have access to the selected class.");
                populateAttendanceModel(model, user, baseAttendances, classOptions, null, null);
                return "attendances/list";
            }

            List<Attendance> filtered = baseAttendances.stream()
                .filter(attendance -> attendance.getSchoolClass() != null && Objects.equals(attendance.getSchoolClass().getId(), classId))
                .toList();

            if (filtered.isEmpty()) {
                model.addAttribute("info", "No attendance records found for the selected class.");
            }

            populateAttendanceModel(model, user, filtered, classOptions, classId, null);
            return "attendances/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error searching by class: " + e.getMessage());
            return listAttendances(model, userDetails);
        }
    }

    @GetMapping("/history/{classId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String viewClassHistory(@PathVariable Long classId,
                                   @RequestParam(value = "date", required = false) String date,
                                   Model model,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (classId == null) {
                model.addAttribute("error", "Missing class identifier for history view.");
                return listAttendances(model, userDetails);
            }

            User user = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
            SchoolClass selectedClass = schoolClassService.getSchoolClassById(classId)
                .orElse(null);

            if (selectedClass == null) {
                model.addAttribute("error", "The requested class could not be found.");
                return listAttendances(model, userDetails);
            }

            List<SchoolClass> classOptions = new ArrayList<>(resolveClassOptions(user));
            boolean classAlreadyPresent = classOptions.stream()
                .filter(Objects::nonNull)
                .anyMatch(clazz -> clazz.getId() != null && Objects.equals(clazz.getId(), selectedClass.getId()));
            if (!classAlreadyPresent) {
                classOptions.add(selectedClass);
            }

            LocalDate parsedDate = null;
            if (date != null && !date.isBlank()) {
                try {
                    parsedDate = LocalDate.parse(date);
                } catch (DateTimeParseException ex) {
                    model.addAttribute("error", "Invalid date provided. Please use YYYY-MM-DD format.");
                }
            }
            final LocalDate selectedDate = parsedDate;

            List<Attendance> attendances = attendanceService.getAttendancesForClasses(List.of(selectedClass));
            if (selectedDate != null) {
                attendances = attendances.stream()
                    .filter(attendance -> selectedDate.equals(attendance.getDate()))
                    .toList();
            }

            populateAttendanceModel(model, user, attendances, classOptions, selectedClass.getId(), selectedDate);

            if (attendances.isEmpty()) {
                StringBuilder infoMessage = new StringBuilder("No attendance records found for ")
                    .append(selectedClass.getName() != null ? selectedClass.getName() : "this class");
                if (selectedDate != null) {
                    infoMessage.append(" on ").append(selectedDate);
                }
                infoMessage.append('.') ;
                model.addAttribute("info", infoMessage.toString());
            }

            model.addAttribute("activeHistoryClassId", selectedClass.getId());
            return "attendances/list";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load attendance history: " + e.getMessage());
            return listAttendances(model, userDetails);
        }
    }

    @GetMapping("/search-by-date")
    @PreAuthorize("hasAnyRole('TEACHER', 'PARENT', 'STUDENT', 'ADMIN')")
    public String searchAttendancesByDate(@RequestParam String date,
                                          Model model,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
            List<Attendance> baseAttendances = loadAttendancesForUser(user);
            List<SchoolClass> classOptions = resolveClassOptions(user);

            LocalDate targetDate;
            try {
                targetDate = LocalDate.parse(date);
            } catch (DateTimeParseException ex) {
                model.addAttribute("error", "Invalid date format. Please use YYYY-MM-DD.");
                populateAttendanceModel(model, user, baseAttendances, classOptions, null, null);
                return "attendances/list";
            }

            List<Attendance> filtered = baseAttendances.stream()
                .filter(attendance -> targetDate.equals(attendance.getDate()))
                .toList();

            if (filtered.isEmpty()) {
                model.addAttribute("info", "No attendance records found for " + targetDate + ".");
            }

            populateAttendanceModel(model, user, filtered, classOptions, null, targetDate);
            return "attendances/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error searching by date: " + e.getMessage());
            return listAttendances(model, userDetails);
        }
    }

    private List<Attendance> loadAttendancesForUser(User user) {
        if (user == null || user.getRole() == null) {
            return attendanceService.getAllAttendances();
        }

        return switch (user.getRole()) {
            case ADMIN -> attendanceService.getAllAttendances();
            case TEACHER -> {
                List<SchoolClass> classes = resolveClassesForTeacher(user);
                List<Attendance> classAttendances = attendanceService.getAttendancesForClasses(classes);
                if (!classAttendances.isEmpty()) {
                    yield classAttendances;
                }
                yield attendanceService.getAttendancesByTeacher(user);
            }
            case STUDENT -> attendanceService.getAttendancesByStudent(user);
            case PARENT -> attendanceService.getAllAttendances();
            default -> attendanceService.getAllAttendances();
        };
    }

    private List<SchoolClass> resolveClassOptions(User user) {
        if (user == null || user.getRole() == null) {
            return schoolClassService.getAllSchoolClasses();
        }

        return switch (user.getRole()) {
            case ADMIN -> schoolClassService.getAllSchoolClasses();
            case TEACHER -> resolveClassesForTeacher(user);
            case STUDENT -> {
                SchoolClass schoolClass = user.getSchoolClass();
                if (schoolClass != null) {
                    yield List.of(schoolClass);
                }
                yield List.of();
            }
            case PARENT -> schoolClassService.getAllSchoolClasses();
            default -> schoolClassService.getAllSchoolClasses();
        };
    }

    private List<SchoolClass> resolveClassesForTeacher(User teacher) {
        List<SchoolClass> classes = new ArrayList<>();
        if (teacher == null) {
            return classes;
        }

        Set<Long> seen = new LinkedHashSet<>();
        List<Timetable> timetables = timetableService.getTimetablesByTeacher(teacher);
        for (Timetable timetable : timetables) {
            SchoolClass schoolClass = timetable.getSchoolClass();
            if (schoolClass == null) {
                continue;
            }
            Long id = schoolClass.getId();
            if (id == null || seen.add(id)) {
                classes.add(schoolClass);
            }
        }

        if (classes.isEmpty()) {
            classes.addAll(schoolClassService.getAllSchoolClasses());
        }

        classes.sort(Comparator.comparing(SchoolClass::getName, String.CASE_INSENSITIVE_ORDER));
        return classes;
    }

    private void populateAttendanceModel(Model model,
                                         User user,
                                         List<Attendance> attendances,
                                         List<SchoolClass> classOptions,
                                         Long selectedClassId,
                                         LocalDate selectedDate) {
        List<SchoolClass> normalizedClassOptions = new ArrayList<>();
        if (classOptions != null) {
            normalizedClassOptions.addAll(classOptions);
        }

        normalizedClassOptions.removeIf(Objects::isNull);

        Set<Long> seenClassIds = new HashSet<>();
        List<SchoolClass> displayClasses = new ArrayList<>();

        for (SchoolClass option : normalizedClassOptions) {
            if (option == null) {
                continue;
            }
            Long id = option.getId();
            if (id == null || seenClassIds.add(id)) {
                displayClasses.add(option);
            }
        }

        for (Attendance attendance : attendances) {
            if (attendance == null) {
                continue;
            }

            SchoolClass schoolClass = resolveAttendanceClass(attendance);
            if (schoolClass != null) {
                Long id = schoolClass.getId();
                if (id == null || seenClassIds.add(id)) {
                    displayClasses.add(schoolClass);
                }
            }
        }

        boolean containsUnassigned = attendances.stream().anyMatch(this::isUnassignedAttendance);
        if (containsUnassigned && displayClasses.stream().noneMatch(Objects::isNull)) {
            displayClasses.add(null);
        }

        displayClasses.sort(Comparator.comparing(clazz -> clazz != null ? clazz.getName() : null, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        Map<SchoolClass, Map<LocalDate, List<Attendance>>> groupedAttendances = new LinkedHashMap<>();

        for (Attendance attendance : attendances) {
            if (attendance == null || attendance.getDate() == null) {
                continue;
            }

            SchoolClass groupingClass = resolveAttendanceClass(attendance);
            Map<LocalDate, List<Attendance>> dateMap = groupedAttendances.get(groupingClass);
            if (dateMap == null) {
                dateMap = new TreeMap<>(Comparator.reverseOrder());
                groupedAttendances.put(groupingClass, dateMap);
            }

            List<Attendance> entries = dateMap.get(attendance.getDate());
            if (entries == null) {
                entries = new ArrayList<>();
                dateMap.put(attendance.getDate(), entries);
            }
            entries.add(attendance);
        }

        if (selectedClassId != null) {
            displayClasses.stream()
                .filter(clazz -> clazz != null && Objects.equals(clazz.getId(), selectedClassId))
                .findFirst()
                .ifPresent(clazz -> groupedAttendances.putIfAbsent(clazz, new TreeMap<>(Comparator.reverseOrder())));
        }

        model.addAttribute("attendances", attendances);
        model.addAttribute("groupedAttendances", groupedAttendances);
        model.addAttribute("classOptions", displayClasses);
        model.addAttribute("selectedClassId", selectedClassId);
        model.addAttribute("selectedDate", selectedDate != null ? selectedDate.toString() : null);
        model.addAttribute("currentUserRole", user != null ? user.getRole() : null);
        model.addAttribute("hasResults", groupedAttendances.values().stream().anyMatch(map -> map != null && map.values().stream().anyMatch(list -> list != null && !list.isEmpty())));

        if (user != null && user.getRole() == Role.STUDENT) {
            long presentDays = attendances.stream().filter(Attendance::isPresent).count();
            long absentDays = attendances.stream().filter(attendance -> !attendance.isPresent()).count();
            long totalSessions = presentDays + absentDays;
            long totalDays = attendances.stream()
                .map(Attendance::getDate)
                .filter(Objects::nonNull)
                .distinct()
                .count();
            double attendancePercentage = totalSessions == 0 ? 0.0 : (presentDays * 100.0) / totalSessions;

            model.addAttribute("totalDays", totalDays);
            model.addAttribute("presentDays", presentDays);
            model.addAttribute("absentDays", absentDays);
            model.addAttribute("attendancePercentage", attendancePercentage);
        } else {
            model.addAttribute("totalDays", attendances.size());
            long presentDays = attendances.stream().filter(Attendance::isPresent).count();
            long absentDays = attendances.stream().filter(attendance -> !attendance.isPresent()).count();
            model.addAttribute("presentDays", presentDays);
            model.addAttribute("absentDays", absentDays);
            model.addAttribute("attendancePercentage", presentDays + absentDays == 0 ? 0.0 : (presentDays * 100.0) / (presentDays + absentDays));
        }
    }

    private SchoolClass resolveAttendanceClass(Attendance attendance) {
        if (attendance == null) {
            return null;
        }

        if (attendance.getSchoolClass() != null) {
            return attendance.getSchoolClass();
        }

        if (attendance.getStudent() != null) {
            return attendance.getStudent().getSchoolClass();
        }

        return null;
    }

    private boolean isUnassignedAttendance(Attendance attendance) {
        return attendance != null && resolveAttendanceClass(attendance) == null;
    }

    // Interactive Attendance Marking - Class and Subject Selection
    @GetMapping("/mark")
    @PreAuthorize("hasRole('TEACHER')")
    public String selectClassForAttendance(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("=== Attendance Mark Controller Called ===");
            User teacher = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
            System.out.println("Teacher: " + teacher.getUsername() + ", Role: " + teacher.getRole());
            
            // Get classes and subjects that the teacher teaches
            List<SchoolClass> schoolClasses = resolveClassesForTeacher(teacher);
            List<Subject> subjects = subjectService.getAllSubjects();
            
            System.out.println("School Classes found: " + schoolClasses.size());
            System.out.println("Subjects found: " + subjects.size());
            
            // If no classes exist, create some sample data
            if (schoolClasses.isEmpty()) {
                System.out.println("No classes found, creating sample data...");
                schoolClasses = schoolClassService.getAllSchoolClasses();
                subjects = subjectService.getAllSubjects();
                System.out.println("After creating sample data - Classes: " + schoolClasses.size() + ", Subjects: " + subjects.size());
            }
            
            model.addAttribute("schoolClasses", schoolClasses);
            model.addAttribute("subjects", subjects);
            model.addAttribute("currentDate", LocalDate.now());
            model.addAttribute("teacher", teacher);
            
            return "attendances/select-class";
        } catch (Exception e) {
            System.out.println("Error in attendance mark controller: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading classes for attendance: " + e.getMessage());
            model.addAttribute("schoolClasses", java.util.Collections.emptyList());
            model.addAttribute("subjects", java.util.Collections.emptyList());
            model.addAttribute("currentDate", LocalDate.now());
            return "attendances/select-class";
        }
    }

    // Interactive Attendance Marking - Show Student Roster
    @GetMapping("/mark/{classId}")
    @PreAuthorize("hasRole('TEACHER')")
    public String markAttendanceForClass(@PathVariable Long classId, 
                                        @RequestParam(value = "date", required = false) String dateStr,
                                        @RequestParam(value = "subjectId", required = false) Long subjectId,
                                        @AuthenticationPrincipal UserDetails userDetails,
                                        Model model, RedirectAttributes redirectAttributes) {
        try {
            System.out.println("=== Mark Attendance For Class Controller Called ===");
            System.out.println("Class ID: " + classId + ", Date: " + dateStr + ", Subject ID: " + subjectId);
            
            SchoolClass schoolClass = schoolClassService.getSchoolClassById(classId).orElseThrow();
            User teacher = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
            Subject subject = subjectId != null ? subjectService.getSubjectById(subjectId).orElse(null) : null;
            
            LocalDate attendanceDate = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now();
            
            // Get all students in the selected class
            List<User> students = userService.getUsersBySchoolClass(schoolClass);
            System.out.println("Students found for class " + schoolClass.getName() + ": " + students.size());
            
            // Check if attendance has already been marked for this class, subject, and date
            List<Attendance> existingAttendances = attendanceService.getAttendancesBySchoolClassSubjectAndDate(schoolClass, subject, attendanceDate);
            System.out.println("Existing attendances: " + existingAttendances.size());
            

            Map<Long, Attendance> attendanceByStudent = new LinkedHashMap<>();
            for (Attendance existingAttendance : existingAttendances) {
                if (existingAttendance.getStudent() == null || existingAttendance.getStudent().getId() == null) {
                    continue;
                }
                attendanceByStudent.put(existingAttendance.getStudent().getId(), existingAttendance);
            }

            Set<Long> presentStudentIds = existingAttendances.stream()
                .filter(Attendance::isPresent)
                .map(attendance -> attendance.getStudent() != null ? attendance.getStudent().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

            String existingNotes = existingAttendances.stream()
                .map(Attendance::getNotes)
                .filter(Objects::nonNull)
                .filter(notesValue -> !notesValue.isBlank())
                .findFirst()
                .orElse(null);

            model.addAttribute("schoolClass", schoolClass);
            model.addAttribute("subject", subject);
            model.addAttribute("teacher", teacher);
            model.addAttribute("students", students);
            model.addAttribute("attendanceDate", attendanceDate);
            model.addAttribute("existingAttendances", existingAttendances);
            model.addAttribute("existingAttendanceMap", attendanceByStudent);
            model.addAttribute("presentStudentIds", presentStudentIds);
            model.addAttribute("sessionNotes", existingNotes);
            
            return "attendances/mark-roster";
        } catch (Exception e) {
            System.out.println("Error in mark attendance for class: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error loading attendance form: " + e.getMessage());
            return "redirect:/attendances/mark";
        }
    }

    // Interactive Attendance Marking - Process Bulk Attendance
    @PostMapping("/mark/{classId}")
    @PreAuthorize("hasRole('TEACHER')")
    public String processBulkAttendance(@PathVariable Long classId,
                                       @RequestParam("date") String dateStr,
                                       @RequestParam(value = "subjectId", required = false) Long subjectId,
                                       @RequestParam(value = "notes", required = false) String notes,
                                       @RequestParam Map<String, String> allParams,
                                       @AuthenticationPrincipal UserDetails userDetails,
                                       RedirectAttributes redirectAttributes) {
        try {
            System.out.println("=== Process Bulk Attendance Controller Called ===");
            System.out.println("Class ID: " + classId + ", Date: " + dateStr + ", Subject ID: " + subjectId);
            
            SchoolClass schoolClass = schoolClassService.getSchoolClassById(classId).orElseThrow();
            User teacher = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
            Subject subject = subjectId != null ? subjectService.getSubjectById(subjectId).orElse(null) : null;
            
            LocalDate attendanceDate = LocalDate.parse(dateStr);
            
            // Delete existing attendance records for this class, subject, and date
            attendanceService.deleteAttendancesBySchoolClassSubjectAndDate(schoolClass, subject, attendanceDate);
            
            // Get all students in the class
            List<User> students = userService.getUsersBySchoolClass(schoolClass);
            System.out.println("Processing attendance for " + students.size() + " students");
            
            // Process attendance for each student
            for (User student : students) {
                String attendanceKey = "attendance_" + student.getId();
                boolean isPresent = "on".equals(allParams.get(attendanceKey));
                
                Attendance attendance = new Attendance();
                attendance.setStudent(student);
                attendance.setTeacher(teacher);
                attendance.setSubject(subject);
                attendance.setSchoolClass(schoolClass);
                attendance.setDate(attendanceDate);
                attendance.setPresent(isPresent);
                attendance.setNotes(notes);
                
                attendanceService.createAttendance(attendance);
            }
            
            String subjectName = subject != null ? " for " + subject.getName() : "";
            redirectAttributes.addFlashAttribute("success", 
                "Attendance marked successfully for " + schoolClass.getName() + subjectName + " on " + attendanceDate);
            
            return "redirect:/attendances/mark";
        } catch (Exception e) {
            System.out.println("Error in process bulk attendance: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error marking attendance: " + e.getMessage());
            return "redirect:/attendances/mark";
        }
    }
    
}

