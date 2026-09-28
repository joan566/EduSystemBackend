package com.edusistem.core.exam.infrastructure.adapter.docx;

import com.edusistem.core.exam.domain.outputports.QuestionDocumentPort;
import com.edusistem.core.exam.domain.vo.DocumentParagraph;
import com.edusistem.core.exam.domain.vo.DocumentParagraph.ListKind;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

/** Lee y genera documentos Word (.docx) de preguntas con Apache POI. */
@Component
public class PoiQuestionDocumentAdapter implements QuestionDocumentPort {

    @Override
    public List<DocumentParagraph> readParagraphs(byte[] content) {
        if (content == null || content.length == 0) {
            throw new InvalidRequestException("EMPTY_FILE", "The uploaded file is empty");
        }
        try (InputStream in = FileMagic.prepareToCheckMagic(new BufferedInputStream(new ByteArrayInputStream(content)))) {
            FileMagic magic = FileMagic.valueOf(in);
            if (magic == FileMagic.OLE2) {
                throw new InvalidRequestException("UNSUPPORTED_DOCUMENT_FORMAT",
                        "Old Word files (.doc) are not supported; save the document as .docx and upload it again");
            }
            if (magic != FileMagic.OOXML) {
                throw new InvalidRequestException("INVALID_DOCUMENT", "The file is not a readable Word document (.docx)");
            }
            try (XWPFDocument document = new XWPFDocument(in)) {
                List<DocumentParagraph> paragraphs = new ArrayList<>();
                for (IBodyElement element : document.getBodyElements()) {
                    collect(element, paragraphs);
                }
                return paragraphs;
            }
        } catch (InvalidRequestException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new InvalidRequestException("INVALID_DOCUMENT", "The file is not a readable Word document (.docx)");
        }
    }

    /** Los párrafos de las tablas se leen fila a fila, celda a celda. */
    private static void collect(IBodyElement element, List<DocumentParagraph> out) {
        if (element instanceof XWPFParagraph paragraph) {
            out.add(new DocumentParagraph(paragraph.getText(), listKind(paragraph)));
        } else if (element instanceof XWPFTable table) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    cell.getBodyElements().forEach(e -> collect(e, out));
                }
            }
        }
    }

    private static ListKind listKind(XWPFParagraph paragraph) {
        if (paragraph.getNumID() == null) {
            return ListKind.NONE;
        }
        String format = paragraph.getNumFmt();
        if (format == null) {
            return ListKind.NONE;
        }
        return switch (format) {
            case "decimal", "decimalZero" -> ListKind.NUMBERED;
            case "lowerLetter", "upperLetter" -> ListKind.LETTERED;
            default -> ListKind.NONE; // viñetas, romanos…: el texto se interpreta tal cual
        };
    }

    @Override
    public byte[] template() {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            line(document, "Plantilla de preguntas – EduSistem", true, 16);
            line(document, "Instrucciones (todo lo que está antes de la pregunta 1 no se importa):", true, 10);
            for (String tip : List.of(
                    "– Escriba cada pregunta con su número, sus opciones con letra y la respuesta correcta, como en los ejemplos.",
                    "– Todas las preguntas deben tener la misma cantidad de opciones (entre 2 y 6).",
                    "– En lugar de la línea «Respuesta:» puede marcar la opción correcta con un asterisco: *B) Corazón",
                    "– La línea «Puntos:» es opcional; si no la escribe, el puntaje se reparte por igual.",
                    "– El enunciado puede ocupar varios párrafos. Las imágenes y ecuaciones no se importan.",
                    "– Borre los ejemplos y escriba sus preguntas. Guarde el archivo como .docx.")) {
                line(document, tip, false, 10);
            }
            line(document, "", false, 10);
            question(document, "1. ¿Cuál es el órgano encargado de bombear la sangre por el cuerpo?",
                    List.of("A) Pulmón", "B) Corazón", "C) Hígado", "D) Riñón"), "Respuesta: B", null);
            question(document, "2. Si un triángulo tiene ángulos de 50° y 60°, ¿cuánto mide el tercer ángulo?",
                    List.of("A) 60°", "B) 70°", "C) 80°", "D) 90°"), "Respuesta: B", "Puntos: 2");
            document.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not generate the question template", e);
        }
    }

    private static void question(XWPFDocument document, String statement, List<String> options, String answer,
                                 String points) {
        line(document, statement, true, 11);
        options.forEach(o -> line(document, o, false, 11));
        line(document, answer, false, 11);
        if (points != null) {
            line(document, points, false, 11);
        }
        line(document, "", false, 11);
    }

    private static void line(XWPFDocument document, String text, boolean bold, int size) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingAfter(60);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setFontSize(size);
        run.setFontFamily("Calibri");
    }
}
