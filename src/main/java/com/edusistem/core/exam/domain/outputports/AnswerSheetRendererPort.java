package com.edusistem.core.exam.domain.outputports;

import com.edusistem.core.exam.domain.vo.AnswerSheetData;
import com.edusistem.core.exam.domain.vo.QuestionBookletData;
import java.util.List;

public interface AnswerSheetRendererPort {

    /** Genera un PDF con una hoja (página) por elemento. */
    default byte[] render(List<AnswerSheetData> sheets) {
        return render(sheets, null);
    }

    /**
     * Genera un PDF con una hoja por elemento; si {@code booklet} no es nulo, detrás de cada hoja van las páginas del
     * cuadernillo de preguntas (cada estudiante recibe su juego completo).
     */
    byte[] render(List<AnswerSheetData> sheets, QuestionBookletData booklet);

    /** Genera solo el cuadernillo de preguntas. */
    byte[] renderBooklet(QuestionBookletData booklet);
}
