package com.edusistem.core.exports.application.use_case.service;

import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.activity.domain.inputports.GradeActivityUseCase;
import com.edusistem.core.activity.domain.outputports.ActivityRepositoryPort;
import com.edusistem.core.activity.domain.vo.ActivityView;
import com.edusistem.core.activity.domain.vo.StudentGradeView;
import com.edusistem.core.attendance.domain.entity.AttendanceRecord;
import com.edusistem.core.attendance.domain.enums.AttendanceStatus;
import com.edusistem.core.attendance.domain.outputports.AttendanceRecordRepositoryPort;
import com.edusistem.core.attendance.domain.outputports.AttendanceSessionRepositoryPort;
import com.edusistem.core.attendance.domain.vo.AttendanceSessionView;
import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.evaluation.domain.entity.Evaluation;
import com.edusistem.core.evaluation.domain.outputports.EvaluationRepositoryPort;
import com.edusistem.core.exports.domain.inputports.ExportUseCase;
import com.edusistem.core.exports.domain.vo.ExportedFile;
import com.edusistem.core.grading.domain.inputports.CalculatePeriodGradeUseCase;
import com.edusistem.core.grading.domain.outputports.EvaluationResultsPort;
import com.edusistem.core.grading.domain.vo.EvaluationResult;
import com.edusistem.core.grading.domain.vo.PeriodGradeReport;
import com.edusistem.core.grading.domain.vo.StudentPeriodGrade;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.domain.exceptions.BusinessRuleException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.shared.domain.outputports.SpreadsheetWriterPort;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.shared.domain.vo.PeriodWorkbookColumns;
import com.edusistem.core.shared.domain.vo.TabularData;
import com.edusistem.core.student.domain.entity.Student;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Exporta a Excel. Todo dato se obtiene a través del contexto académico del profesor autenticado: nunca se exporta
 * información de un teaching period o grupo que no le pertenece.
 */
public class ExportService implements ExportUseCase {

    private static final int PAGE = PageQuery.MAX_SIZE;

    private final StudentRepositoryPort students;
    private final TeachingPeriodRepositoryPort teachingPeriods;
    private final EvaluationRepositoryPort evaluations;
    private final EvaluationResultsPort results;
    private final CalculatePeriodGradeUseCase periodGrades;
    private final AttendanceSessionRepositoryPort sessions;
    private final AttendanceRecordRepositoryPort records;
    private final ActivityRepositoryPort activities;
    private final GradeActivityUseCase gradeActivity;
    private final SpreadsheetWriterPort writer;
    private final OwnershipGuard guard;
    private final RecordAuditUseCase audit;

    public ExportService(StudentRepositoryPort students, TeachingPeriodRepositoryPort teachingPeriods,
                         EvaluationRepositoryPort evaluations, EvaluationResultsPort results,
                         CalculatePeriodGradeUseCase periodGrades, AttendanceSessionRepositoryPort sessions,
                         AttendanceRecordRepositoryPort records, ActivityRepositoryPort activities,
                         GradeActivityUseCase gradeActivity, SpreadsheetWriterPort writer, OwnershipGuard guard,
                         RecordAuditUseCase audit) {
        this.students = students;
        this.teachingPeriods = teachingPeriods;
        this.evaluations = evaluations;
        this.results = results;
        this.periodGrades = periodGrades;
        this.sessions = sessions;
        this.records = records;
        this.activities = activities;
        this.gradeActivity = gradeActivity;
        this.writer = writer;
        this.guard = guard;
        this.audit = audit;
    }

    @Override
    public ExportedFile students(Long teacherId, Long groupId, Long teachingPeriodId) {
        List<Student> list;
        if (teachingPeriodId != null) {
            list = students.findActiveByGroupId(period(teacherId, teachingPeriodId).groupId());
        } else if (groupId != null) {
            guard.requireGroup(teacherId, groupId);
            list = students.findActiveByGroupId(groupId);
        } else {
            list = new ArrayList<>();
            PageResult<Student> page;
            int n = 0;
            do {
                page = students.searchByTeacher(teacherId, null, null, new PageQuery(n++, PAGE));
                list.addAll(page.items());
            } while (n < page.totalPages());
        }
        List<List<Object>> rows = list.stream().map(s -> List.<Object>of(s.getStudentCode(),
                nullToEmpty(s.getIdentificationNumber()), s.getFirstName(), s.getLastName(), nullToEmpty(s.getEmail()))).toList();
        byte[] content = writer.write(new TabularData("Students",
                List.of("student_code", "identification_number", "first_name", "last_name", "email"), rows));
        audit.success(teacherId, AuditAction.EXPORT, "Students", teachingPeriodId, list.size() + " students");
        return new ExportedFile("students.xlsx", content);
    }

