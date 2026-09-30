package com.edusistem.core.shared.infrastructure.adapter;

import com.edusistem.core.shared.domain.outputports.SpreadsheetWriterPort;
import com.edusistem.core.shared.domain.vo.TabularData;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class PoiSpreadsheetWriter implements SpreadsheetWriterPort {

    /** Filas que se miran para calcular el ancho de cada columna (autoSizeColumn recorre todas y es lento). */
    private static final int ROWS_FOR_WIDTH = 200;
    private static final int MAX_COLUMN_CHARS = 70;
    /** Filas vacías, bajo los datos, en las que también se ofrecen las listas desplegables. */
    private static final int DROPDOWN_EXTRA_ROWS = 1000;

    private record Styles(CellStyle header, CellStyle requiredHeader, CellStyle date, CellStyle dateTime,
                          CellStyle time, CellStyle wrapped) {
    }

    @Override
    public byte[] write(TabularData data) {
        return writeWorkbook(List.of(data));
    }

    @Override
    public byte[] writeWorkbook(List<TabularData> sheets) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles styles = styles(workbook);
            for (TabularData data : sheets) {
                writeSheet(workbook.createSheet(safeSheetName(data.sheetName())), data, styles);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not generate the spreadsheet", e);
        }
    }

    private static Styles styles(Workbook workbook) {
        Font bold = workbook.createFont();
        bold.setBold(true);
        CellStyle header = workbook.createCellStyle();
        header.setFont(bold);
        CellStyle requiredHeader = workbook.createCellStyle();
        requiredHeader.setFont(bold);
        requiredHeader.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        requiredHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        CreationHelper helper = workbook.getCreationHelper();
        CellStyle date = workbook.createCellStyle();
        date.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd"));
        CellStyle dateTime = workbook.createCellStyle();
        dateTime.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd hh:mm"));
        CellStyle time = workbook.createCellStyle();
        time.setDataFormat(helper.createDataFormat().getFormat("hh:mm"));
        CellStyle wrapped = workbook.createCellStyle();
        wrapped.setWrapText(true);
        wrapped.setVerticalAlignment(VerticalAlignment.TOP);
        return new Styles(header, requiredHeader, date, dateTime, time, wrapped);
    }

    private static void writeSheet(Sheet sheet, TabularData data, Styles styles) {
        TabularData.Hints hints = data.hints();
        Row headerRow = sheet.createRow(0);
        for (int c = 0; c < data.headers().size(); c++) {
            Cell cell = headerRow.createCell(c);
            cell.setCellValue(data.headers().get(c));
            cell.setCellStyle(hints.requiredColumns().contains(c) ? styles.requiredHeader() : styles.header());
        }
        int rowIndex = 1;
        for (List<Object> values : data.rows()) {
            Row row = sheet.createRow(rowIndex++);
            for (int c = 0; c < values.size(); c++) {
                write(row.createCell(c), values.get(c), styles);
            }
        }
        sheet.createFreezePane(0, 1);
        addNotes(sheet, headerRow, hints.notes());
        addDropdowns(sheet, data.rows().size(), hints.dropdowns());
        sizeColumns(sheet, data, styles.wrapped());
    }

    private static void addNotes(Sheet sheet, Row headerRow, Map<Integer, String> notes) {
        if (notes.isEmpty()) {
            return;
        }
        Drawing<?> drawing = sheet.createDrawingPatriarch();
        CreationHelper helper = sheet.getWorkbook().getCreationHelper();
        notes.forEach((column, text) -> {
            Cell cell = headerRow.getCell(column);
            if (cell == null) {
                return;
            }
            ClientAnchor anchor = helper.createClientAnchor();
            anchor.setCol1(column);
            anchor.setCol2(column + 4);
            anchor.setRow1(0);
            anchor.setRow2(5);
            Comment comment = drawing.createCellComment(anchor);
            comment.setString(helper.createRichTextString(text));
            cell.setCellComment(comment);
        });
    }

    /** Listas sugeridas: si se escribe otro valor Excel sólo advierte, porque el lector acepta más variantes. */
    private static void addDropdowns(Sheet sheet, int dataRows, Map<Integer, List<String>> dropdowns) {
        if (dropdowns.isEmpty()) {
            return;
        }
        DataValidationHelper helper = sheet.getDataValidationHelper();
        int lastRow = dataRows + DROPDOWN_EXTRA_ROWS;
        dropdowns.forEach((column, options) -> {
            if (options.isEmpty()) {
                return;
            }
            DataValidation validation = helper.createValidation(
                    helper.createExplicitListConstraint(options.toArray(String[]::new)),
                    new CellRangeAddressList(1, lastRow, column, column));
            validation.setErrorStyle(DataValidation.ErrorStyle.WARNING);
            validation.setShowErrorBox(true);
            validation.setSuppressDropDownArrow(true);
            sheet.addValidationData(validation);
        });
    }

    /** Ancho según el contenido de las primeras filas; los textos muy largos se ajustan en varias líneas. */
    private static void sizeColumns(Sheet sheet, TabularData data, CellStyle wrapped) {
        for (int c = 0; c < data.headers().size(); c++) {
            int chars = data.headers().get(c).length();
            for (int r = 0; r < Math.min(ROWS_FOR_WIDTH, data.rows().size()); r++) {
                List<Object> values = data.rows().get(r);
                if (c < values.size() && values.get(c) != null) {
                    chars = Math.max(chars, displayLength(values.get(c)));
                }
            }
            int width = Math.min(chars, MAX_COLUMN_CHARS) + 3;
            sheet.setColumnWidth(c, width * 256);
            if (chars > MAX_COLUMN_CHARS) {
                for (int r = 1; r <= data.rows().size(); r++) {
                    Cell cell = sheet.getRow(r).getCell(c);
                    if (cell != null && cell.getCellStyle().getIndex() == 0) {
                        cell.setCellStyle(wrapped);
                    }
                }
            }
        }
    }

    private static int displayLength(Object value) {
        if (value instanceof LocalDateTime) {
            return 16;
        }
        if (value instanceof LocalDate) {
            return 10;
        }
        if (value instanceof LocalTime) {
            return 5;
        }
        if (value instanceof BigDecimal d) {
            return d.stripTrailingZeros().toPlainString().length();
        }
        return String.valueOf(value).length();
    }

    private static void write(Cell cell, Object value, Styles styles) {
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof BigDecimal d) {
            cell.setCellValue(d.doubleValue());
        } else if (value instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else if (value instanceof LocalDate d) {
            cell.setCellValue(d);
            cell.setCellStyle(styles.date());
        } else if (value instanceof LocalDateTime d) {
            cell.setCellValue(d);
            cell.setCellStyle(styles.dateTime());
        } else if (value instanceof LocalTime t) {
            cell.setCellValue(t.toSecondOfDay() / 86400.0);
            cell.setCellStyle(styles.time());
        } else {
            cell.setCellValue(neutralize(String.valueOf(value)));
        }
    }

    /** Evita inyección de fórmulas en Excel: los textos que empiezan por = + - @ se guardan como texto literal. */
    private static String neutralize(String text) {
        return !text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0 && !text.matches("-?\\d+(\\.\\d+)?")
                ? "'" + text : text;
    }

    private static String safeSheetName(String name) {
        String cleaned = name == null ? "Sheet1" : name.replaceAll("[\\\\/?*\\[\\]:]", " ");
        return cleaned.length() > 31 ? cleaned.substring(0, 31) : cleaned;
    }
}
