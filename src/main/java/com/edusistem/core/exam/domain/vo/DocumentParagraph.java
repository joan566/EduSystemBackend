package com.edusistem.core.exam.domain.vo;

/**
 * Párrafo de un documento de texto (Word) con el tipo de lista automática que lo numera, si la hay: el número o la
 * letra de una lista automática no forma parte de {@code text}.
 */
public record DocumentParagraph(String text, ListKind listKind) {

    public enum ListKind {
        NONE,
        /** Lista numerada (1, 2, 3…). */
        NUMBERED,
        /** Lista con letras (a, b, c… o A, B, C…). */
        LETTERED
    }

    public DocumentParagraph {
        text = text == null ? "" : text;
        listKind = listKind == null ? ListKind.NONE : listKind;
    }
}
