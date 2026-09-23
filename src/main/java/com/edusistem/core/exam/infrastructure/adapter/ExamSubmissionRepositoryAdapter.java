package com.edusistem.core.exam.infrastructure.adapter;

import com.edusistem.core.exam.domain.entity.ExamAnswer;
import com.edusistem.core.exam.domain.entity.ExamSubmission;
import com.edusistem.core.exam.domain.enums.ExamSubmissionStatus;
import com.edusistem.core.exam.domain.outputports.ExamSubmissionRepositoryPort;
import com.edusistem.core.exam.domain.vo.ExamSubmissionSummary;
import com.edusistem.core.exam.infrastructure.entity.ExamAnswerEntity;
import com.edusistem.core.exam.infrastructure.entity.ExamSubmissionEntity;
import com.edusistem.core.exam.infrastructure.mapper.ExamMapper;
import com.edusistem.core.exam.infrastructure.repository.SpringDataExamAnswerRepository;
import com.edusistem.core.exam.infrastructure.repository.SpringDataExamSubmissionRepository;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.shared.infrastructure.adapter.PageMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ExamSubmissionRepositoryAdapter implements ExamSubmissionRepositoryPort {

    private final SpringDataExamSubmissionRepository submissions;
    private final SpringDataExamAnswerRepository answers;
    private final ExamMapper mapper;

    public ExamSubmissionRepositoryAdapter(SpringDataExamSubmissionRepository submissions,
                                           SpringDataExamAnswerRepository answers, ExamMapper mapper) {
        this.submissions = submissions;
        this.answers = answers;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public ExamSubmission save(ExamSubmission submission) {
        ExamSubmissionEntity saved = submissions.save(mapper.toEntity(submission));
        Set<Long> kept = submission.getAnswers().stream().map(ExamAnswer::getId).filter(id -> id != null)
                .collect(Collectors.toSet());
        List<ExamAnswerEntity> obsolete = answers.findBySubmissionIdOrderByIdAsc(saved.getId()).stream()
                .filter(a -> !kept.contains(a.getId())).toList();
        if (!obsolete.isEmpty()) {
            answers.deleteAllInBatch(obsolete);
            answers.flush();
        }
        for (ExamAnswer answer : submission.getAnswers()) {
            ExamAnswerEntity entity = mapper.toEntity(answer);
            entity.setSubmissionId(saved.getId());
            answers.save(entity);
        }
        return findById(saved.getId()).orElseThrow();
    }

    @Override
    public Optional<ExamSubmission> findById(Long id) {
        return submissions.findById(id).map(this::assemble);
    }

    @Override
    public Optional<ExamSubmission> findByExamIdAndStudentId(Long examId, Long studentId) {
        return submissions.findByExamIdAndStudentId(examId, studentId).map(this::assemble);
    }

    @Override
    public PageResult<ExamSubmissionSummary> findSummariesByExamId(Long examId, ExamSubmissionStatus status, PageQuery page) {
        return PageMapper.toResult(submissions.findSummaries(examId, status, PageMapper.pageable(page)), s -> s);
    }

    private ExamSubmission assemble(ExamSubmissionEntity entity) {
        ExamSubmission submission = mapper.toDomain(entity);
        submission.setAnswers(answers.findBySubmissionIdOrderByIdAsc(entity.getId()).stream().map(mapper::toDomain)
                .collect(Collectors.toCollection(ArrayList::new)));
        return submission;
    }
}
