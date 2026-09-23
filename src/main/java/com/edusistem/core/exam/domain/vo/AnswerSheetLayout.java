package com.edusistem.core.exam.domain.vo;

/**
 * Geometría determinista de la hoja de respuestas (A4 en puntos PDF, origen arriba-izquierda, eje Y hacia abajo).
 * El generador PDF dibuja con estas coordenadas y el lector OMR busca cada burbuja exactamente en ellas.
 */
public record AnswerSheetLayout(int numberOfQuestions, int optionCount) {

    public static final double PAGE_WIDTH = 595;
    public static final double PAGE_HEIGHT = 842;

    public static final int ROWS_PER_COLUMN = 25;
    public static final double COLUMN_WIDTH = 118;
    public static final double GRID_LEFT = 60;
    public static final double FIRST_ROW_Y = 240;
    public static final double ROW_SPACING = 22;
    public static final double OPTION_SPACING = 17;
    public static final double FIRST_OPTION_OFFSET = 30;
    public static final double BUBBLE_RADIUS = 6;

    public static final double QR_X = 445;
    public static final double QR_Y = 70;
    public static final double QR_SIZE = 90;

    public record Point(double x, double y) {
    }

    /** Marcador de esquina: cuadrado negro relleno. El de arriba-izquierda es más grande para fijar la orientación. */
    public record Marker(String corner, double centerX, double centerY, double size) {
    }

    public AnswerSheetLayout {
        if (numberOfQuestions < 1 || optionCount < 2) {
            throw new IllegalArgumentException("Invalid layout");
        }
    }

    public java.util.List<Marker> markers() {
        return java.util.List.of(
                new Marker("TL", 40, 40, 30),
                new Marker("TR", 555, 40, 18),
                new Marker("BR", 555, 802, 18),
                new Marker("BL", 40, 802, 18));
    }

    public int columns() {
        return (numberOfQuestions + ROWS_PER_COLUMN - 1) / ROWS_PER_COLUMN;
    }

    /** Centro de la burbuja; {@code question} base 1, {@code option} base 0 (A=0). */
    public Point bubbleCenter(int question, int option) {
        int index = question - 1;
        int column = index / ROWS_PER_COLUMN;
        int row = index % ROWS_PER_COLUMN;
        return new Point(GRID_LEFT + column * COLUMN_WIDTH + FIRST_OPTION_OFFSET + option * OPTION_SPACING,
                FIRST_ROW_Y + row * ROW_SPACING);
    }

    /** Posición (esquina izquierda, línea base) del número de pregunta. */
    public Point questionLabelPosition(int question) {
        Point first = bubbleCenter(question, 0);
        return new Point(first.x() - FIRST_OPTION_OFFSET + 2, first.y() + 3);
    }

    /** Posición de la letra de opción sobre la primera fila de cada columna. */
    public Point optionHeaderPosition(int column, int option) {
        return new Point(GRID_LEFT + column * COLUMN_WIDTH + FIRST_OPTION_OFFSET + option * OPTION_SPACING - 3,
                FIRST_ROW_Y - BUBBLE_RADIUS - 5);
    }

    public static char optionLetter(int option) {
        return (char) ('A' + option);
    }
}
