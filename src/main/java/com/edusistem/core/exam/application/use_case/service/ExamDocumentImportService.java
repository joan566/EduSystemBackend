package com.edusistem.core.exam.application.use_case.service;

import com.edusistem.core.exam.application.use_case.dtos.ExamCommands;
import com.edusistem.core.exam.domain.inputports.ImportExamFromDocumentUseCase;
import com.edusistem.core.exam.domain.inputports.ManageExamUseCase;
import com.edusistem.core.exam.domain.outputports.QuestionDocumentPort;
import com.edusistem.core.exam.domain.vo.ExamDetails;
import java.util.List;

/** Interpreta el documento Word y delega en {@link ManageExamUseCase}, que aplica las mismas reglas que la API JSON. */
public class ExamDocumentImportService implements ImportExamFromDocumentUseCase {

    private final QuestionDocumentPort documents;
    private final ManageExamUseCase exams;
    private final QuestionDocumentParser parser = new QuestionDocumentParser();

    public ExamDocumentImportService(QuestionDocumentPort documents, ManageExamUseCase exams) {
        this.documents = documents;
        this.exams = exams;
    }

    @Override
    public ExamDetails createFromDocument(ExamCommands.CreateFromDocument command) {
        List<ExamCommands.QuestionInput> questions = preview(command.content());
        return exams.create(new ExamCommands.Create(command.teacherId(), command.teachingPeriodId(), command.name(),
                command.description(), command.evaluationDate(), command.maximumScore(), questions.size(), questions));
    }

    @Override
    public ExamDetails replaceQuestionsFromDocument(ExamCommands.ReplaceQuestionsFromDocument command) {
        return exams.replaceQuestions(new ExamCommands.ReplaceQuestions(command.teacherId(), command.examId(),
                preview(command.content())));
    }

    @Override
    public List<ExamCommands.QuestionInput> preview(byte[] content) {
        return parser.parse(documents.readParagraphs(content));
    }

    @Override
    public byte[] template() {
        return documents.template();
    }
}
