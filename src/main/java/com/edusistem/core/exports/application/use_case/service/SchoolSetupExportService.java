package com.edusistem.core.exports.application.use_case.service;

import com.edusistem.core.academic.domain.entity.AcademicPeriod;
import com.edusistem.core.academic.domain.outputports.AcademicPeriodRepositoryPort;
import com.edusistem.core.academic.domain.outputports.GradeRepositoryPort;
import com.edusistem.core.academic.domain.outputports.GroupRepositoryPort;
import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.academic.domain.outputports.TeachingPeriodScheduleRepositoryPort;
import com.edusistem.core.academic.domain.vo.GroupView;
import com.edusistem.core.academic.domain.vo.ScheduledClassView;
import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.activity.domain.inputports.GradeActivityUseCase;
import com.edusistem.core.activity.domain.outputports.ActivityRepositoryPort;
import com.edusistem.core.activity.domain.vo.ActivityView;
import com.edusistem.core.activity.domain.vo.StudentGradeView;
import com.edusistem.core.attendance.domain.entity.AttendanceRecord;
import com.edusistem.core.attendance.domain.outputports.AttendanceRecordRepositoryPort;
import com.edusistem.core.attendance.domain.outputports.AttendanceSessionRepositoryPort;
import com.edusistem.core.attendance.domain.vo.AttendanceSessionView;
import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.evaluation.domain.entity.EvaluationCategory;
import com.edusistem.core.evaluation.domain.enums.EvaluationCategoryCode;
import com.edusistem.core.evaluation.domain.outputports.EvaluationCategoryRepositoryPort;
import com.edusistem.core.exports.domain.inputports.ExportSchoolSetupUseCase;
import com.edusistem.core.exports.domain.vo.ExportedFile;
import com.edusistem.core.grading.domain.entity.GradingConfiguration;
import com.edusistem.core.grading.domain.entity.GradingScale;
import com.edusistem.core.grading.domain.entity.GradingWeight;
import com.edusistem.core.grading.domain.outputports.GradingConfigurationRepositoryPort;
import com.edusistem.core.grading.domain.outputports.GradingScaleRepositoryPort;
import com.edusistem.core.shared.domain.outputports.SpreadsheetWriterPort;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
import com.edusistem.core.shared.domain.vo.SpreadsheetVocabulary;
import com.edusistem.core.shared.domain.vo.TabularData;
import com.edusistem.core.student.domain.entity.Student;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;
import com.edusistem.core.subject.domain.entity.Subject;
import com.edusistem.core.subject.domain.outputports.SubjectRepositoryPort;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Exporta toda la configuración del profesor autenticado en el libro de {@link SchoolSetupSheets}. Todo se consulta
 * por {@code teacherId}: nunca incluye datos de otro profesor. Los estudiantes sin número de identificación no se
 * incluyen (el importador los identifica por ese número), ni sus notas o asistencia.
 */
public class SchoolSetupExportService implements ExportSchoolSetupUseCase {

    private static final int PAGE = PageQuery.MAX_SIZE;

    private final AcademicPeriodRepositoryPort academicPeriods;
    private final GradeRepositoryPort grades;
    private final SubjectRepositoryPort subjects;
    private final GroupRepositoryPort groups;
    private final TeachingPeriodRepositoryPort teachingPeriods;
    private final GradingConfigurationRepositoryPort configurations;
    private final GradingScaleRepositoryPort scales;
    private final EvaluationCategoryRepositoryPort categories;
    private final TeachingPeriodScheduleRepositoryPort schedules;
    private final StudentRepositoryPort students;
    private final ActivityRepositoryPort activities;
    private final GradeActivityUseCase gradeActivity;
    private final AttendanceSessionRepositoryPort sessions;
    private final AttendanceRecordRepositoryPort records;
    private final SpreadsheetWriterPort writer;
    private final RecordAuditUseCase audit;

    public SchoolSetupExportService(AcademicPeriodRepositoryPort academicPeriods, GradeRepositoryPort grades,
                                    SubjectRepositoryPort subjects, GroupRepositoryPort groups,
                                    TeachingPeriodRepositoryPort teachingPeriods,
                                    GradingConfigurationRepositoryPort configurations, GradingScaleRepositoryPort scales,
                                    EvaluationCategoryRepositoryPort categories,
                                    TeachingPeriodScheduleRepositoryPort schedules, StudentRepositoryPort students,
                                    ActivityRepositoryPort activities, GradeActivityUseCase gradeActivity,
                                    AttendanceSessionRepositoryPort sessions, AttendanceRecordRepositoryPort records,
                                    SpreadsheetWriterPort writer, RecordAuditUseCase audit) {
        this.academicPeriods = academicPeriods;
        this.grades = grades;
        this.subjects = subjects;
        this.groups = groups;
        this.teachingPeriods = teachingPeriods;
        this.configurations = configurations;
        this.scales = scales;
        this.categories = categories;
        this.schedules = schedules;
        this.students = students;
        this.activities = activities;
        this.gradeActivity = gradeActivity;
        this.sessions = sessions;
        this.records = records;
        this.writer = writer;
        this.audit = audit;
    }

