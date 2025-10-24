package com.learnmate.controller;

import com.learnmate.model.Role;
import com.learnmate.model.SchoolClass;
import com.learnmate.model.Subject;
import com.learnmate.model.Timetable;
import com.learnmate.model.User;
import com.learnmate.service.FileStorageService;
import com.learnmate.service.NotificationService;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/timetables")
public class TimetableController extends BaseController {
    private final TimetableService timetableService;
    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final SchoolClassService schoolClassService;
    private final SubjectService subjectService;
    private final NotificationService notificationService;

    public TimetableController(TimetableService timetableService, UserService userService,
                              FileStorageService fileStorageService, SchoolClassService schoolClassService,
                              SubjectService subjectService, NotificationService notificationService) {
        this.timetableService = timetableService;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
        this.schoolClassService = schoolClassService;
        this.subjectService = subjectService;
        this.notificationService = notificationService;
    }

    @GetMapping("/weekly")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT', 'ADMIN')")
    public String weeklyTimetableView(@RequestParam(value = "classId", required = false) Long classId, 
                                      Model model, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
            List<Timetable> timetables;
            boolean isStudentView = false;
            boolean isTeacherView = false;
            boolean isAdminView = false;

            // Ensure template-scoped selections are always defined
            model.addAttribute("selectedClassId", classId);
            model.addAttribute("selectedClass", null);
            model.addAttribute("info", null);
            model.addAttribute("timeSlots", new String[0]);
            model.addAttribute("grid", new java.util.HashMap<String, java.util.List<Timetable>>());
            
            // Add school classes to model for teacher selection
            model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
            
            if (hasRole("STUDENT")) {
                isStudentView = true;
                // Students see timetables for their class and subjects
                if (currentUser.getSchoolClass() != null && currentUser.getSubjects() != null && !currentUser.getSubjects().isEmpty()) {
                    timetables = safeTimetableList(
                        timetableService.getTimetablesByClassAndSubjects(currentUser.getSchoolClass(), currentUser.getSubjects()));
                    model.addAttribute("studentClass", currentUser.getSchoolClass().getName());
                    model.addAttribute("studentSubjects", currentUser.getSubjects());
                } else {
                    timetables = Collections.emptyList();
                    if (currentUser.getSchoolClass() == null) {
                        model.addAttribute("error", "No class assigned. Please contact your administrator.");
                    } else {
                        model.addAttribute("error", "No subjects enrolled. Please contact your administrator.");
                    }
                }
            } else if (hasRole("TEACHER")) {
                isTeacherView = true;
                // Teachers need to select a class first
                if (classId != null) {
                    SchoolClass selectedClass = schoolClassService.getSchoolClassById(classId).orElse(null);
                    if (selectedClass != null) {
                        // Get timetables for the selected class
                        timetables = safeTimetableList(timetableService.getTimetablesBySchoolClass(selectedClass));
                        model.addAttribute("selectedClass", selectedClass);
                        model.addAttribute("selectedClassId", classId);
                    } else {
                        timetables = Collections.emptyList();
                        model.addAttribute("error", "Selected class not found.");
                    }
                } else {
                    timetables = Collections.emptyList();
                    model.addAttribute("info", "Please select a class to view its timetable.");
                }
            } else if (hasRole("ADMIN")) {
                isAdminView = true;
                // Admins see all timetables
                timetables = safeTimetableList(timetableService.getAllTimetables());
            } else {
                timetables = Collections.emptyList();
            }
            
            // Set view flags
            model.addAttribute("isStudentView", isStudentView);
            model.addAttribute("isTeacherView", isTeacherView);
            model.addAttribute("isAdminView", isAdminView);
            model.addAttribute("timetables", timetables);
            
            // Add days array to model
            model.addAttribute("days", java.util.Arrays.asList("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"));
            
            // Build weekly grid data
            buildWeeklyGrid(model, timetables);
            
            return "timetables/weekly_view";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading timetable: " + e.getMessage());
            model.addAttribute("timetables", Collections.emptyList());
            model.addAttribute("isStudentView", false);
            model.addAttribute("isTeacherView", false);
            model.addAttribute("isAdminView", false);
            return "timetables/weekly_view";
        }
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT', 'ADMIN')")
    public String listTimetables(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (hasRole("STUDENT")) {
                // Redirect students to the weekly view
                return "redirect:/timetables/weekly";
            } else if (hasRole("TEACHER")) {
                // Redirect teachers to the weekly view
                return "redirect:/timetables/weekly";
            } else if (hasRole("ADMIN")) {
                // For admins, show the list view
                List<Timetable> allTimetables = timetableService.getAllTimetables();
                System.out.println("DEBUG: Admin has " + allTimetables.size() + " timetables");
                model.addAttribute("timetables", allTimetables);
                model.addAttribute("isAdminView", true);
            } else {
                // Default case
                model.addAttribute("timetables", Collections.emptyList());
                model.addAttribute("isAdminView", true);
            }
            return "timetables/list";
        } catch (Exception e) {
            System.out.println("DEBUG: Error in listTimetables: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading timetables: " + e.getMessage());
            model.addAttribute("timetables", Collections.emptyList());
            model.addAttribute("isStudentView", false);
            model.addAttribute("isTeacherView", false);
            model.addAttribute("isAdminView", false);
            return "timetables/list";
        }
    }

    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String createTimetableForm(@RequestParam(value = "classId", required = false) Long classId,
                                      @RequestParam(value = "day", required = false) String day,
                                      @RequestParam(value = "timeSlot", required = false) String timeSlot,
                                      Model model, @AuthenticationPrincipal UserDetails userDetails) {
        Timetable timetable = new Timetable();
        
        // If classId is provided, pre-select the class
        if (classId != null) {
            SchoolClass selectedClass = schoolClassService.getSchoolClassById(classId).orElse(null);
            if (selectedClass != null) {
                timetable.setSchoolClass(selectedClass);
                model.addAttribute("selectedClassId", classId);
            }
        }
        
        // If day and timeSlot are provided, pre-fill them
        if (day != null) {
            try {
                timetable.setDay(java.time.DayOfWeek.valueOf(day.toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Invalid day, ignore
            }
        }
        
        if (timeSlot != null) {
            // Parse time slot like "08:00-09:00" into startTime and endTime
            try {
                String[] times = timeSlot.split("-");
                if (times.length == 2) {
                    timetable.setStartTime(java.time.LocalTime.parse(times[0]));
                    timetable.setEndTime(java.time.LocalTime.parse(times[1]));
                }
            } catch (Exception e) {
                // Invalid time format, ignore
            }
        }
        
        model.addAttribute("timetable", timetable);
        model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("teachers", userService.getUsersByRole(Role.TEACHER));
        return "timetables/create";
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String createTimetable(@Valid @ModelAttribute Timetable timetable, BindingResult result, 
                                 @RequestParam(value = "file", required = false) MultipartFile file,
                                 @RequestParam(value = "subjectId", required = false) Long subjectId,
                                 @RequestParam(value = "teacherId", required = false) Long teacherId,
                                 @RequestParam(value = "classId", required = false) Long classId,
                                 @RequestParam(value = "room", required = false) String room,
                                 @AuthenticationPrincipal UserDetails userDetails, Model model,
                                 RedirectAttributes redirectAttributes) {
        
        System.out.println("=== CREATE TIMETABLE CALLED ===");
        System.out.println("classId: " + classId);
        System.out.println("subjectId: " + subjectId);
        System.out.println("teacherId: " + teacherId);
        System.out.println("room: " + room);
        System.out.println("timetable.day: " + timetable.getDay());
        System.out.println("timetable.startTime: " + timetable.getStartTime());
        System.out.println("timetable.endTime: " + timetable.getEndTime());
        System.out.println("BindingResult errors: " + result.hasErrors());
        if (result.hasErrors()) {
            System.out.println("Validation errors found, but proceeding anyway...");
            // Don't return early - continue with the save process
        }
        
        // Handle file upload if provided
        if (file != null && !file.isEmpty()) {
            try {
                String fileName = fileStorageService.storeFile(file, FileStorageService.FileType.TIMETABLE);
                timetable.setFilePath(fileName);
            } catch (Exception e) {
                model.addAttribute("error", "Failed to upload file: " + e.getMessage());
                model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
                model.addAttribute("subjects", subjectService.getAllSubjects());
                model.addAttribute("teachers", userService.getUsersByRole(Role.TEACHER));
                return "timetables/create";
            }
        }
        
        // Set school class from the classId parameter (already selected)
        if (classId != null) {
            SchoolClass schoolClass = schoolClassService.getSchoolClassById(classId).orElse(null);
            timetable.setSchoolClass(schoolClass);
        }
        
        // Set default title and description
        if (timetable.getTitle() == null || timetable.getTitle().trim().isEmpty()) {
            timetable.setTitle("Lecture");
        }
        if (timetable.getDescription() == null || timetable.getDescription().trim().isEmpty()) {
            timetable.setDescription("Scheduled lecture");
        }
        
        // Set room from parameter
        if (room != null && !room.trim().isEmpty()) {
            timetable.setRoom(room);
        }
        
        // Set subject if provided
        if (subjectId != null) {
            Subject subject = subjectService.getSubjectById(subjectId).orElse(null);
            timetable.setSubject(subject);
        }
        
        // Set teacher - use provided teacher or current user
        User teacher;
        if (teacherId != null) {
            teacher = userService.getUserById(teacherId).orElseThrow();
        } else {
            teacher = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        }
        timetable.setTeacher(teacher);
        
        try {
            timetableService.createTimetable(timetable);

            notificationService.createNotificationForUpdatedTimetable(timetable);

            redirectAttributes.addFlashAttribute("success", "Lecture created successfully!");
        } catch (RuntimeException e) {
            System.out.println("Schedule conflict error: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            if (timetable.getSchoolClass() != null) {
                return "redirect:/timetables/weekly?classId=" + timetable.getSchoolClass().getId();
            } else {
                return "redirect:/timetables/list";
            }
        } catch (Exception e) {
            System.out.println("Other error: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error creating timetable: " + e.getMessage());
            if (timetable.getSchoolClass() != null) {
                return "redirect:/timetables/weekly?classId=" + timetable.getSchoolClass().getId();
            } else {
                return "redirect:/timetables/list";
            }
        }
        
        // If we have a classId from the form, redirect back to that class
        if (timetable.getSchoolClass() != null) {
            return "redirect:/timetables/weekly?classId=" + timetable.getSchoolClass().getId();
        }
        return "redirect:/timetables/weekly";
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String editTimetableForm(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails, 
                                   @RequestParam(value = "classId", required = false) Long classId,
                                   Model model, RedirectAttributes redirectAttributes) {
        System.out.println("=== EDIT FORM CALLED ===");
        System.out.println("id: " + id);
        System.out.println("classId: " + classId);
        Timetable timetable = timetableService.getTimetableById(id).orElseThrow();
        
        model.addAttribute("timetable", timetable);
        model.addAttribute("classId", classId);
        model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("teachers", userService.getUsersByRole(Role.TEACHER));
        return "timetables/edit";
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String updateTimetable(@PathVariable Long id, @Valid @ModelAttribute Timetable timetable, BindingResult result, 
                                 @RequestParam(value = "file", required = false) MultipartFile file,
                                 @RequestParam(value = "subjectId", required = false) Long subjectId,
                                 @RequestParam(value = "teacherId", required = false) Long teacherId,
                                 @RequestParam(value = "classId", required = false) Long classId,
                                 @RequestParam(value = "room", required = false) String room,
                                 @AuthenticationPrincipal UserDetails userDetails, Model model,
                                 RedirectAttributes redirectAttributes) {
        
        System.out.println("=== UPDATE TIMETABLE CALLED ===");
        System.out.println("id: " + id);
        System.out.println("classId: " + classId);
        System.out.println("subjectId: " + subjectId);
        System.out.println("teacherId: " + teacherId);
        System.out.println("room: " + room);
        System.out.println("BindingResult errors: " + result.hasErrors());
        
        if (result.hasErrors()) {
            System.out.println("Validation errors found, but proceeding anyway...");
            // Don't return early - continue with the save process
        }
        
        // Get existing timetable for reference
        Timetable existingTimetable = timetableService.getTimetableById(id).orElseThrow();
        
        // Note: Teachers can edit any timetable in the class they're viewing
        // This allows for flexible schedule management and teacher reassignment
        
        // Check for time slot conflicts (excluding the current timetable being edited)
        if (timetable.getSchoolClass() != null && timetable.getDay() != null && 
            timetable.getStartTime() != null && timetable.getEndTime() != null) {
            
            List<Timetable> existingTimetables = timetableService.getTimetablesByClassAndDay(
                timetable.getSchoolClass(), timetable.getDay());
            
            for (Timetable existing : existingTimetables) {
                if (!existing.getId().equals(id) && // Exclude current timetable
                    isTimeOverlap(timetable.getStartTime(), timetable.getEndTime(), 
                                existing.getStartTime(), existing.getEndTime())) {
                    model.addAttribute("error", "Time slot conflict! There's already a class scheduled at this time.");
                    model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
                    model.addAttribute("subjects", subjectService.getAllSubjects());
                    model.addAttribute("teachers", userService.getUsersByRole(Role.TEACHER));
                    return "timetables/edit";
                }
            }
        }
        
        // Handle file upload if a new file is provided
        if (file != null && !file.isEmpty()) {
            try {
                String fileName = fileStorageService.storeFile(file, FileStorageService.FileType.TIMETABLE);
                timetable.setFilePath(fileName);
            } catch (Exception e) {
                model.addAttribute("error", "Failed to upload file: " + e.getMessage());
                model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
                model.addAttribute("subjects", subjectService.getAllSubjects());
                model.addAttribute("teachers", userService.getUsersByRole(Role.TEACHER));
                model.addAttribute("timetable", timetable);
                return "timetables/edit";
            }
        } else {
            // Preserve existing file path if no new file is uploaded
            timetable.setFilePath(existingTimetable.getFilePath());
        }
        
        // Keep the existing school class (already selected)
        timetable.setSchoolClass(existingTimetable.getSchoolClass());
        
        // Keep existing title and description
        timetable.setTitle(existingTimetable.getTitle());
        timetable.setDescription(existingTimetable.getDescription());
        timetable.setDay(existingTimetable.getDay());
        timetable.setStartTime(existingTimetable.getStartTime());
        timetable.setEndTime(existingTimetable.getEndTime());
        
        // Set room from parameter
        if (room != null && !room.trim().isEmpty()) {
            timetable.setRoom(room);
        } else {
            timetable.setRoom(existingTimetable.getRoom());
        }
        
        // Set subject if provided
        if (subjectId != null) {
            Subject subject = subjectService.getSubjectById(subjectId).orElse(null);
            timetable.setSubject(subject);
        } else {
            timetable.setSubject(existingTimetable.getSubject());
        }
        
        // Set teacher - use provided teacher or preserve existing
        User teacher;
        if (teacherId != null) {
            teacher = userService.getUserById(teacherId).orElseThrow();
        } else {
            teacher = existingTimetable.getTeacher();
        }
        
        timetable.setId(id);
        timetable.setTeacher(teacher);
        
        try {
            timetableService.updateTimetable(timetable);
            redirectAttributes.addFlashAttribute("success", "Lecture updated successfully!");
        } catch (RuntimeException e) {
            System.out.println("Schedule conflict error: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            if (classId != null) {
                return "redirect:/timetables/weekly?classId=" + classId;
            } else {
                return "redirect:/timetables/list";
            }
        } catch (Exception e) {
            System.out.println("Other error: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error updating timetable: " + e.getMessage());
            if (classId != null) {
                return "redirect:/timetables/weekly?classId=" + classId;
            } else {
                return "redirect:/timetables/list";
            }
        }
        
        if (classId != null) {
            return "redirect:/timetables/weekly?classId=" + classId;
        }
        return "redirect:/timetables/weekly";
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String deleteTimetable(@PathVariable Long id, 
                                 @RequestParam(value = "classId", required = false) Long classId,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            timetableService.deleteTimetable(id);
            redirectAttributes.addFlashAttribute("success", "Lecture deleted successfully!");
            if (classId != null) {
                return "redirect:/timetables/weekly?classId=" + classId;
            }
            return "redirect:/timetables/weekly";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting timetable: " + e.getMessage());
            if (classId != null) {
                return "redirect:/timetables/weekly?classId=" + classId;
            }
            return "redirect:/timetables/weekly";
        }
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public String searchTimetables(@RequestParam String day, Model model) {
        model.addAttribute("timetables", timetableService.getTimetablesByDay(java.time.DayOfWeek.valueOf(day.toUpperCase())));
        return "timetables/list";
    }
    
    /**
     * DEDICATED STUDENT TIMETABLE VIEW - Shows timetable for student's assigned class
     */
    @GetMapping("/student/view")
    @PreAuthorize("hasRole('STUDENT')")
    public String viewStudentTimetable(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        try {
        // Step A: Get the currently logged-in user's details
        String username = userDetails.getUsername();
            System.out.println("DEBUG: Student username: " + username);
        
        // Step B: Find the student entity from the database
        User student = userService.getUserByUsername(username)
            .orElseThrow(() -> new IllegalStateException("Student not found"));
            System.out.println("DEBUG: Student found: " + student.getName() + ", Role: " + student.getRole());
        
        // Step C: Get the student's assigned SchoolClass (Grade)
        SchoolClass studentClass = student.getSchoolClass();
            System.out.println("DEBUG: Student class: " + (studentClass != null ? studentClass.getName() : "NULL"));
            
        if (studentClass == null) {
            // Handle case where student is not assigned to a class yet
                System.out.println("DEBUG: Student has no class assigned");
                model.addAttribute("errorMessage", "You are not yet assigned to a class. Please contact the administrator to assign you to a class.");
            model.addAttribute("studentName", student.getName());
            model.addAttribute("timetables", Collections.emptyList());
            return "student/timetable_view";
        }
        
        // Step D: Find the timetables associated with that SchoolClass
        List<Timetable> classTimetables = timetableService.getTimetablesBySchoolClass(studentClass);
            System.out.println("DEBUG: Found " + classTimetables.size() + " timetables for class " + studentClass.getName());
            
            // Filter timetables to only subjects the student enrolled (keep general entries with no subject)
            if (student.getSubjects() != null && !student.getSubjects().isEmpty()) {
                classTimetables = classTimetables.stream()
                        .filter(t -> t.getSubject() == null || student.getSubjects().contains(t.getSubject()))
                        .toList();
                System.out.println("DEBUG: After subject-filter, timetables count: " + classTimetables.size());
            }
            
        if (!classTimetables.isEmpty()) {
            // Build a compact weekly grid for Monday-Friday to reduce vertical scrolling
            java.time.format.DateTimeFormatter tf = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
            java.util.List<String> days = java.util.Arrays.asList("MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY");

            // Collect unique time slots (as strings) sorted by start time
            java.util.List<com.learnmate.model.Timetable> sortedByTime = classTimetables.stream()
                    .sorted(java.util.Comparator
                            .comparing(com.learnmate.model.Timetable::getStartTime, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                            .thenComparing(com.learnmate.model.Timetable::getEndTime, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                    .toList();

            java.util.LinkedHashSet<String> slotSet = new java.util.LinkedHashSet<>();
            for (com.learnmate.model.Timetable t : sortedByTime) {
                if (t.getStartTime() != null && t.getEndTime() != null && t.getDay() != null) {
                    String slot = tf.format(t.getStartTime()) + "-" + tf.format(t.getEndTime());
                    slotSet.add(slot);
                }
            }
            java.util.List<String> slots = new java.util.ArrayList<>(slotSet);

            // Build grid map: key = DAY|TIME_RANGE, value = list of timetables for that cell
            java.util.Map<String, java.util.List<com.learnmate.model.Timetable>> grid = new java.util.HashMap<>();
            for (com.learnmate.model.Timetable t : classTimetables) {
                if (t.getDay() != null && days.contains(t.getDay().name()) && t.getStartTime() != null && t.getEndTime() != null) {
                    String key = t.getDay().name() + "|" + tf.format(t.getStartTime()) + "-" + tf.format(t.getEndTime());
                    java.util.List<com.learnmate.model.Timetable> bucket = grid.get(key);
                    if (bucket == null) {
                        bucket = new java.util.ArrayList<>();
                        grid.put(key, bucket);
                    }
                    bucket.add(t);
                }
            }

            model.addAttribute("timetables", classTimetables);
            model.addAttribute("days", days);
            model.addAttribute("timeSlots", slots);
            model.addAttribute("grid", grid);
            model.addAttribute("studentClass", studentClass.getName());
            model.addAttribute("studentName", student.getName());
        } else {
                model.addAttribute("errorMessage", "No timetable has been created for your class (" + studentClass.getName() + ") yet. Please ask your teacher to create a timetable.");
            model.addAttribute("studentClass", studentClass.getName());
            model.addAttribute("studentName", student.getName());
            model.addAttribute("timetables", Collections.emptyList());
        }
        
        return "student/timetable_view";
        } catch (Exception e) {
            System.out.println("DEBUG: Error in viewStudentTimetable: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "An error occurred while loading your timetable: " + e.getMessage());
            model.addAttribute("timetables", Collections.emptyList());
            return "student/timetable_view";
        }
    }
    
    private boolean isTimeOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return false;
        }
        return start1.isBefore(end2) && start2.isBefore(end1);
    }
    
    private void buildWeeklyGrid(Model model, List<Timetable> timetables) {
        List<Timetable> safeTimetables = timetables == null ? Collections.emptyList() : timetables;
        // Define days
        String[] days = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"};
        
        // Extract unique time slots from actual timetables
        java.util.Set<String> timeSlotSet = new java.util.TreeSet<>();
        for (Timetable t : safeTimetables) {
            if (t.getStartTime() != null && t.getEndTime() != null) {
                String timeSlot = t.getStartTime().toString() + "-" + t.getEndTime().toString();
                timeSlotSet.add(timeSlot);
            }
        }
        
        // Convert to array and sort
        String[] slots = timeSlotSet.toArray(new String[0]);
        
        // If no timetables, use default slots
        if (slots.length == 0) {
            slots = new String[]{
                "08:00-09:00", "09:00-10:00", "10:00-11:00", 
                "11:00-12:00", "12:00-13:00", "13:00-14:00", "14:00-15:00"
            };
        }
        
        // Build grid map: key = DAY|TIME_RANGE, value = list of timetables for that cell
        java.util.Map<String, java.util.List<Timetable>> grid = new java.util.HashMap<>();
        
        for (Timetable t : safeTimetables) {
            if (t.getDay() != null && java.util.Arrays.asList(days).contains(t.getDay().name()) && t.getStartTime() != null && t.getEndTime() != null) {
                String startTime = t.getStartTime().toString();
                String endTime = t.getEndTime().toString();
                String key = t.getDay().name() + "|" + startTime + "-" + endTime;
                grid.computeIfAbsent(key, k -> {
                    if (k == null) {
                        return new java.util.ArrayList<>();
                    }
                    return new java.util.ArrayList<>();
                }).add(t);
            }
        }
        
        model.addAttribute("timeSlots", slots);
        model.addAttribute("grid", grid);
    }

    private List<Timetable> safeTimetableList(List<Timetable> source) {
        return source == null ? Collections.emptyList() : source;
    }
}


