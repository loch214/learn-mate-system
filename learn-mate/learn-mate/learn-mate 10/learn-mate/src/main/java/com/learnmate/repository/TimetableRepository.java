package com.learnmate.repository;

import com.learnmate.model.SchoolClass;
import com.learnmate.model.Timetable;
import com.learnmate.model.User;
import com.learnmate.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

@Repository
public interface TimetableRepository extends JpaRepository<Timetable, Long> {
    List<Timetable> findBySchoolClass(SchoolClass schoolClass);
    List<Timetable> findByDay(DayOfWeek day);
    List<Timetable> findByTeacher(User teacher);
    List<Timetable> findBySchoolClassAndDay(SchoolClass schoolClass, DayOfWeek day);
    List<Timetable> findBySchoolClassAndDayAndSubject(SchoolClass schoolClass, DayOfWeek day, Subject subject);
    List<Timetable> findBySchoolClassAndSubjectIn(SchoolClass schoolClass, Set<Subject> subjects);
}
