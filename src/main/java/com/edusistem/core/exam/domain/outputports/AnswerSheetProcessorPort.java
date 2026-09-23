package com.edusistem.core.exam.domain.outputports;

import com.edusistem.core.exam.domain.vo.AnswerSheetLayout;
import com.edusistem.core.exam.domain.vo.BubbleReading;
import java.util.Optional;

/** Abstracción del procesamiento de imagen; el dominio no depende de ZXing/OpenCV ni de ningún algoritmo concreto. */
public interface AnswerSheetProcessorPort {

    /** Texto del QR de la hoja, o vacío si no se detecta ninguno. */
    Optional<String> readQrCode(byte[] image);

    /**
     * Localiza la hoja y mide el relleno de cada burbuja según el layout.
     * @throws com.edusistem.core.exam.domain.exceptions.AnswerSheetProcessingException si la hoja no puede detectarse
     */
    BubbleReading readBubbles(byte[] image, AnswerSheetLayout layout);
}
