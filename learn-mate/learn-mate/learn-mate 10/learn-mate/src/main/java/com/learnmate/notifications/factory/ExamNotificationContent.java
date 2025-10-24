package com.learnmate.notifications.factory;

import com.learnmate.model.Exam;
import com.learnmate.model.Subject;

public class ExamNotificationContent implements NotificationContent {

    private final Exam exam;

    public ExamNotificationContent(Exam exam) {
        this.exam = exam;
    }

    @Override
    public String getMessage() {
        if (exam == null) {
            return "New Exam Alert: Details will be shared soon.";
        }

        Subject subject = exam.getSubject();
        String subjectName = subject != null && subject.getName() != null && !subject.getName().isBlank()
            ? subject.getName()
            : "your class";

        return "New Exam Alert: An exam for " + subjectName + " has been scheduled.";
    }
}
