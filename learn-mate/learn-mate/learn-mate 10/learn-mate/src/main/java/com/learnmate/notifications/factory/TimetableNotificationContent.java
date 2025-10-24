package com.learnmate.notifications.factory;

import com.learnmate.model.Timetable;

public class TimetableNotificationContent implements NotificationContent {

    private final Timetable timetable;

    public TimetableNotificationContent(Timetable timetable) {
        this.timetable = timetable;
    }

    @Override
    public String getMessage() {
        if (timetable == null) {
            return "Timetable Update: Please check the latest schedule.";
        }
        return "Timetable Update: The schedule for your class has been changed.";
    }
}
