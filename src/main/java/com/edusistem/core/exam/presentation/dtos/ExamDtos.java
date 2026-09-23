package com.edusistem.core.exam.presentation.dtos;

import com.edusistem.core.exam.application.use_case.dtos.ExamCommands;
import com.edusistem.core.exam.domain.entity.ExamQuestion;
import com.edusistem.core.exam.domain.vo.ExamDetails;
import com.edusistem.core.exam.domain.vo.ExamView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ExamDtos {

    private ExamDtos() {
    }

    public record OptionRequest(@NotBlank @Pattern(regexp = "[A-Za-z]", message = "must be a single letter") String letter,
                                @NotBlank String text) {
    }

    public record QuestionRequest(@NotNull @Positive Integer questionNumber, @NotBlank String statement,
                                  @NotBlank @Pattern(regexp = "[A-Za-z]", message = "must be a single letter") String correctOption,
                                  @Positive BigDecimal points, @NotEmpty @Valid List<OptionRequest> options) {

        public ExamCommands.QuestionInput toInput() {
            return new ExamCommands.QuestionInput(questionNumber, statement, correctOption, points,
                    options.stream().map(o -> new ExamCommands.OptionInput(o.letter(), o.text())).toList());
        }
    }

    public record CreateExamRequest(@NotNull Long teachingPeriodId, @NotBlank @Size(max = 150) String name,
                                    String description, LocalDateTime evaluationDate,
                                    @Positive BigDecimal maximumScore, @NotNull @Positive Integer numberOfQuestions,
                                    @Valid List<QuestionRequest> questions) {
    }

    public record UpdateExamRequest(@NotBlank @Size(max = 150) String name, String description,
                                    LocalDateTime evaluationDate) {
    }

    public record ReplaceQuestionsRequest(@NotEmpty @Valid List<QuestionRequest> questions) {
    }

    public record OptionResponse(String letter, String text) {
    }

    public record QuestionResponse(int questionNumber, String statement, String correctOption, BigDecimal points,
                                   List<OptionResponse> options) {

        static QuestionResponse from(ExamQuestion q) {
            return new QuestionResponse(q.getQuestionNumber(), q.getStatement(), q.getCorrectOption(), q.getPoints(),
                    q.getOptions().stream().map(o -> new OptionResponse(o.getOptionLetter(), o.getOptionText())).toList());
        }
    }

    public record ExamSummaryResponse(Long id, Long evaluationId, Long teachingPeriodId, String name, String description,
                                      LocalDateTime evaluationDate, BigDecimal maximumScore, int numberOfQuestions) {

        public static ExamSummaryResponse from(ExamView v) {
            return new ExamSummaryResponse(v.examId(), v.evaluationId(), v.teachingPeriodId(), v.name(), v.description(),
                    v.evaluationDate(), v.maximumScore(), v.numberOfQuestions());
        }
    }

    public record ExamResponse(Long id, Long evaluationId, Long teachingPeriodId, String name, String description,
                               LocalDateTime evaluationDate, BigDecimal maximumScore, int numberOfQuestions,
                               boolean ready, int optionCount, List<QuestionResponse> questions) {

        public static ExamResponse from(ExamDetails d) {
            ExamView v = d.summary();
            return new ExamResponse(v.examId(), v.evaluationId(), v.teachingPeriodId(), v.name(), v.description(),
                    v.evaluationDate(), v.maximumScore(), d.exam().getNumberOfQuestions(), d.exam().isReady(),
                    d.exam().optionCount(), d.exam().getQuestions().stream().map(QuestionResponse::from).toList());
        }
    }
}
