package com.edusistem.core.exam.infrastructure.adapter;

import com.edusistem.core.exam.domain.entity.Exam;
import com.edusistem.core.exam.domain.entity.ExamQuestion;
import com.edusistem.core.exam.domain.entity.ExamQuestionOption;
import com.edusistem.core.exam.domain.outputports.ExamRepositoryPort;
import com.edusistem.core.exam.domain.vo.ExamView;
import com.edusistem.core.exam.infrastructure.entity.ExamEntity;
import com.edusistem.core.exam.infrastructure.entity.ExamQuestionEntity;
import com.edusistem.core.exam.infrastructure.entity.ExamQuestionOptionEntity;
import com.edusistem.core.exam.infrastructure.mapper.ExamMapper;
import com.edusistem.core.exam.infrastructure.repository.SpringDataExamQuestionOptionRepository;
import com.edusistem.core.exam.infrastructure.repository.SpringDataExamQuestionRepository;
import com.edusistem.core.exam.infrastructure.repository.SpringDataExamRepository;
import com.edusistem.core.exam.infrastructure.repository.SpringDataExamSubmissionRepository;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.shared.infrastructure.adapter.PageMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ExamRepositoryAdapter implements ExamRepositoryPort {

    private final SpringDataExamRepository exams;
    private final SpringDataExamQuestionRepository questions;
    private final SpringDataExamQuestionOptionRepository options;
    private final SpringDataExamSubmissionRepository submissions;
    private final ExamMapper mapper;

    public ExamRepositoryAdapter(SpringDataExamRepository exams, SpringDataExamQuestionRepository questions,
                                 SpringDataExamQuestionOptionRepository options,
                                 SpringDataExamSubmissionRepository submissions, ExamMapper mapper) {
        this.exams = exams;
        this.questions = questions;
        this.options = options;
        this.submissions = submissions;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Exam save(Exam exam) {
        ExamEntity saved = exams.save(mapper.toEntity(exam));
        if (!exam.getQuestions().isEmpty()) {
            options.deleteByExamId(saved.getId());
            questions.deleteByExamId(saved.getId());
            for (ExamQuestion question : exam.getQuestions()) {
                ExamQuestionEntity qe = mapper.toEntity(question);
                qe.setId(null);
                qe.setExamId(saved.getId());
                ExamQuestionEntity savedQuestion = questions.save(qe);
                List<ExamQuestionOptionEntity> optionEntities = new ArrayList<>();
                for (ExamQuestionOption option : question.getOptions()) {
                    ExamQuestionOptionEntity oe = mapper.toEntity(option);
                    oe.setId(null);
                    oe.setQuestionId(savedQuestion.getId());
                    optionEntities.add(oe);
                }
                options.saveAll(optionEntities);
            }
        }
        return findById(saved.getId()).orElseThrow();
    }

    @Override
    public Optional<Exam> findById(Long id) {
        return exams.findById(id).map(entity -> {
            Exam exam = mapper.toDomain(entity);
            List<ExamQuestionEntity> questionEntities = questions.findByExamIdOrderByQuestionNumberAsc(id);
            Map<Long, List<ExamQuestionOption>> optionsByQuestion = questionEntities.isEmpty() ? Map.of()
                    : options.findByQuestionIdInOrderByQuestionIdAscOptionLetterAsc(
                            questionEntities.stream().map(ExamQuestionEntity::getId).toList()).stream()
                    .map(mapper::toDomain).collect(Collectors.groupingBy(ExamQuestionOption::getQuestionId));
            exam.setQuestions(questionEntities.stream().map(qe -> {
                ExamQuestion q = mapper.toDomain(qe);
                q.setOptions(new ArrayList<>(optionsByQuestion.getOrDefault(qe.getId(), List.of())));
                return q;
            }).collect(Collectors.toCollection(ArrayList::new)));
            return exam;
        });
    }

    @Override
    public Optional<ExamView> findViewById(Long id) {
        return exams.findViewById(id);
    }

    @Override
    public PageResult<ExamView> findViewsByTeachingPeriodId(Long teachingPeriodId, PageQuery page) {
        return PageMapper.toResult(exams.findViews(teachingPeriodId, PageMapper.pageable(page)), v -> v);
    }

    @Override
    public boolean hasSubmissions(Long examId) {
        return submissions.existsByExamId(examId);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        options.deleteByExamId(id);
        questions.deleteByExamId(id);
        exams.deleteById(id);
    }
}
