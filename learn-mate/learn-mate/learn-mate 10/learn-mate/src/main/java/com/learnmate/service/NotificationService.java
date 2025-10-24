package com.learnmate.service;

import com.learnmate.model.Exam;
import com.learnmate.model.Material;
import com.learnmate.model.Notification;
import com.learnmate.model.NotificationType;
import com.learnmate.model.Role;
import com.learnmate.model.SchoolClass;
import com.learnmate.model.Timetable;
import com.learnmate.model.User;
import com.learnmate.notifications.factory.NotificationContent;
import com.learnmate.notifications.factory.NotificationFactory;
import com.learnmate.repository.NotificationRepository;
import com.learnmate.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationFactory notificationFactory;
    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               NotificationFactory notificationFactory) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.notificationFactory = notificationFactory;
    }

    @Transactional
    public void createNotificationForNewExam(Exam exam) {
        if (exam == null || exam.getSchoolClass() == null) {
            return;
        }

        NotificationContent content = notificationFactory.createNotificationContent("EXAM", exam);
        String message = content.getMessage();

        createNotificationForClass(
            exam.getSchoolClass(),
            "New Exam Posted",
            message,
            NotificationType.SYSTEM,
            null,
            null
        );
    }

    @Transactional
    public void createNotificationForUpdatedTimetable(Timetable timetable) {
        if (timetable == null || timetable.getSchoolClass() == null) {
            return;
        }

        NotificationContent content = notificationFactory.createNotificationContent("TIMETABLE", timetable);
        String message = content.getMessage();

        createNotificationForClass(
            timetable.getSchoolClass(),
            "Timetable Updated",
            message,
            NotificationType.SYSTEM,
            null,
            null
        );
    }

    @Transactional
    public void createNotificationForClass(SchoolClass schoolClass, String title, String message) {
        createNotificationForClass(schoolClass, title, message, NotificationType.SYSTEM, null, null);
    }

    @Transactional
    public void createNotificationForClass(SchoolClass schoolClass,
                                           String title,
                                           String message,
                                           NotificationType type,
                                           User createdBy,
                                           String broadcastKey) {
        if (schoolClass == null) {
            return;
        }

        List<User> potentialRecipients = userRepository.findBySchoolClass(schoolClass);
        for (User student : potentialRecipients) {
            if (student.getRole() != Role.STUDENT) {
                continue;
            }

            createNotificationForUser(student, schoolClass, title, message, type, Role.STUDENT.name(), createdBy, broadcastKey, null, null, null, null, null);
        }
    }

    @Transactional
    public void createNotificationForRole(String role, String title, String message) {
        createNotificationForRole(role, title, message, NotificationType.SYSTEM, null, null, null, null, null, null, null);
    }

    @Transactional
    public void createNotificationForRole(String role,
                                          String title,
                                          String message,
                                          NotificationType type,
                                          User createdBy,
                                          String broadcastKey,
                                          String fileName,
                                          String originalFileName,
                                          String filePath,
                                          String fileType,
                                          Long fileSize) {
        if (role == null || role.isBlank()) {
            return;
        }

        Role targetRole;
        try {
            targetRole = Role.valueOf(role);
        } catch (IllegalArgumentException ex) {
            return;
        }

        List<User> recipients = userRepository.findByRole(targetRole);
        for (User recipient : recipients) {
            // For non-student roles, school class can be null
            SchoolClass schoolClass = (targetRole == Role.STUDENT) ? recipient.getSchoolClass() : null;
            createNotificationForUser(recipient, schoolClass, title, message, type, role, createdBy, broadcastKey, fileName, originalFileName, filePath, fileType, fileSize);
        }
    }

    @Transactional
    public void createManualNotification(User author,
                                         String title,
                                         String message,
                                         List<String> targetRoles) {
        boolean created = false;

        Set<String> roles = targetRoles != null ? new HashSet<>(targetRoles) : Set.of();
        String broadcastKey = UUID.randomUUID().toString();

        for (String role : roles) {
            createNotificationForRole(role, title, message, NotificationType.MANUAL, author, broadcastKey, null, null, null, null, null);
            created = true;
        }

        if (!created && author != null) {
            Notification notification = buildBaseNotification(title, message, NotificationType.MANUAL);
            notification.setTargetUser(author);
            notification.setCreatedBy(author);
            notification.setBroadcastKey(UUID.randomUUID().toString());
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void createManualNotificationWithClasses(User author,
                                                    String title,
                                                    String message,
                                                    List<String> targetRoles,
                                                    List<Long> selectedClassIds) {
        boolean created = false;
        String broadcastKey = UUID.randomUUID().toString();

        for (String role : targetRoles) {
            if ("STUDENT".equals(role) && selectedClassIds != null && !selectedClassIds.isEmpty()) {
                // Target students in specific classes
                createNotificationForStudentsInClasses(selectedClassIds, title, message, NotificationType.MANUAL, author, broadcastKey);
                created = true;
            } else {
                // Target all users with the specified role (for TEACHER, PARENT, ADMIN)
                createNotificationForRole(role, title, message, NotificationType.MANUAL, author, broadcastKey, null, null, null, null, null);
                created = true;
            }
        }

        if (!created && author != null) {
            Notification notification = buildBaseNotification(title, message, NotificationType.MANUAL);
            notification.setTargetUser(author);
            notification.setCreatedBy(author);
            notification.setBroadcastKey(UUID.randomUUID().toString());
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void createManualNotificationWithClassesAndSubjects(User author,
                                                              String title,
                                                              String message,
                                                              List<String> targetRoles,
                                                              List<Long> selectedClassIds,
                                                              List<Long> selectedSubjectIds,
                                                              String fileName,
                                                              String originalFileName,
                                                              String filePath,
                                                              String fileType,
                                                              Long fileSize) {
        boolean created = false;
        String broadcastKey = UUID.randomUUID().toString();

        for (String role : targetRoles) {
            if ("STUDENT".equals(role) && selectedClassIds != null && !selectedClassIds.isEmpty() && selectedSubjectIds != null && !selectedSubjectIds.isEmpty()) {
                // Target students in specific classes AND enrolled in specific subjects
                createNotificationForStudentsInClassesAndSubjects(selectedClassIds, selectedSubjectIds, title, message, NotificationType.MANUAL, author, broadcastKey, fileName, originalFileName, filePath, fileType, fileSize);
                created = true;
            } else {
                // Target all users with the specified role (for TEACHER, PARENT, ADMIN)
                createNotificationForRole(role, title, message, NotificationType.MANUAL, author, broadcastKey, fileName, originalFileName, filePath, fileType, fileSize);
                created = true;
            }
        }

        if (!created && author != null) {
            Notification notification = buildBaseNotification(title, message, NotificationType.MANUAL);
            notification.setTargetUser(author);
            notification.setCreatedBy(author);
            notification.setBroadcastKey(UUID.randomUUID().toString());
            setFileInfo(notification, fileName, originalFileName, filePath, fileType, fileSize);
            notificationRepository.save(notification);
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForUser(User user) {
        if (user == null) {
            return List.of();
        }

        List<Notification> notifications = new ArrayList<>(notificationRepository.findByTargetUserOrderByCreatedAtDesc(user));

        notifications.sort((first, second) -> {
            LocalDateTime firstTime = first.getCreatedAt();
            LocalDateTime secondTime = second.getCreatedAt();

            if (Objects.equals(firstTime, secondTime)) {
                return 0;
            }
            if (firstTime == null) {
                return 1;
            }
            if (secondTime == null) {
                return -1;
            }
            return secondTime.compareTo(firstTime);
        });

        return notifications;
    }

    @Transactional
    public void notifyMaterialUploaded(Material material) {
        if (material == null) {
            return;
        }

        SchoolClass schoolClass = material.getSchoolClass();
        if (schoolClass == null) {
            return;
        }

        String subjectName = material.getSubject() != null ? material.getSubject().getName() : "your class";
        String materialTitle = material.getTitle() != null && !material.getTitle().isBlank()
            ? material.getTitle()
            : "New resource";

        createNotificationForClass(
            schoolClass,
            "New Material Uploaded",
            materialTitle + " for " + subjectName + " is now available.",
            NotificationType.SYSTEM,
            null,
            null
        );
    }

    private Notification buildBaseNotification(String title, String message, NotificationType type) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setType(type);
        notification.setRead(false);
        notification.setReadAt(null);
        return notification;
    }

    @Transactional(readOnly = true)
    public Optional<Notification> getNotification(Long id) {
        return notificationRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public long getTotalNotificationCount() {
        return notificationRepository.count();
    }

    @Transactional(readOnly = true)
    public long getUnreadNotificationCount(User user) {
        if (user == null) {
            return 0;
        }
        return notificationRepository.countByTargetUserAndReadFalse(user);
    }

    @Transactional(readOnly = true)
    public long getTodayNotificationCount() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return notificationRepository.countByCreatedAtBetween(startOfDay, endOfDay);
    }

    @Transactional
    public void updateNotification(Long id, String title, String message) {
        notificationRepository.findById(id).ifPresent(notification -> {
            String broadcastKey = notification.getBroadcastKey();
            if (broadcastKey != null && !broadcastKey.isBlank()) {
                List<Notification> broadcastNotifications = notificationRepository.findByBroadcastKey(broadcastKey);
                for (Notification broadcastNotification : broadcastNotifications) {
                    broadcastNotification.setTitle(title);
                    broadcastNotification.setMessage(message);
                }
                notificationRepository.saveAll(broadcastNotifications);
            } else {
                notification.setTitle(title);
                notification.setMessage(message);
                notificationRepository.save(notification);
            }
        });
    }

    @Transactional
    public void deleteNotification(Long id) {
        notificationRepository.findById(id).ifPresent(notification -> {
            String broadcastKey = notification.getBroadcastKey();
            if (broadcastKey != null && !broadcastKey.isBlank()) {
                notificationRepository.deleteByBroadcastKey(broadcastKey);
            } else {
                notificationRepository.delete(notification);
            }
        });
    }

    @Transactional
    public void markNotificationAsRead(Long notificationId, User user) {
        if (user == null || notificationId == null) {
            return;
        }

        notificationRepository.findById(notificationId).ifPresent(notification -> {
            User targetUser = notification.getTargetUser();
            if (targetUser == null || targetUser.getId() == null) {
                return;
            }

            if (!targetUser.getId().equals(user.getId())) {
                return;
            }

            if (!notification.isRead()) {
                notification.setRead(true);
                notification.setReadAt(LocalDateTime.now());
                notificationRepository.save(notification);
            }
        });
    }

    @Transactional(readOnly = true)
    public long countUnreadNotifications(User user) {
        if (user == null) {
            return 0;
        }
        return notificationRepository.countByTargetUserAndReadFalse(user);
    }

    @Transactional
    public void markAllAsRead(User user) {
        if (user == null) {
            return;
        }

        List<Notification> notifications = notificationRepository.findByTargetUser(user);
        boolean updated = false;
        LocalDateTime now = LocalDateTime.now();
        for (Notification notification : notifications) {
            if (!notification.isRead()) {
                notification.setRead(true);
                notification.setReadAt(now);
                updated = true;
            }
        }

        if (updated) {
            notificationRepository.saveAll(notifications);
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsCreatedBy(User user) {
        if (user == null) {
            return List.of();
        }
        return notificationRepository.findByCreatedByOrderByCreatedAtDesc(user);
    }


    private void createNotificationForUser(User recipient,
                                           SchoolClass schoolClass,
                                           String title,
                                           String message,
                                           NotificationType type,
                                           String roleTag,
                                           User createdBy,
                                           String broadcastKey,
                                           String fileName,
                                           String originalFileName,
                                           String filePath,
                                           String fileType,
                                           Long fileSize) {
        if (recipient == null) {
            return;
        }

        Notification notification = buildBaseNotification(title, message, type);
        notification.setTargetUser(recipient);
        notification.setTargetClass(schoolClass);
        notification.setTargetRole(roleTag);
        notification.setCreatedBy(createdBy);
        notification.setBroadcastKey(broadcastKey);
        setFileInfo(notification, fileName, originalFileName, filePath, fileType, fileSize);
        notificationRepository.save(notification);
    }

    @Transactional
    public void createNotificationForStudentsInClasses(List<Long> classIds,
                                                      String title,
                                                      String message,
                                                      NotificationType type,
                                                      User createdBy,
                                                      String broadcastKey) {
        if (classIds == null || classIds.isEmpty()) {
            return;
        }

        for (Long classId : classIds) {
            // Get all students in this specific class
            List<User> studentsInClass = userRepository.findBySchoolClassIdAndRole(classId, Role.STUDENT);
            
            for (User student : studentsInClass) {
                createNotificationForUser(student, student.getSchoolClass(), title, message, type, Role.STUDENT.name(), createdBy, broadcastKey, null, null, null, null, null);
            }
        }
    }

    @Transactional
    public void createNotificationForStudentsInClassesAndSubjects(List<Long> classIds,
                                                                  List<Long> subjectIds,
                                                                  String title,
                                                                  String message,
                                                                  NotificationType type,
                                                                  User createdBy,
                                                                  String broadcastKey,
                                                                  String fileName,
                                                                  String originalFileName,
                                                                  String filePath,
                                                                  String fileType,
                                                                  Long fileSize) {
        if (classIds == null || classIds.isEmpty() || subjectIds == null || subjectIds.isEmpty()) {
            return;
        }

        for (Long classId : classIds) {
            // Get students in this specific class who are enrolled in the selected subjects
            List<User> studentsInClassAndSubjects = userRepository.findBySchoolClassIdAndRoleAndSubjectIds(classId, Role.STUDENT, subjectIds);
            
            for (User student : studentsInClassAndSubjects) {
                createNotificationForUser(student, student.getSchoolClass(), title, message, type, Role.STUDENT.name(), createdBy, broadcastKey, fileName, originalFileName, filePath, fileType, fileSize);
            }
        }
    }

    private void setFileInfo(Notification notification, String fileName, String originalFileName, String filePath, String fileType, Long fileSize) {
        if (fileName != null) {
            notification.setFileName(fileName);
            notification.setOriginalFileName(originalFileName);
            notification.setFilePath(filePath);
            notification.setFileType(fileType);
            notification.setFileSize(fileSize);
        }
    }

}
