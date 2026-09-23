package com.edusistem.core.exam.domain.outputports;

import com.edusistem.core.exam.domain.vo.AnswerSheetData;
import java.util.List;

public interface AnswerSheetRendererPort {

    /** Genera un PDF con una hoja (página) por elemento. */
    byte[] render(List<AnswerSheetData> sheets);
}
