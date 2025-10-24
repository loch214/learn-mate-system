package com.learnmate.controller;

import com.learnmate.model.Notification;
import com.learnmate.model.Role;
import com.learnmate.model.User;
import com.learnmate.service.NotificationService;
import com.learnmate.service.SchoolClassService;
import com.learnmate.service.SubjectService;
import com.learnmate.service.UserService;
import com.learnmate.service.FileStorageService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.nio.file.Path;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;
    private final SchoolClassService schoolClassService;
    private final SubjectService subjectService;
    private final FileStorageService fileStorageService;

    public NotificationController(NotificationService notificationService,
                                  UserService userService,
                                  SchoolClassService schoolClassService,
                                  SubjectService subjectService,
                                  FileStorageService fileStorageService) {
        this.notificationService = notificationService;
        this.userService = userService;
        this.schoolClassService = schoolClassService;
        this.subjectService = subjectService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/list")
    public String showNotifications(Model model, Authentication authentication) {
        User currentUser = resolveCurrentUser(authentication);

        List<Notification> receivedNotifications = notificationService.getNotificationsForUser(currentUser);
        List<SentNotificationView> sentNotifications = buildSentNotificationViews(notificationService.getNotificationsCreatedBy(currentUser));

        model.addAttribute("receivedNotifications", receivedNotifications);
        model.addAttribute("sentNotifications", sentNotifications);
        model.addAttribute("currentUserId", currentUser != null ? currentUser.getId() : null);
        model.addAttribute("unreadNotificationCount", notificationService.countUnreadNotifications(currentUser));

        return "notifications/list";
    }

    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public String showCreateForm(Model model, Authentication authentication) {
        List<String> roleSelections;
        Object existingPrefillRoles = model.getAttribute("prefillRoles");
        if (existingPrefillRoles instanceof List<?>) {
            roleSelections = ((List<?>) existingPrefillRoles).stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
        } else {
            roleSelections = List.of();
        }
        model.addAttribute("prefillRoles", roleSelections);

        if (!model.containsAttribute("prefillTitle")) {
            model.addAttribute("prefillTitle", "");
        }
        if (!model.containsAttribute("prefillMessage")) {
            model.addAttribute("prefillMessage", "");
        }
        if (!model.containsAttribute("prefillSelectedClasses")) {
            model.addAttribute("prefillSelectedClasses", List.of());
        }

        model.addAttribute("roles", Role.values());
        model.addAttribute("schoolClasses", schoolClassService.getAllSchoolClasses());
        
        // Get subjects based on user role
        User currentUser = resolveCurrentUser(authentication);
        if (currentUser != null && currentUser.getRole() == Role.TEACHER) {
            // Teachers see only their subjects
            model.addAttribute("teacherSubjects", currentUser.getSubjects());
        } else if (currentUser != null && currentUser.getRole() == Role.ADMIN) {
            // Admins see all subjects
            model.addAttribute("teacherSubjects", subjectService.getAllSubjects());
        } else {
            model.addAttribute("teacherSubjects", List.of());
        }
        
        return "notifications/create";
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public String handleCreateNotification(Authentication authentication,
                                           @RequestParam(required = false) String title,
                                           @RequestParam(required = false) String message,
                                           @RequestParam(required = false, name = "targetRoles") List<String> targetRoles,
                                           @RequestParam(required = false, name = "selectedClasses") List<Long> selectedClasses,
                                           @RequestParam(required = false, name = "selectedSubjects") List<Long> selectedSubjects,
                                           @RequestParam(required = false) MultipartFile file,
                                           RedirectAttributes redirectAttributes) {
        String sanitizedTitle = title != null ? title.trim() : "";
        String sanitizedMessage = message != null ? message.trim() : "";

        boolean missingTitle = sanitizedTitle.isBlank();
        boolean missingMessage = sanitizedMessage.isBlank();
        boolean missingRoles = targetRoles == null || targetRoles.isEmpty();

        if (missingTitle || missingMessage || missingRoles) {
            StringBuilder error = new StringBuilder();
            if (missingTitle) {
                error.append("Title is required. ");
            }
            if (missingMessage) {
                error.append("Message is required. ");
            }
            if (missingRoles) {
                error.append("Select at least one role. ");
            }

            redirectAttributes.addFlashAttribute("error", error.toString().trim());
            redirectAttributes.addFlashAttribute("prefillTitle", sanitizedTitle);
            redirectAttributes.addFlashAttribute("prefillMessage", sanitizedMessage);
            redirectAttributes.addFlashAttribute("prefillRoles", targetRoles != null ? targetRoles : List.of());
            redirectAttributes.addFlashAttribute("prefillSelectedClasses", selectedClasses != null ? selectedClasses : List.of());
            redirectAttributes.addFlashAttribute("prefillSelectedSubjects", selectedSubjects != null ? selectedSubjects : List.of());
            return "redirect:/notifications/create";
        }

        User author = resolveCurrentUser(authentication);

        // Handle file upload if provided
        String fileName = null;
        String originalFileName = null;
        String filePath = null;
        String fileType = null;
        Long fileSize = null;

        if (file != null && !file.isEmpty()) {
            try {
                fileName = fileStorageService.storeFile(file, FileStorageService.FileType.NOTIFICATION);
                originalFileName = file.getOriginalFilename();
                filePath = fileName; // The service returns the unique filename
                fileType = file.getContentType();
                fileSize = file.getSize();
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Failed to upload file: " + e.getMessage());
                redirectAttributes.addFlashAttribute("prefillTitle", sanitizedTitle);
                redirectAttributes.addFlashAttribute("prefillMessage", sanitizedMessage);
                redirectAttributes.addFlashAttribute("prefillRoles", targetRoles);
                redirectAttributes.addFlashAttribute("prefillSelectedClasses", selectedClasses != null ? selectedClasses : List.of());
                redirectAttributes.addFlashAttribute("prefillSelectedSubjects", selectedSubjects != null ? selectedSubjects : List.of());
                return "redirect:/notifications/create";
            }
        }

        // Check if STUDENT is selected but no classes are chosen
        if (targetRoles.contains("STUDENT") && (selectedClasses == null || selectedClasses.isEmpty())) {
            redirectAttributes.addFlashAttribute("error", "When targeting students, you must select at least one class.");
            redirectAttributes.addFlashAttribute("prefillTitle", sanitizedTitle);
            redirectAttributes.addFlashAttribute("prefillMessage", sanitizedMessage);
            redirectAttributes.addFlashAttribute("prefillRoles", targetRoles);
            redirectAttributes.addFlashAttribute("prefillSelectedClasses", selectedClasses != null ? selectedClasses : List.of());
            redirectAttributes.addFlashAttribute("prefillSelectedSubjects", selectedSubjects != null ? selectedSubjects : List.of());
            return "redirect:/notifications/create";
        }

        // Check if STUDENT is selected but no subjects are chosen
        if (targetRoles.contains("STUDENT") && (selectedSubjects == null || selectedSubjects.isEmpty())) {
            redirectAttributes.addFlashAttribute("error", "When targeting students, you must select at least one subject.");
            redirectAttributes.addFlashAttribute("prefillTitle", sanitizedTitle);
            redirectAttributes.addFlashAttribute("prefillMessage", sanitizedMessage);
            redirectAttributes.addFlashAttribute("prefillRoles", targetRoles);
            redirectAttributes.addFlashAttribute("prefillSelectedClasses", selectedClasses != null ? selectedClasses : List.of());
            redirectAttributes.addFlashAttribute("prefillSelectedSubjects", selectedSubjects != null ? selectedSubjects : List.of());
            return "redirect:/notifications/create";
        }

        notificationService.createManualNotificationWithClassesAndSubjects(author, sanitizedTitle, sanitizedMessage, targetRoles, selectedClasses, selectedSubjects, fileName, originalFileName, filePath, fileType, fileSize);

        redirectAttributes.addFlashAttribute("success", "Notification created successfully.");
        return "redirect:/notifications/list";
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id, Authentication authentication) {
        try {
            User currentUser = resolveCurrentUser(authentication);
            Optional<Notification> notificationOptional = notificationService.getNotification(id);
            
            if (notificationOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Notification notification = notificationOptional.get();
            
            // Check if user has access to this notification
            if (!notification.getTargetUser().equals(currentUser) && 
                !notification.getCreatedBy().equals(currentUser)) {
                return ResponseEntity.status(403).build();
            }
            
            if (notification.getFileName() == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Get file path
            Path filePath = fileStorageService.getFileStorageLocationPublic(FileStorageService.FileType.NOTIFICATION)
                    .resolve(notification.getFileName()).normalize();
            
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            // Determine content type
            String contentType = notification.getFileType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + notification.getOriginalFileName() + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/mark-read/{id}")
    public String markNotificationAsRead(@PathVariable Long id,
                                         Authentication authentication,
                                         RedirectAttributes redirectAttributes) {
        User currentUser = resolveCurrentUser(authentication);
        notificationService.markNotificationAsRead(id, currentUser);
        redirectAttributes.addFlashAttribute("success", "Notification marked as read.");
        return "redirect:/notifications/list";
    }

    @PostMapping("/mark-all-read")
    public String markAllNotificationsAsRead(Authentication authentication,
                                             RedirectAttributes redirectAttributes) {
        User currentUser = resolveCurrentUser(authentication);
        notificationService.markAllAsRead(currentUser);
        redirectAttributes.addFlashAttribute("success", "All notifications marked as read.");
        return "redirect:/notifications/list";
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public String showEditForm(@PathVariable Long id,
                               Model model,
                               RedirectAttributes redirectAttributes,
                               Authentication authentication) {
        User currentUser = resolveCurrentUser(authentication);
        Optional<Notification> notificationOptional = notificationService.getNotification(id);
        if (notificationOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Notification not found.");
            return "redirect:/notifications/list";
        }

        Notification notification = notificationOptional.get();
        if (!canManageNotification(currentUser, notification)) {
            redirectAttributes.addFlashAttribute("error", "You can only edit notifications you created.");
            return "redirect:/notifications/list";
        }
        model.addAttribute("notification", notification);
        return "notifications/edit";
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public String updateNotification(@PathVariable Long id,
                                     @RequestParam String title,
                                     @RequestParam String message,
                                     RedirectAttributes redirectAttributes,
                                     Authentication authentication) {
        User currentUser = resolveCurrentUser(authentication);
        Optional<Notification> notificationOptional = notificationService.getNotification(id);
        if (notificationOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Notification not found.");
            return "redirect:/notifications/list";
        }

        Notification notification = notificationOptional.get();
        if (!canManageNotification(currentUser, notification)) {
            redirectAttributes.addFlashAttribute("error", "You can only edit notifications you created.");
            return "redirect:/notifications/list";
        }

        String sanitizedTitle = title != null ? title.trim() : "";
        String sanitizedMessage = message != null ? message.trim() : "";

        if (sanitizedTitle.isBlank() || sanitizedMessage.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Title and message are required.");
            return "redirect:/notifications/edit/" + id;
        }

        notificationService.updateNotification(id, sanitizedTitle, sanitizedMessage);
        redirectAttributes.addFlashAttribute("success", "Notification updated successfully.");
        return "redirect:/notifications/list";
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public String deleteNotification(@PathVariable Long id,
                                     RedirectAttributes redirectAttributes,
                                     Authentication authentication) {
        User currentUser = resolveCurrentUser(authentication);
        Optional<Notification> notificationOptional = notificationService.getNotification(id);
        if (notificationOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Notification not found.");
            return "redirect:/notifications/list";
        }

        if (!canManageNotification(currentUser, notificationOptional.get())) {
            redirectAttributes.addFlashAttribute("error", "You can only delete notifications you created.");
            return "redirect:/notifications/list";
        }

        notificationService.deleteNotification(id);
        redirectAttributes.addFlashAttribute("success", "Notification deleted successfully.");
        return "redirect:/notifications/list";
    }

    private User resolveCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            throw new IllegalStateException("Unable to resolve authenticated user");
        }
        return userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
    }

    private boolean canManageNotification(User currentUser, Notification notification) {
        if (currentUser == null || notification == null) {
            return false;
        }
        if (currentUser.getRole() == Role.ADMIN) {
            return true;
        }
        User creator = notification.getCreatedBy();
        return creator != null
            && creator.getId() != null
            && creator.getId().equals(currentUser.getId());
    }

    private List<SentNotificationView> buildSentNotificationViews(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return List.of();
        }

        Map<String, SentNotificationAccumulator> grouped = new LinkedHashMap<>();
        for (Notification notification : notifications) {
            if (notification == null) {
                continue;
            }
            String key = resolveAggregateKey(notification);
            SentNotificationAccumulator accumulator = grouped.get(key);
            if (accumulator == null) {
                accumulator = new SentNotificationAccumulator(notification);
                grouped.put(key, accumulator);
            }
            accumulator.include(notification);
        }

        List<SentNotificationView> views = new ArrayList<>();
        grouped.values().stream()
            .sorted((first, second) -> compareByCreatedAt(first.sample(), second.sample()))
            .forEach(accumulator -> views.add(accumulator.toView()));
        return views;
    }

    private static String resolveAggregateKey(Notification notification) {
        String broadcastKey = notification.getBroadcastKey();
        if (broadcastKey != null && !broadcastKey.isBlank()) {
            return broadcastKey;
        }
        return "SINGLE-" + notification.getId();
    }

    private static int compareByCreatedAt(Notification first, Notification second) {
        if (first == null && second == null) {
            return 0;
        }
        if (first == null) {
            return 1;
        }
        if (second == null) {
            return -1;
        }
        var firstCreated = first.getCreatedAt();
        var secondCreated = second.getCreatedAt();
        if (firstCreated == null && secondCreated == null) {
            return 0;
        }
        if (firstCreated == null) {
            return 1;
        }
        if (secondCreated == null) {
            return -1;
        }
        return secondCreated.compareTo(firstCreated);
    }

    private record SentNotificationView(Notification notification, List<String> roles, List<String> classes) {}

    private static final class SentNotificationAccumulator {
        private final Notification sample;
        private final Set<String> roles = new LinkedHashSet<>();
        private final Set<String> classes = new LinkedHashSet<>();

        private SentNotificationAccumulator(Notification sample) {
            this.sample = sample;
            include(sample);
        }

        private SentNotificationAccumulator include(Notification notification) {
            if (notification.getTargetRole() != null) {
                roles.add(notification.getTargetRole());
            }
            if (notification.getTargetClass() != null && notification.getTargetClass().getName() != null) {
                classes.add(notification.getTargetClass().getName());
            }
            return this;
        }

        private Notification sample() {
            return sample;
        }

        private SentNotificationView toView() {
            return new SentNotificationView(sample, new ArrayList<>(roles), new ArrayList<>(classes));
        }
    }

}