    @Override
    public ExportedFile schoolSetup(Long teacherId) {
        List<GradingScale> visibleScales = scales.findVisibleTo(teacherId);
        List<String> scaleOptions = visibleScales.stream().map(GradingScale::displayName).distinct().toList();
        List<GroupView> groupList = all(page -> groups.search(teacherId, null, null, page));
        List<TeachingPeriodView> classes = all(page -> teachingPeriods.findViewsByTeacherId(teacherId, null, null, page));
        classes.sort(Comparator.comparing(TeachingPeriodView::academicYear).thenComparing(TeachingPeriodView::gradeName)
                .thenComparing(TeachingPeriodView::groupName).thenComparing(TeachingPeriodView::subjectName)
                .thenComparing(TeachingPeriodView::startDate));

        Map<Long, List<Student>> rosterByGroup = new HashMap<>();
        for (GroupView group : groupList) {
            rosterByGroup.put(group.id(), students.findActiveByGroupId(group.id()).stream()
                    .filter(s -> s.getIdentificationNumber() != null).toList());
        }

        List<List<Object>> activityRows = new ArrayList<>();
        List<List<Object>> gradeRows = new ArrayList<>();
        List<List<Object>> attendanceRows = new ArrayList<>();
        List<List<Object>> scheduleRows = new ArrayList<>();
        for (TeachingPeriodView tp : classes) {
            Map<Long, String> identificationById = rosterByGroup.getOrDefault(tp.groupId(), List.of()).stream()
                    .collect(Collectors.toMap(Student::getId, Student::getIdentificationNumber));
            schedules.findViewsByTeachingPeriodId(tp.id()).stream()
                    .sorted(Comparator.comparing(ScheduledClassView::dayOfWeek).thenComparing(ScheduledClassView::startTime))
                    .forEach(s -> scheduleRows.add(row(tp, SpreadsheetVocabulary.dayLabel(s.dayOfWeek()), s.startTime(),
                            s.endTime(), s.room())));
            for (ActivityView a : all(page -> activities.findViewsByTeachingPeriodId(tp.id(), page))) {
                activityRows.add(row(tp, a.name(), a.description(), a.evaluationDate(), a.maximumScore(),
                        a.activityType()));
                for (StudentGradeView g : gradeActivity.listGrades(teacherId, a.activityId())) {
                    String identification = identificationById.get(g.studentId());
                    if (g.grade() != null && identification != null) {
                        gradeRows.add(row(tp, a.name(), identification, g.grade()));
                    }
                }
            }
            List<AttendanceSessionView> sessionList = all(page -> sessions.findViewsByTeachingPeriodId(tp.id(), page));
            sessionList.sort(Comparator.comparing(AttendanceSessionView::sessionDate));
            for (AttendanceSessionView session : sessionList) {
                for (AttendanceRecord r : records.findBySessionId(session.sessionId())) {
                    String identification = identificationById.get(r.getStudentId());
                    if (identification != null && r.getStatus() != null) {
                        attendanceRows.add(row(tp, session.sessionDate(), identification,
                                SpreadsheetVocabulary.attendanceLabel(r.getStatus().name())));
                    }
                }
            }
        }

        byte[] content = writer.writeWorkbook(List.of(
                SchoolSetupSheets.instructions(),
                table(SchoolSetupSheets.ACADEMIC_PERIODS, periodRows(teacherId)),
                table(SchoolSetupSheets.ACADEMIC_GRADES, grades.findByTeacherIdOrderedByName(teacherId).stream()
                        .map(g -> values(g.getName(), g.getDescription())).toList()),
                table(SchoolSetupSheets.SUBJECTS, all(page -> subjects.search(teacherId, null, page)).stream()
                        .sorted(Comparator.comparing(Subject::getName))
                        .map(s -> values(s.getName(), s.getDescription())).toList()),
                table(SchoolSetupSheets.GROUPS, groupList.stream()
                        .sorted(Comparator.comparing(GroupView::academicYear).thenComparing(GroupView::gradeName)
                                .thenComparing(GroupView::name))
                        .map(g -> values(g.gradeName(), g.name(), g.academicYear())).toList()),
                SchoolSetupSheets.table(SchoolSetupSheets.CLASSES, classRows(classes, visibleScales), scaleOptions),
                table(SchoolSetupSheets.SCHEDULES, scheduleRows),
                table(SchoolSetupSheets.STUDENTS, studentRows(groupList, rosterByGroup)),
                table(SchoolSetupSheets.ACTIVITIES, activityRows),
                table(SchoolSetupSheets.ACTIVITY_GRADES, gradeRows),
                table(SchoolSetupSheets.ATTENDANCE, attendanceRows)));
        audit.success(teacherId, AuditAction.EXPORT, "SchoolSetup", null,
                classes.size() + " classes, " + rosterByGroup.values().stream().mapToInt(List::size).sum() + " enrollments");
        return new ExportedFile("configuracion-escolar.xlsx", content);
    }

