package com.learnmate.notifications.factory;

import com.learnmate.model.Exam;
import com.learnmate.model.Timetable;
import org.springframework.stereotype.Component;

@Component
public class NotificationFactory {

    public NotificationContent createNotificationContent(String type, Object data) {
        if ("EXAM".equalsIgnoreCase(type)) {
            if (!(data instanceof Exam exam)) {
                throw new IllegalArgumentException("Expected Exam data for EXAM notifications");
            }
            return new ExamNotificationContent(exam);
        } else if ("TIMETABLE".equalsIgnoreCase(type)) {
            if (!(data instanceof Timetable timetable)) {
                throw new IllegalArgumentException("Expected Timetable data for TIMETABLE notifications");
            }
            return new TimetableNotificationContent(timetable);
        }

        throw new IllegalArgumentException("Unsupported notification type: " + type);
    }
}