    @Override
    public ExportedFile grades(Long teacherId, Long teachingPeriodId) {
        TeachingPeriodView period = period(teacherId, teachingPeriodId);
        List<Student> roster = students.findActiveByGroupId(period.groupId());
        List<Evaluation> evals = evaluations.findByTeachingPeriodId(teachingPeriodId);
        Map<String, EvaluationResult> byKey = results.findByTeachingPeriodId(teachingPeriodId).stream()
                .collect(Collectors.toMap(r -> r.studentId() + ":" + r.evaluationId(), Function.identity(), (a, b) -> a));
        Map<Long, BigDecimal> periodGradeByStudent = periodGradesOrEmpty(teacherId, teachingPeriodId);

        List<String> headers = new ArrayList<>(List.of("student_code", "identification_number", "last_name", "first_name"));
        evals.forEach(e -> headers.add(e.getName() + " (max " + e.getMaximumScore().stripTrailingZeros().toPlainString() + ")"));
        boolean withPeriodGrade = !periodGradeByStudent.isEmpty();
        if (withPeriodGrade) {
            headers.add("period_grade");
        }
        List<List<Object>> rows = new ArrayList<>();
        for (Student s : roster) {
            List<Object> row = new ArrayList<>(List.of(s.getStudentCode(), nullToEmpty(s.getIdentificationNumber()),
                    s.getLastName(), s.getFirstName()));
            for (Evaluation e : evals) {
                EvaluationResult r = byKey.get(s.getId() + ":" + e.getId());
                row.add(r == null || r.excluded() ? null : r.earned());
            }
            if (withPeriodGrade) {
                row.add(periodGradeByStudent.get(s.getId()));
            }
            rows.add(row);
        }
        byte[] content = writer.write(new TabularData("Grades", headers, rows));
        audit.success(teacherId, AuditAction.EXPORT, "Grades", teachingPeriodId, roster.size() + " students");
        return new ExportedFile("grades-teaching-period-" + teachingPeriodId + ".xlsx", content);
    }

    @Override
    public ExportedFile attendance(Long teacherId, Long teachingPeriodId) {
        TeachingPeriodView period = period(teacherId, teachingPeriodId);
        List<Student> roster = students.findActiveByGroupId(period.groupId());
        List<AttendanceSessionView> allSessions = new ArrayList<>();
        PageResult<AttendanceSessionView> page;
        int n = 0;
        do {
            page = sessions.findViewsByTeachingPeriodId(teachingPeriodId, new PageQuery(n++, PAGE));
            allSessions.addAll(page.items());
        } while (n < page.totalPages());
        allSessions.sort(java.util.Comparator.comparing(AttendanceSessionView::sessionDate));

        Map<Long, Map<Long, AttendanceStatus>> statusBySession = new HashMap<>();
        for (AttendanceSessionView s : allSessions) {
            statusBySession.put(s.sessionId(), records.findBySessionId(s.sessionId()).stream()
                    .collect(Collectors.toMap(AttendanceRecord::getStudentId, AttendanceRecord::getStatus)));
        }
        List<String> headers = new ArrayList<>(List.of("student_code", "last_name", "first_name"));
        allSessions.forEach(s -> headers.add(s.sessionDate().toString()));
        headers.addAll(List.of("present", "absent", "excused", "attendance_percent"));

        List<List<Object>> rows = new ArrayList<>();
        for (Student student : roster) {
            List<Object> row = new ArrayList<>(List.of(student.getStudentCode(), student.getLastName(), student.getFirstName()));
            int present = 0;
            int absent = 0;
            int excused = 0;
            for (AttendanceSessionView s : allSessions) {
                AttendanceStatus status = statusBySession.get(s.sessionId()).get(student.getId());
                row.add(status == null ? null : status.name());
                if (status == AttendanceStatus.PRESENT) {
                    present++;
                } else if (status == AttendanceStatus.ABSENT) {
                    absent++;
                } else if (status == AttendanceStatus.EXCUSED) {
                    excused++;
                }
            }
            row.addAll(List.of(present, absent, excused));
            row.add(present + absent == 0 ? null : BigDecimal.valueOf(present * 100.0 / (present + absent))
                    .setScale(1, RoundingMode.HALF_UP));
            rows.add(row);
        }
        byte[] content = writer.write(new TabularData("Attendance", headers, rows));
        audit.success(teacherId, AuditAction.EXPORT, "Attendance", teachingPeriodId, allSessions.size() + " sessions");
        return new ExportedFile("attendance-teaching-period-" + teachingPeriodId + ".xlsx", content);
    }

    @Override
    public ExportedFile full(Long teacherId, Long teachingPeriodId) {
        TeachingPeriodView periodView = period(teacherId, teachingPeriodId);
        List<Student> roster = students.findActiveByGroupId(periodView.groupId());
        List<ActivityView> activityList = allActivities(teachingPeriodId);
        List<AttendanceSessionView> sessionList = allSessions(teachingPeriodId);

        byte[] content = writer.writeWorkbook(List.of(fullStudentsSheet(roster), fullGradesSheet(teacherId, roster, activityList),
                fullAttendanceSheet(roster, sessionList)));
        audit.success(teacherId, AuditAction.EXPORT, "TeachingPeriodFull", teachingPeriodId, roster.size() + " students");
        return new ExportedFile("teaching-period-" + teachingPeriodId + "-full.xlsx", content);
    }

