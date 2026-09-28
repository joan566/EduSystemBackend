package com.edusistem.core.exam.domain.vo;

/**
 * Geometría determinista de la hoja de respuestas (A4 en puntos PDF, origen arriba-izquierda, eje Y hacia abajo).
 * El generador PDF dibuja con estas coordenadas y el lector OMR busca cada burbuja exactamente en ellas.
 */
public record AnswerSheetLayout(int numberOfQuestions, int optionCount) {

    public static final double PAGE_WIDTH = 595;
    public static final double PAGE_HEIGHT = 842;

    public static final int MAX_ROWS_PER_COLUMN = 25;
    public static final double CONTENT_LEFT = 52;
    public static final double CONTENT_RIGHT = 543;
    public static final double COLUMN_WIDTH = 122;
    public static final double GRID_LEFT = CONTENT_LEFT;
    public static final double FIRST_ROW_Y = 244;
    public static final double ROW_SPACING = 22;
    public static final double OPTION_SPACING = 17;
    /** Deja sitio al número de pregunta sin invadir el anillo de papel de la última burbuja de la columna anterior. */
    public static final double FIRST_OPTION_OFFSET = 26;
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
        return (numberOfQuestions + MAX_ROWS_PER_COLUMN - 1) / MAX_ROWS_PER_COLUMN;
    }

    /** Filas por columna, repartiendo las preguntas por igual (40 preguntas → 2 columnas de 20). */
    public int rowsPerColumn() {
        int columns = columns();
        return (numberOfQuestions + columns - 1) / columns;
    }

    /** Filas ocupadas en la columna indicada (la última puede tener menos). */
    public int rowsInColumn(int column) {
        return Math.min(rowsPerColumn(), numberOfQuestions - column * rowsPerColumn());
    }

    public double columnLeft(int column) {
        return GRID_LEFT + column * COLUMN_WIDTH;
    }

    /** Centro de la burbuja; {@code question} base 1, {@code option} base 0 (A=0). */
    public Point bubbleCenter(int question, int option) {
        int index = question - 1;
        int column = index / rowsPerColumn();
        int row = index % rowsPerColumn();
        return new Point(columnLeft(column) + FIRST_OPTION_OFFSET + option * OPTION_SPACING,
                FIRST_ROW_Y + row * ROW_SPACING);
    }

    public static char optionLetter(int option) {
        return (char) ('A' + option);
    }
}
