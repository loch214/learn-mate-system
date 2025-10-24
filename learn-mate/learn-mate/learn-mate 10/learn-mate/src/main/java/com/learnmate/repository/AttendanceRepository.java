package com.learnmate.repository;

import com.learnmate.model.Attendance;
import com.learnmate.model.SchoolClass;
import com.learnmate.model.Subject;
import com.learnmate.model.Timetable;
import com.learnmate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByStudent(User student);
    List<Attendance> findByTimetableAndDate(Timetable timetable, LocalDate date);
    List<Attendance> findByDate(LocalDate date);
    
    // Custom queries for school class-based attendance (when attendance is marked without specific timetable)
    @Query("SELECT a FROM Attendance a WHERE a.timetable IS NULL AND a.date = :date AND a.student.schoolClass = :schoolClass")
    List<Attendance> findBySchoolClassAndDate(@Param("schoolClass") SchoolClass schoolClass, @Param("date") LocalDate date);
    
    @Modifying
    @Query("DELETE FROM Attendance a WHERE a.timetable IS NULL AND a.date = :date AND a.student.schoolClass = :schoolClass")
    void deleteBySchoolClassAndDate(@Param("schoolClass") SchoolClass schoolClass, @Param("date") LocalDate date);
    
    // New methods for enhanced attendance tracking
    List<Attendance> findByTeacher(User teacher);
    
        @Query("""
                SELECT a FROM Attendance a
                WHERE a.schoolClass = :schoolClass
                    AND ((:subject IS NULL AND a.subject IS NULL) OR a.subject = :subject)
                    AND a.date = :date
        """)
        List<Attendance> findBySchoolClassAndSubjectAndDate(@Param("schoolClass") SchoolClass schoolClass,
                                                                                                                @Param("subject") Subject subject,
                                                                                                                @Param("date") LocalDate date);

        @Modifying(clearAutomatically = true)
        @Query("""
                DELETE FROM Attendance a
                WHERE a.schoolClass = :schoolClass
                    AND ((:subject IS NULL AND a.subject IS NULL) OR a.subject = :subject)
                    AND a.date = :date
        """)
        void deleteBySchoolClassAndSubjectAndDate(@Param("schoolClass") SchoolClass schoolClass,
                                                                                            @Param("subject") Subject subject,
                                                                                            @Param("date") LocalDate date);

    List<Attendance> findBySchoolClassIn(Collection<SchoolClass> schoolClasses);
}