    private TabularData fullStudentsSheet(List<Student> roster) {
        List<List<Object>> rows = roster.stream().map(s -> List.<Object>of(nullToEmpty(s.getIdentificationNumber()),
                s.getFirstName(), s.getLastName(), nullToEmpty(s.getEmail()))).toList();
        return new TabularData("Students", List.of("identification_number", "first_name", "last_name", "email"), rows);
    }

    private TabularData fullGradesSheet(Long teacherId, List<Student> roster, List<ActivityView> activityList) {
        List<String> headers = new ArrayList<>(List.of("identification_number", "last_name", "first_name"));
        activityList.forEach(a -> headers.add(PeriodWorkbookColumns.activityHeader(a.name(), a.activityId(), a.maximumScore())));
        Map<Long, Map<Long, BigDecimal>> gradeByActivityThenStudent = new HashMap<>();
        for (ActivityView a : activityList) {
            gradeByActivityThenStudent.put(a.activityId(), gradeActivity.listGrades(teacherId, a.activityId()).stream()
                    .filter(g -> g.grade() != null)
                    .collect(Collectors.toMap(StudentGradeView::studentId, StudentGradeView::grade)));
        }
        List<List<Object>> rows = new ArrayList<>();
        for (Student s : roster) {
            List<Object> row = new ArrayList<>(List.of(nullToEmpty(s.getIdentificationNumber()), s.getLastName(), s.getFirstName()));
            activityList.forEach(a -> row.add(gradeByActivityThenStudent.get(a.activityId()).get(s.getId())));
            rows.add(row);
        }
        return new TabularData("Grades", headers, rows);
    }

    private TabularData fullAttendanceSheet(List<Student> roster, List<AttendanceSessionView> sessionList) {
        List<String> headers = new ArrayList<>(List.of("identification_number", "last_name", "first_name"));
        sessionList.forEach(s -> headers.add(PeriodWorkbookColumns.sessionHeader(s.sessionDate(), s.sessionId())));
        Map<Long, Map<Long, AttendanceStatus>> statusBySession = new HashMap<>();
        for (AttendanceSessionView s : sessionList) {
            statusBySession.put(s.sessionId(), records.findBySessionId(s.sessionId()).stream()
                    .collect(Collectors.toMap(AttendanceRecord::getStudentId, AttendanceRecord::getStatus)));
        }
        List<List<Object>> rows = new ArrayList<>();
        for (Student student : roster) {
            List<Object> row = new ArrayList<>(List.of(nullToEmpty(student.getIdentificationNumber()), student.getLastName(),
                    student.getFirstName()));
            for (AttendanceSessionView s : sessionList) {
                AttendanceStatus status = statusBySession.get(s.sessionId()).get(student.getId());
                row.add(status == null ? null : status.name());
            }
            rows.add(row);
        }
        return new TabularData("Attendance", headers, rows);
    }

    private List<ActivityView> allActivities(Long teachingPeriodId) {
        List<ActivityView> list = new ArrayList<>();
        PageResult<ActivityView> page;
        int n = 0;
        do {
            page = activities.findViewsByTeachingPeriodId(teachingPeriodId, new PageQuery(n++, PAGE));
            list.addAll(page.items());
        } while (n < page.totalPages());
        return list;
    }

    private List<AttendanceSessionView> allSessions(Long teachingPeriodId) {
        List<AttendanceSessionView> list = new ArrayList<>();
        PageResult<AttendanceSessionView> page;
        int n = 0;
        do {
            page = sessions.findViewsByTeachingPeriodId(teachingPeriodId, new PageQuery(n++, PAGE));
            list.addAll(page.items());
        } while (n < page.totalPages());
        list.sort(java.util.Comparator.comparing(AttendanceSessionView::sessionDate));
        return list;
    }

    private TeachingPeriodView period(Long teacherId, Long teachingPeriodId) {
        guard.requireTeachingPeriod(teacherId, teachingPeriodId);
        return teachingPeriods.findViewById(teachingPeriodId)
                .orElseThrow(() -> ResourceNotFoundException.of("TeachingPeriod", teachingPeriodId));
    }

    /** La nota del periodo solo se incluye si la configuración es válida (pesos = 100%). */
    private Map<Long, BigDecimal> periodGradesOrEmpty(Long teacherId, Long teachingPeriodId) {
        try {
            PeriodGradeReport report = periodGrades.calculate(teacherId, teachingPeriodId);
            return report.students().stream().collect(Collectors.toMap(StudentPeriodGrade::studentId,
                    StudentPeriodGrade::periodGrade));
        } catch (BusinessRuleException e) {
            return Map.of();
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
