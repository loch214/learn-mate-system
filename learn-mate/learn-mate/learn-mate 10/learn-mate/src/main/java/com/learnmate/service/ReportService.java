package com.learnmate.service;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.learnmate.model.Attendance;
import com.learnmate.model.Fee;
import com.learnmate.model.Mark;
import com.learnmate.model.Exam;
import com.learnmate.model.SchoolClass;
import com.learnmate.model.Subject;
import com.learnmate.model.User;
import com.opencsv.CSVWriter;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class ReportService {
    private final AttendanceService attendanceService;
    private final MarkService markService;
    private final FeeService feeService;

    public ReportService(AttendanceService attendanceService, MarkService markService, FeeService feeService) {
        this.attendanceService = attendanceService;
        this.markService = markService;
        this.feeService = feeService;
    }

    public byte[] generateAttendanceReportPDF() {
        List<Attendance> attendances = attendanceService.getAllAttendances();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();
            document.add(new Paragraph("Attendance Report"));
            for (Attendance a : attendances) {
                document.add(new Paragraph("Student: " + a.getStudent().getName() + ", Date: " + a.getDate() + ", Present: " + a.isPresent()));
            }
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    public String generateAttendanceReportCSV() {
        List<Attendance> attendances = attendanceService.getAllAttendances();
        StringWriter sw = new StringWriter();
        try (CSVWriter csvWriter = new CSVWriter(sw)) {
            csvWriter.writeNext(new String[]{"Student", "Date", "Present"});
            for (Attendance a : attendances) {
                csvWriter.writeNext(new String[]{a.getStudent().getName(), a.getDate().toString(), String.valueOf(a.isPresent())});
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sw.toString();
    }

    // Similar methods for marks and fees
    public byte[] generateMarksReportPDF() {
        List<Mark> marks = markService.getAllMarks();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();
            document.add(new Paragraph("Marks Report"));
            document.add(Chunk.NEWLINE);

            if (marks.isEmpty()) {
                document.add(new Paragraph("No marks available."));
            } else {
                Map<String, Map<String, Map<String, List<Mark>>>> grouped = groupMarksByClassAndSubject(marks);

                for (Map.Entry<String, Map<String, Map<String, List<Mark>>>> classEntry : grouped.entrySet()) {
                    document.add(new Paragraph("Class: " + classEntry.getKey()));
                    document.add(Chunk.NEWLINE);

                    for (Map.Entry<String, Map<String, List<Mark>>> subjectEntry : classEntry.getValue().entrySet()) {
                        document.add(new Paragraph("  Subject: " + subjectEntry.getKey()));

                        for (Map.Entry<String, List<Mark>> examEntry : subjectEntry.getValue().entrySet()) {
                            document.add(new Paragraph("    Exam: " + examEntry.getKey()));

                            for (Mark mark : examEntry.getValue()) {
                                String studentLine = buildStudentLine(mark);
                                document.add(new Paragraph("      " + studentLine));
                                if (mark.getComments() != null && !mark.getComments().trim().isEmpty()) {
                                    document.add(new Paragraph("        Comments: " + mark.getComments()));
                                }
                            }

                            document.add(Chunk.NEWLINE);
                        }
                    }
                    document.add(new Paragraph("--------------------------------------------"));
                    document.add(Chunk.NEWLINE);
                }
            }
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    public String generateMarksReportCSV() {
        List<Mark> marks = markService.getAllMarks();
        StringWriter sw = new StringWriter();
        try (CSVWriter csvWriter = new CSVWriter(sw)) {
            csvWriter.writeNext(new String[]{"Class", "Subject", "Exam", "Exam Date", "Student", "Username", "Score", "Max Marks", "Published", "Comments"});

            if (!marks.isEmpty()) {
                Map<String, Map<String, Map<String, List<Mark>>>> grouped = groupMarksByClassAndSubject(marks);
                DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_DATE;

                for (Map.Entry<String, Map<String, Map<String, List<Mark>>>> classEntry : grouped.entrySet()) {
                    for (Map.Entry<String, Map<String, List<Mark>>> subjectEntry : classEntry.getValue().entrySet()) {
                        for (Map.Entry<String, List<Mark>> examEntry : subjectEntry.getValue().entrySet()) {
                            for (Mark mark : examEntry.getValue()) {
                                Exam exam = mark.getExam();
                                String examDate = exam != null && exam.getDate() != null ? exam.getDate().format(dateFormatter) : "";
                                String maxMarks = exam != null && exam.getMaxMarks() != null ? String.valueOf(exam.getMaxMarks()) : "";
                                csvWriter.writeNext(new String[]{
                                    classEntry.getKey(),
                                    subjectEntry.getKey(),
                                    examEntry.getKey(),
                                    examDate,
                                    mark.getStudent().getName(),
                                    mark.getStudent().getUsername(),
                                    String.valueOf(mark.getScore()),
                                    maxMarks,
                                    mark.isPublished() ? "Yes" : "No",
                                    mark.getComments() != null ? mark.getComments() : ""
                                });
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sw.toString();
    }

    public byte[] generateFeesReportPDF() {
        List<Fee> fees = feeService.getAllFees();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();
            document.add(new Paragraph("Fees Report"));
            for (Fee f : fees) {
                document.add(new Paragraph("Student: " + f.getStudent().getName() + ", Amount: " + f.getAmount() + ", Status: " + f.getStatus()));
            }
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    public String generateFeesReportCSV() {
        List<Fee> fees = feeService.getAllFees();
        StringWriter sw = new StringWriter();
        try (CSVWriter csvWriter = new CSVWriter(sw)) {
            csvWriter.writeNext(new String[]{"Student", "Amount", "Status"});
            for (Fee f : fees) {
                csvWriter.writeNext(new String[]{f.getStudent().getName(), String.valueOf(f.getAmount()), f.getStatus()});
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sw.toString();
    }

    public byte[] generateParentMarksReportPDF(User parent) {
        if (parent == null) {
            throw new IllegalArgumentException("Parent user is required to generate the report");
        }

        Set<User> children = parent.getChildren();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            addParagraph(document, "Academic Report");
            addParagraph(document, "Parent: " + parent.getName() + " (" + parent.getUsername() + ")");
            addParagraph(document, "Generated on: " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(LocalDateTime.now()));
            addNewLine(document);

            if (children == null || children.isEmpty()) {
                addParagraph(document, "No students are linked to your account. Please contact the school administration for assistance.");
                document.close();
                return baos.toByteArray();
            }

            List<Mark> marks = markService.getMarksForStudents(children).stream()
                    .filter(mark -> mark != null && mark.isPublished())
                    .collect(Collectors.toList());

            if (marks.isEmpty()) {
                addParagraph(document, "No published results are available for your linked students at this time.");
                document.close();
                return baos.toByteArray();
            }

            Map<User, List<Mark>> marksByStudent = marks.stream()
                    .collect(Collectors.groupingBy(Mark::getStudent));

            Comparator<String> caseInsensitive = String.CASE_INSENSITIVE_ORDER;
            Comparator<Mark> markComparator = buildParentMarkComparator();

            marksByStudent.entrySet().stream()
                    .sorted(Comparator.comparing(entry -> entry.getKey().getName(), caseInsensitive))
                    .forEach(entry -> {
                        User student = entry.getKey();
                        addParagraph(document, student.getName() + " (" + student.getUsername() + ")");
                        addNewLine(document);

                        entry.getValue().stream()
                                .sorted(markComparator)
                                .forEach(mark -> {
                                    String subjectName = resolveSubjectName(mark);
                                    String examLabel = resolveExamLabel(mark);
                                    String scoreLine = String.format("Subject: %s | Exam: %s | Score: %d/%s",
                                            subjectName,
                                            examLabel,
                                            mark.getScore(),
                                            resolveMaxMarks(mark));
                                    addParagraph(document, "  " + scoreLine);
                                    if (mark.getComments() != null && !mark.getComments().isBlank()) {
                                        addParagraph(document, "    Teacher note: " + mark.getComments());
                                    }
                                });

                        addNewLine(document);
                    });

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating parent marks PDF", e);
        }
    }

    private Map<String, Map<String, Map<String, List<Mark>>>> groupMarksByClassAndSubject(List<Mark> marks) {
        Comparator<String> caseInsensitive = String.CASE_INSENSITIVE_ORDER;

        return marks.stream()
            .collect(Collectors.groupingBy(
                this::resolveClassName,
                () -> new TreeMap<>(caseInsensitive),
                Collectors.groupingBy(
                    this::resolveSubjectName,
                    () -> new TreeMap<>(caseInsensitive),
                    Collectors.groupingBy(
                        this::resolveExamLabel,
                        () -> new TreeMap<>(caseInsensitive),
                        Collectors.collectingAndThen(Collectors.toList(), list -> {
                            list.sort(Comparator.comparing(m -> m.getStudent().getName(), caseInsensitive));
                            return list;
                        })
                    )
                )
            ));
    }

    private String resolveClassName(Mark mark) {
        Exam exam = mark.getExam();
        if (exam != null) {
            SchoolClass examClass = exam.getSchoolClass();
            if (examClass != null && examClass.getName() != null) {
                return examClass.getName();
            }
            if (exam.getGrade() != null && !exam.getGrade().isBlank()) {
                return exam.getGrade();
            }
        }

        SchoolClass studentClass = mark.getStudent() != null ? mark.getStudent().getSchoolClass() : null;
        if (studentClass != null && studentClass.getName() != null) {
            return studentClass.getName();
        }

        return "Unassigned Class";
    }

    private String resolveSubjectName(Mark mark) {
        Exam exam = mark.getExam();
        if (exam != null) {
            Subject subject = exam.getSubject();
            if (subject != null && subject.getName() != null) {
                return subject.getName();
            }
        }
        return "Unknown Subject";
    }

    private String resolveExamLabel(Mark mark) {
        Exam exam = mark.getExam();
        if (exam == null) {
            return "Unspecified Exam";
        }

        StringBuilder label = new StringBuilder();
        if (exam.getDate() != null) {
            label.append(exam.getDate().format(DateTimeFormatter.ISO_DATE)).append(" - ");
        }

        if (exam.getTitle() != null && !exam.getTitle().isBlank()) {
            label.append(exam.getTitle());
        } else if (exam.getSubject() != null && exam.getSubject().getName() != null) {
            label.append(exam.getSubject().getName()).append(" Exam");
        } else {
            label.append("Exam #").append(exam.getId());
        }

        return label.toString();
    }

    private String buildStudentLine(Mark mark) {
        Exam exam = mark.getExam();
        String maxMarks = exam != null && exam.getMaxMarks() != null ? String.valueOf(exam.getMaxMarks()) : "N/A";
        String publishedTag = mark.isPublished() ? "[Published]" : "[Draft]";
        return String.format("%s (%s) - %d/%s %s",
            mark.getStudent().getName(),
            mark.getStudent().getUsername(),
            mark.getScore(),
            maxMarks,
            publishedTag);
    }

    private Comparator<Mark> buildParentMarkComparator() {
        Comparator<LocalDate> dateComparator = Comparator.nullsLast(Comparator.naturalOrder());
        Comparator<String> stringComparator = Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);

        return Comparator
                .comparing((Mark mark) -> {
                    Exam exam = mark.getExam();
                    return exam != null ? exam.getDate() : null;
                }, dateComparator)
                .thenComparing(mark -> resolveSubjectName(mark), stringComparator)
                .thenComparing(mark -> resolveExamLabel(mark), stringComparator);
    }

    private String resolveMaxMarks(Mark mark) {
        Exam exam = mark.getExam();
        return exam != null && exam.getMaxMarks() != null ? String.valueOf(exam.getMaxMarks()) : "N/A";
    }

    private void addParagraph(Document document, String text) {
        try {
            document.add(new Paragraph(text));
        } catch (Exception e) {
            throw new RuntimeException("Error writing to PDF", e);
        }
    }

    private void addNewLine(Document document) {
        try {
            document.add(Chunk.NEWLINE);
        } catch (Exception e) {
            throw new RuntimeException("Error writing to PDF", e);
        }
    }
}

