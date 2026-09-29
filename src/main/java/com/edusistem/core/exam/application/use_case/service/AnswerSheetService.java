package com.edusistem.core.exam.application.use_case.service;

import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.audit.domain.vo.AuditTarget;
import com.edusistem.core.exam.application.use_case.service.ExamContextLoader.ExamContext;
import com.edusistem.core.exam.domain.inputports.GenerateAnswerSheetUseCase;
import com.edusistem.core.exam.domain.outputports.AnswerSheetRendererPort;
import com.edusistem.core.exam.domain.vo.AnswerSheetData;
import com.edusistem.core.exam.domain.vo.AnswerSheetLayout;
import com.edusistem.core.exam.domain.vo.PdfDocument;
import com.edusistem.core.exam.domain.vo.QrPayload;
import com.edusistem.core.exam.domain.vo.QuestionBookletData;
import com.edusistem.core.shared.domain.exceptions.BusinessRuleException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.student.domain.entity.Student;
import com.edusistem.core.student.domain.outputports.StudentGroupRepositoryPort;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;
import java.util.List;

public class AnswerSheetService implements GenerateAnswerSheetUseCase {

    private final ExamContextLoader loader;
    private final StudentRepositoryPort students;
    private final StudentGroupRepositoryPort studentGroups;
    private final AnswerSheetRendererPort renderer;
    private final RecordAuditUseCase audit;

    public AnswerSheetService(ExamContextLoader loader, StudentRepositoryPort students,
                              StudentGroupRepositoryPort studentGroups, AnswerSheetRendererPort renderer,
                              RecordAuditUseCase audit) {
        this.loader = loader;
        this.students = students;
        this.studentGroups = studentGroups;
        this.renderer = renderer;
        this.audit = audit;
    }

    @Override
    public PdfDocument forStudent(Long teacherId, Long examId, Long studentId, boolean includeQuestions) {
        ExamContext ctx = readyContext(teacherId, examId);
        Student student = students.findById(studentId).orElseThrow(() -> ResourceNotFoundException.of("Student", studentId));
        if (!studentGroups.existsActive(studentId, ctx.period().groupId())) {
            throw ResourceNotFoundException.of("Student", studentId);
        }
        byte[] pdf = renderer.render(List.of(sheetFor(ctx, student)), includeQuestions ? bookletFor(ctx) : null);
        audit.success(teacherId, AuditAction.EXPORT, target("AnswerSheet", examId, ctx,
                AuditTarget.label(ctx.evaluation().getName(), student.getFirstName() + " " + student.getLastName())),
                "student " + student.getStudentCode());
        return new PdfDocument("answer-sheet-exam" + examId + "-" + student.getStudentCode() + ".pdf", pdf);
    }

    @Override
    public PdfDocument forGroup(Long teacherId, Long examId, boolean includeQuestions) {
        ExamContext ctx = readyContext(teacherId, examId);
        List<Student> groupStudents = students.findActiveByGroupId(ctx.period().groupId());
        if (groupStudents.isEmpty()) {
            throw new BusinessRuleException("GROUP_HAS_NO_STUDENTS", "The group has no active students");
        }
        byte[] pdf = renderer.render(groupStudents.stream().map(s -> sheetFor(ctx, s)).toList(),
                includeQuestions ? bookletFor(ctx) : null);
        audit.success(teacherId, AuditAction.EXPORT, target("AnswerSheet", examId, ctx, ctx.evaluation().getName()),
                groupStudents.size() + " sheets");
        return new PdfDocument("answer-sheets-exam" + examId + ".pdf", pdf);
    }

    @Override
    public PdfDocument questionBooklet(Long teacherId, Long examId) {
        ExamContext ctx = readyContext(teacherId, examId);
        byte[] pdf = renderer.renderBooklet(bookletFor(ctx));
        audit.success(teacherId, AuditAction.EXPORT, target("QuestionBooklet", examId, ctx, ctx.evaluation().getName()),
                ctx.evaluation().getName());
        return new PdfDocument("questions-exam" + examId + ".pdf", pdf);
    }

    private static AuditTarget target(String entityType, Long examId, ExamContext ctx, String label) {
        return AuditTarget.inTeachingPeriod(entityType, examId, ctx.evaluation().getTeachingPeriodId(), label);
    }

    private ExamContext readyContext(Long teacherId, Long examId) {
        ExamContext ctx = loader.load(teacherId, examId);
        if (!ctx.exam().isReady()) {
            throw new BusinessRuleException("EXAM_NOT_READY", "Define all the exam questions before generating answer sheets");
        }
        return ctx;
    }

    private static QuestionBookletData bookletFor(ExamContext ctx) {
        var period = ctx.period();
        return new QuestionBookletData(ctx.evaluation().getName(), period.subjectName(),
                period.gradeName() + " " + period.groupName(), ctx.exam().getQuestions());
    }

    private AnswerSheetData sheetFor(ExamContext ctx, Student student) {
        var period = ctx.period();
        return new AnswerSheetData(ctx.evaluation().getName(), period.subjectName(),
                period.gradeName() + " " + period.groupName(), student.getLastName() + " " + student.getFirstName(),
                student.getStudentCode(), QrPayload.encode(ctx.exam().getId(), student.getStudentCode()),
                new AnswerSheetLayout(ctx.exam().getNumberOfQuestions(), ctx.exam().optionCount()));
    }
}
