package com.edusistem.core.exam.infrastructure.mapper;

import com.edusistem.core.exam.domain.entity.Exam;
import com.edusistem.core.exam.domain.entity.ExamAnswer;
import com.edusistem.core.exam.domain.entity.ExamQuestion;
import com.edusistem.core.exam.domain.entity.ExamQuestionOption;
import com.edusistem.core.exam.domain.entity.ExamSubmission;
import com.edusistem.core.exam.infrastructure.entity.ExamAnswerEntity;
import com.edusistem.core.exam.infrastructure.entity.ExamEntity;
import com.edusistem.core.exam.infrastructure.entity.ExamQuestionEntity;
import com.edusistem.core.exam.infrastructure.entity.ExamQuestionOptionEntity;
import com.edusistem.core.exam.infrastructure.entity.ExamSubmissionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Las colecciones hijas (preguntas, opciones, respuestas) las ensamblan los adapters. */
@Mapper(componentModel = "spring")
public interface ExamMapper {

    @Mapping(target = "questions", ignore = true)
    Exam toDomain(ExamEntity entity);

    ExamEntity toEntity(Exam exam);

    @Mapping(target = "options", ignore = true)
    ExamQuestion toDomain(ExamQuestionEntity entity);

    ExamQuestionEntity toEntity(ExamQuestion question);

    ExamQuestionOption toDomain(ExamQuestionOptionEntity entity);

    ExamQuestionOptionEntity toEntity(ExamQuestionOption option);

    @Mapping(target = "answers", ignore = true)
    ExamSubmission toDomain(ExamSubmissionEntity entity);

    ExamSubmissionEntity toEntity(ExamSubmission submission);

    ExamAnswer toDomain(ExamAnswerEntity entity);

    ExamAnswerEntity toEntity(ExamAnswer answer);
}