    private List<List<Object>> periodRows(Long teacherId) {
        return all(page -> academicPeriods.findByTeacherId(teacherId, page)).stream()
                .sorted(Comparator.comparing(AcademicPeriod::getStartDate).thenComparing(AcademicPeriod::getName))
                .map(p -> values(p.getName(), p.getStartDate(), p.getEndDate())).toList();
    }

    private List<List<Object>> classRows(List<TeachingPeriodView> classes, List<GradingScale> visibleScales) {
        Map<Long, GradingScale> scaleById = visibleScales.stream()
                .collect(Collectors.toMap(GradingScale::getId, Function.identity()));
        Map<Long, EvaluationCategoryCode> codeByCategoryId = new HashMap<>();
        for (EvaluationCategory category : categories.findAll()) {
            Arrays.stream(EvaluationCategoryCode.values()).filter(c -> c.name().equals(category.getName())).findFirst()
                    .ifPresent(code -> codeByCategoryId.put(category.getId(), code));
        }
        List<List<Object>> rows = new ArrayList<>();
        for (TeachingPeriodView tp : classes) {
            GradingConfiguration configuration = configurations.findByTeachingPeriodId(tp.id()).orElse(null);
            if (configuration == null) {
                rows.add(row(tp, null, null, null, null, null));
                continue;
            }
            Map<EvaluationCategoryCode, BigDecimal> weights = new HashMap<>();
            for (GradingWeight w : configuration.getWeights()) {
                EvaluationCategoryCode code = codeByCategoryId.get(w.getEvaluationCategoryId());
                if (code != null) {
                    weights.put(code, w.getWeight());
                }
            }
            GradingScale scale = scaleById.get(configuration.getGradingScaleId());
            rows.add(row(tp, scale == null ? null : scale.displayName(), configuration.getPassingGrade(),
                    weights.getOrDefault(EvaluationCategoryCode.EXAMS, BigDecimal.ZERO),
                    weights.getOrDefault(EvaluationCategoryCode.ACTIVITIES, BigDecimal.ZERO),
                    weights.getOrDefault(EvaluationCategoryCode.ATTENDANCE, BigDecimal.ZERO)));
        }
        return rows;
    }

    private static List<List<Object>> studentRows(List<GroupView> groupList, Map<Long, List<Student>> rosterByGroup) {
        List<List<Object>> rows = new ArrayList<>();
        for (GroupView group : groupList) {
            rosterByGroup.get(group.id()).stream()
                    .sorted(Comparator.comparing(Student::getLastName).thenComparing(Student::getFirstName))
                    .forEach(s -> rows.add(values(s.getIdentificationNumber(), s.getFirstName(), s.getLastName(),
                            s.getEmail(), group.gradeName(), group.name(), group.academicYear())));
        }
        return rows;
    }

    private static TabularData table(String sheetName, List<List<Object>> rows) {
        return SchoolSetupSheets.table(sheetName, rows, List.of());
    }

    /** Fila de una hoja que depende de una clase: las 5 columnas que la identifican y luego {@code values}. */
    private static List<Object> row(TeachingPeriodView tp, Object... values) {
        List<Object> row = new ArrayList<>(List.of(tp.gradeName(), tp.groupName(), tp.academicYear(), tp.subjectName(),
                tp.academicPeriodName()));
        row.addAll(Arrays.asList(values));
        return row;
    }

    /** Como {@code List.of} pero admite celdas vacías (null). */
    private static List<Object> values(Object... values) {
        return Arrays.asList(values);
    }

    private static <T> List<T> all(Function<PageQuery, PageResult<T>> fetch) {
        List<T> list = new ArrayList<>();
        PageResult<T> page;
        int n = 0;
        do {
            page = fetch.apply(new PageQuery(n++, PAGE));
            list.addAll(page.items());
        } while (n < page.totalPages());
        return list;
    }
}
