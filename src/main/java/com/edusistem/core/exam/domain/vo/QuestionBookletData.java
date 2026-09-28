package com.edusistem.core.exam.domain.vo;

import com.edusistem.core.exam.domain.entity.ExamQuestion;
import java.util.List;

/** Datos impresos en el cuadernillo de preguntas (enunciados y opciones, nunca la respuesta correcta). */
public record QuestionBookletData(String examName, String subjectName, String groupName, List<ExamQuestion> questions) {
}
